package com.medipro.manager.data.catalog

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.medipro.manager.core.search.PhoneticEncoder
import com.medipro.manager.core.search.SearchMatchKind
import com.medipro.manager.core.search.SearchNormalizer
import com.medipro.manager.core.search.SearchQueryEnhancer
import com.medipro.manager.core.search.SearchScore
import com.medipro.manager.domain.model.CatalogMedicine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class ScoredCatalogMedicine(
    val medicine: CatalogMedicine,
    val score: Int,
    val kind: SearchMatchKind,
)

/**
 * Read-only 271K master catalog — tiered search with short-circuit layers.
 */
@Singleton
class CatalogDatabaseHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var db: SQLiteDatabase? = null
    private var hasPhoneticColumns: Boolean? = null
    private var hasSynonymTable: Boolean? = null
    private var hasCompositionTokens: Boolean? = null

    suspend fun ensureOpen(): SQLiteDatabase? = withContext(Dispatchers.IO) {
        db?.takeIf { it.isOpen } ?: openCatalog()?.also { db = it }
    }

    suspend fun search(query: String, limit: Int = 50): List<CatalogMedicine> = withContext(Dispatchers.IO) {
        searchScored(query, limit).map { it.medicine }
    }

    suspend fun searchScored(query: String, limit: Int = 50): List<ScoredCatalogMedicine> =
        withContext(Dispatchers.IO) {
            val catalog = ensureOpen() ?: return@withContext emptyList()
            val normalized = SearchQueryEnhancer.normalize(query)
            if (normalized.length < 2) return@withContext emptyList()

            if (SearchQueryEnhancer.isBarcodeQuery(normalized)) {
                return@withContext findByBarcode(normalized)?.let {
                    listOf(ScoredCatalogMedicine(it, SearchScore.BARCODE, SearchMatchKind.BARCODE))
                } ?: emptyList()
            }

            val ranked = linkedMapOf<Long, ScoredCatalogMedicine>()

            fun add(items: List<CatalogMedicine>, kind: SearchMatchKind, baseScore: Int) {
                items.forEachIndexed { index, medicine ->
                    val score = baseScore - index.coerceAtMost(10)
                    val existing = ranked[medicine.id]
                    if (existing == null || score > existing.score) {
                        ranked[medicine.id] = ScoredCatalogMedicine(medicine, score, kind)
                    }
                }
            }

            add(searchExactBrand(catalog, normalized, limit), SearchMatchKind.EXACT_BRAND, SearchScore.EXACT_BRAND)
            if (SearchQueryEnhancer.hasEnoughResults(ranked.size)) return@withContext finalize(ranked, limit)

            add(searchExactGeneric(catalog, normalized, limit), SearchMatchKind.EXACT_GENERIC, SearchScore.EXACT_GENERIC)
            if (SearchQueryEnhancer.hasEnoughResults(ranked.size)) return@withContext finalize(ranked, limit)

            SearchQueryEnhancer.buildPrimaryFtsQuery(normalized)?.let { ftsQuery ->
                add(searchFts(catalog, ftsQuery, limit), SearchMatchKind.FTS_PREFIX, SearchScore.FTS_PREFIX)
            }
            if (SearchQueryEnhancer.hasEnoughResults(ranked.size)) return@withContext finalize(ranked, limit)

            add(searchStartsWith(catalog, normalized, limit), SearchMatchKind.STARTS_WITH, SearchScore.STARTS_WITH)
            if (SearchQueryEnhancer.hasEnoughResults(ranked.size)) return@withContext finalize(ranked, limit)

            add(searchContains(catalog, normalized, limit), SearchMatchKind.CONTAINS, SearchScore.CONTAINS)
            if (SearchQueryEnhancer.hasEnoughResults(ranked.size)) return@withContext finalize(ranked, limit)

            SearchNormalizer.expandGenericTokens(SearchNormalizer.tokenize(normalized)).forEach { token ->
                add(searchCompositionToken(catalog, token, limit), SearchMatchKind.CONTAINS, SearchScore.CONTAINS)
                if (SearchQueryEnhancer.hasEnoughResults(ranked.size)) return@withContext finalize(ranked, limit)
            }

            add(searchSynonyms(catalog, normalized, limit), SearchMatchKind.SYNONYM, SearchScore.SYNONYM)
            if (SearchQueryEnhancer.hasEnoughResults(ranked.size)) return@withContext finalize(ranked, limit)

            add(searchPhonetic(catalog, normalized, limit), SearchMatchKind.PHONETIC, SearchScore.PHONETIC)
            if (SearchQueryEnhancer.hasEnoughResults(ranked.size)) return@withContext finalize(ranked, limit)

            add(searchTypoBucket(catalog, normalized, limit), SearchMatchKind.TYPO, SearchScore.TYPO)
            finalize(ranked, limit)
        }

    suspend fun searchByComposition(
        composition: String,
        excludeBrand: String,
        limit: Int = 12,
    ): List<CatalogMedicine> = withContext(Dispatchers.IO) {
        val catalog = ensureOpen() ?: return@withContext emptyList()
        val comp = composition.trim()
        if (comp.isBlank()) return@withContext emptyList()

        val tokens = SearchNormalizer.tokenize(comp)
        if (tokens.isEmpty()) return@withContext emptyList()

        if (supportsCompositionTokens(catalog)) {
            val where = tokens.joinToString(" AND ") { "compositionTokens LIKE '%' || ? || '%'" }
            val args = (tokens + excludeBrand + limit.toString()).toTypedArray()
            return@withContext catalog.rawQuery(
                """
                SELECT id, brandName, genericName, composition, strength,
                       dosageForm, manufacturer, category, barcode
                FROM catalog_medicines
                WHERE $where AND brandName != ?
                ORDER BY brandName ASC
                LIMIT ?
                """.trimIndent(),
                args,
            ).use { cursor -> mapCursor(cursor) }
        }

        val ftsQuery = SearchQueryEnhancer.buildFtsQuery(comp) ?: return@withContext emptyList()
        catalog.rawQuery(
            """
            SELECT c.id, c.brandName, c.genericName, c.composition, c.strength,
                   c.dosageForm, c.manufacturer, c.category, c.barcode
            FROM catalog_medicines c
            INNER JOIN catalog_medicines_fts fts ON c.id = fts.docid
            WHERE catalog_medicines_fts MATCH ?
            AND c.brandName != ?
            ORDER BY c.brandName ASC
            LIMIT ?
            """.trimIndent(),
            arrayOf(ftsQuery, excludeBrand, limit.toString()),
        ).use { cursor -> mapCursor(cursor) }
    }

    suspend fun findByBarcode(barcode: String): CatalogMedicine? = withContext(Dispatchers.IO) {
        val catalog = ensureOpen() ?: return@withContext null
        catalog.rawQuery(
            """
            SELECT id, brandName, genericName, composition, strength,
                   dosageForm, manufacturer, category, barcode
            FROM catalog_medicines
            WHERE barcode = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(barcode.trim()),
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.toMedicine() else null
        }
    }

    suspend fun getById(id: Long): CatalogMedicine? = withContext(Dispatchers.IO) {
        val catalog = ensureOpen() ?: return@withContext null
        catalog.rawQuery(
            """
            SELECT id, brandName, genericName, composition, strength,
                   dosageForm, manufacturer, category, barcode
            FROM catalog_medicines WHERE id = ? LIMIT 1
            """.trimIndent(),
            arrayOf(id.toString()),
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.toMedicine() else null
        }
    }

    private fun searchExactBrand(catalog: SQLiteDatabase, brand: String, limit: Int): List<CatalogMedicine> =
        catalog.rawQuery(
            """
            SELECT id, brandName, genericName, composition, strength,
                   dosageForm, manufacturer, category, barcode
            FROM catalog_medicines
            WHERE brandName = ? COLLATE NOCASE
            LIMIT ?
            """.trimIndent(),
            arrayOf(brand, limit.toString()),
        ).use { cursor -> mapCursor(cursor) }

    private fun searchExactGeneric(catalog: SQLiteDatabase, generic: String, limit: Int): List<CatalogMedicine> =
        catalog.rawQuery(
            """
            SELECT id, brandName, genericName, composition, strength,
                   dosageForm, manufacturer, category, barcode
            FROM catalog_medicines
            WHERE genericName = ? COLLATE NOCASE
            LIMIT ?
            """.trimIndent(),
            arrayOf(generic, limit.toString()),
        ).use { cursor -> mapCursor(cursor) }

    private fun searchFts(catalog: SQLiteDatabase, ftsQuery: String, limit: Int): List<CatalogMedicine> =
        catalog.rawQuery(
            """
            SELECT c.id, c.brandName, c.genericName, c.composition, c.strength,
                   c.dosageForm, c.manufacturer, c.category, c.barcode
            FROM catalog_medicines c
            WHERE c.id IN (
                SELECT docid FROM catalog_medicines_fts WHERE catalog_medicines_fts MATCH ?
            )
            ORDER BY c.brandName ASC
            LIMIT ?
            """.trimIndent(),
            arrayOf(ftsQuery, limit.toString()),
        ).use { cursor -> mapCursor(cursor) }

    private fun searchStartsWith(catalog: SQLiteDatabase, prefix: String, limit: Int): List<CatalogMedicine> =
        catalog.rawQuery(
            """
            SELECT id, brandName, genericName, composition, strength,
                   dosageForm, manufacturer, category, barcode
            FROM catalog_medicines
            WHERE brandName LIKE ? OR genericName LIKE ?
            ORDER BY brandName ASC
            LIMIT ?
            """.trimIndent(),
            arrayOf("$prefix%", "$prefix%", limit.toString()),
        ).use { cursor -> mapCursor(cursor) }

    private fun searchContains(catalog: SQLiteDatabase, query: String, limit: Int): List<CatalogMedicine> =
        catalog.rawQuery(
            """
            SELECT id, brandName, genericName, composition, strength,
                   dosageForm, manufacturer, category, barcode
            FROM catalog_medicines
            WHERE brandName LIKE ? OR genericName LIKE ? OR composition LIKE ?
               OR strength LIKE ? OR manufacturer LIKE ? OR dosageForm LIKE ?
            ORDER BY brandName ASC
            LIMIT ?
            """.trimIndent(),
            arrayOf(
                "%$query%", "%$query%", "%$query%",
                "%$query%", "%$query%", "%$query%",
                limit.toString(),
            ),
        ).use { cursor -> mapCursor(cursor) }

    private fun searchCompositionToken(catalog: SQLiteDatabase, token: String, limit: Int): List<CatalogMedicine> {
        if (supportsCompositionTokens(catalog)) {
            return catalog.rawQuery(
                """
                SELECT id, brandName, genericName, composition, strength,
                       dosageForm, manufacturer, category, barcode
                FROM catalog_medicines
                WHERE compositionTokens LIKE ?
                ORDER BY brandName ASC
                LIMIT ?
                """.trimIndent(),
                arrayOf("%$token%", limit.toString()),
            ).use { cursor -> mapCursor(cursor) }
        }
        return searchContains(catalog, token, limit)
    }

    private fun searchSynonyms(catalog: SQLiteDatabase, query: String, limit: Int): List<CatalogMedicine> {
        if (!supportsSynonymTable(catalog)) {
            return SearchQueryEnhancer.buildSynonymFtsQueries(query).flatMap { ftsQuery ->
                searchFts(catalog, ftsQuery, limit)
            }.distinctBy { it.id }
        }

        val tokens = query.lowercase().split(' ').filter { it.isNotBlank() }
        val canonicals = tokens.flatMap { token ->
            catalog.rawQuery(
                "SELECT DISTINCT canonical FROM catalog_synonyms WHERE term = ? COLLATE NOCASE LIMIT 8",
                arrayOf(token),
            ).use { cursor ->
                buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
            }
        }.distinct()

        return canonicals.flatMap { canonical ->
            SearchQueryEnhancer.buildPrimaryFtsQuery(canonical)?.let { searchFts(catalog, it, limit) } ?: emptyList()
        }.distinctBy { it.id }
    }

    private fun searchPhonetic(catalog: SQLiteDatabase, query: String, limit: Int): List<CatalogMedicine> {
        if (supportsPhoneticColumns(catalog)) {
            val firstToken = SearchQueryEnhancer.phoneticKey(query).split(' ').firstOrNull().orEmpty()
            if (firstToken.isBlank()) return emptyList()
            return catalog.rawQuery(
                """
                SELECT id, brandName, genericName, composition, strength,
                       dosageForm, manufacturer, category, barcode
                FROM catalog_medicines
                WHERE phoneticBrand LIKE ? OR phoneticGeneric LIKE ?
                ORDER BY brandName ASC
                LIMIT ?
                """.trimIndent(),
                arrayOf("$firstToken%", "$firstToken%", limit.toString()),
            ).use { cursor -> mapCursor(cursor) }
        }

        val prefix = SearchNormalizer.typoBucketPrefix(query)
        if (prefix.length < 2) return emptyList()
        return catalog.rawQuery(
            """
            SELECT id, brandName, genericName, composition, strength,
                   dosageForm, manufacturer, category, barcode
            FROM catalog_medicines
            WHERE brandName LIKE ? OR genericName LIKE ?
            ORDER BY brandName ASC
            LIMIT 120
            """.trimIndent(),
            arrayOf("$prefix%", "$prefix%"),
        ).use { cursor ->
            mapCursor(cursor).filter { medicine ->
                PhoneticEncoder.matches(query, medicine.brandName) ||
                    PhoneticEncoder.matches(query, medicine.genericName)
            }.take(limit)
        }
    }

    private fun searchTypoBucket(catalog: SQLiteDatabase, query: String, limit: Int): List<CatalogMedicine> {
        val prefix = SearchNormalizer.typoBucketPrefix(query)
        if (prefix.length < 2) return emptyList()

        val candidates = catalog.rawQuery(
            """
            SELECT id, brandName, genericName, composition, strength,
                   dosageForm, manufacturer, category, barcode
            FROM catalog_medicines
            WHERE brandName LIKE ? OR genericName LIKE ?
            ORDER BY brandName ASC
            LIMIT ?
            """.trimIndent(),
            arrayOf("$prefix%", "$prefix%", SearchScore.TYPO_BUCKET_LIMIT.toString()),
        ).use { cursor -> mapCursor(cursor) }

        return SearchQueryEnhancer.rankTypoBucket(
            query,
            candidates.flatMap { listOf(it.brandName, it.genericName) },
        ).mapNotNull { (name, _) ->
            candidates.firstOrNull {
                it.brandName.equals(name, ignoreCase = true) ||
                    it.genericName.equals(name, ignoreCase = true)
            }
        }.distinctBy { it.id }.take(limit)
    }

    private fun finalize(ranked: LinkedHashMap<Long, ScoredCatalogMedicine>, limit: Int): List<ScoredCatalogMedicine> =
        ranked.values.sortedByDescending { it.score }.take(limit)

    private fun supportsPhoneticColumns(catalog: SQLiteDatabase): Boolean {
        hasPhoneticColumns?.let { return it }
        val found = hasColumn(catalog, "catalog_medicines", "phoneticBrand")
        hasPhoneticColumns = found
        return found
    }

    private fun supportsSynonymTable(catalog: SQLiteDatabase): Boolean {
        hasSynonymTable?.let { return it }
        val found = catalog.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='catalog_synonyms'",
            null,
        ).use { it.moveToFirst() }
        hasSynonymTable = found
        return found
    }

    private fun supportsCompositionTokens(catalog: SQLiteDatabase): Boolean {
        hasCompositionTokens?.let { return it }
        val found = hasColumn(catalog, "catalog_medicines", "compositionTokens")
        hasCompositionTokens = found
        return found
    }

    private fun hasColumn(catalog: SQLiteDatabase, table: String, column: String): Boolean =
        catalog.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
            generateSequence { if (cursor.moveToNext()) cursor.getString(1) else null }
                .any { it == column }
        }

    private fun mapCursor(cursor: android.database.Cursor): List<CatalogMedicine> = buildList {
        while (cursor.moveToNext()) add(cursor.toMedicine())
    }

    private fun android.database.Cursor.toMedicine() = CatalogMedicine(
        id = getLong(0),
        brandName = getString(1),
        genericName = getString(2),
        composition = getString(3),
        strength = getString(4),
        dosageForm = getString(5),
        manufacturer = getString(6),
        category = getString(7),
        barcode = getString(8),
    )

    private fun openCatalog(): SQLiteDatabase? {
        val dest = File(context.filesDir, CATALOG_DB_NAME)
        if (!dest.exists()) {
            try {
                context.assets.open(ASSET_CATALOG_DB).use { input ->
                    FileOutputStream(dest).use { output -> input.copyTo(output) }
                }
            } catch (_: Exception) {
                return null
            }
        }
        return SQLiteDatabase.openDatabase(dest.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
    }

    companion object {
        const val ASSET_CATALOG_DB = "databases/catalog.db"
        private const val CATALOG_DB_NAME = "catalog.db"
    }
}
