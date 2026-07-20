package com.medipro.manager.data.repository

import com.medipro.manager.core.database.dao.BatchDao
import com.medipro.manager.core.database.dao.MedicineBatchPreviewRow
import com.medipro.manager.core.database.dao.MedicineDao
import com.medipro.manager.core.database.dao.StockDao
import com.medipro.manager.core.database.entity.MedicineEntity
import com.medipro.manager.core.search.SearchAnalytics
import com.medipro.manager.core.search.SearchMatchKind
import com.medipro.manager.core.search.SearchQueryEnhancer
import com.medipro.manager.core.search.SearchResultCache
import com.medipro.manager.data.catalog.CatalogDatabaseHelper
import com.medipro.manager.data.search.InventorySearchEngine
import com.medipro.manager.domain.model.CatalogMedicine
import com.medipro.manager.domain.model.PosSearchResponse
import com.medipro.manager.domain.model.PosSearchResult
import com.medipro.manager.domain.model.PosSearchSource
import com.medipro.manager.domain.repository.CatalogRepository
import com.medipro.manager.domain.repository.PosSearchRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

@Singleton
class PosSearchRepositoryImpl @Inject constructor(
    private val medicineDao: MedicineDao,
    private val stockDao: StockDao,
    private val batchDao: BatchDao,
    private val inventorySearchEngine: InventorySearchEngine,
    private val catalogDb: CatalogDatabaseHelper,
    private val catalogRepository: CatalogRepository,
    private val searchAnalytics: SearchAnalytics,
) : PosSearchRepository {

    private val cache = SearchResultCache<PosSearchResponse>()

    override suspend fun search(query: String): PosSearchResponse {
        val normalized = SearchQueryEnhancer.normalize(query)
        if (normalized.length < 2) return PosSearchResponse(emptyList())

        cache.get(normalized.lowercase())?.let { return it }

        val response: PosSearchResponse
        val elapsedMs = measureTimeMillis {
            response = coroutineScope {
                if (SearchQueryEnhancer.isBarcodeQuery(normalized)) {
                    searchAnalytics.recordBarcodeScan()
                    return@coroutineScope PosSearchResponse(
                        results = listOfNotNull(lookupBarcode(normalized)),
                    )
                }

            val inventoryDeferred = async { inventorySearchEngine.search(normalized, limit = 50) }
            val catalogDeferred = async { catalogDb.searchScored(normalized, limit = 30) }

            val inventoryScored = inventoryDeferred.await()
            val inventoryBrands = inventoryScored.map { it.entity.brandName.lowercase() }.toSet()
            val now = System.currentTimeMillis()
            val medicineIds = inventoryScored.map { it.entity.id }
            val batchPreviewMap = if (medicineIds.isNotEmpty()) {
                buildFefoBatchMap(batchDao.getBatchPreviewsForMedicines(medicineIds), now)
            } else {
                emptyMap()
            }

            val inventoryResults = inventoryScored.map { scored ->
                val qty = stockDao.getTotalQuantity(scored.entity.id) ?: 0
                val batchPreview = batchPreviewMap[scored.entity.id]
                scored.entity.toSearchResult(
                    stockQty = qty,
                    score = scored.score,
                    kind = scored.kind,
                    batchNumber = batchPreview?.first,
                    expiryDate = batchPreview?.second,
                )
            }

            val catalogOnly = catalogDeferred.await()
                .filter { it.medicine.brandName.lowercase() !in inventoryBrands }
                .map { scored -> scored.medicine.toCatalogResult(scored.score, scored.kind) }

            val merged = (inventoryResults + catalogOnly)
                .sortedByDescending { it.matchScore }
                .distinctBy { it.key }

            val top = merged.firstOrNull()
            val topKind = top?.matchKind?.let { kind ->
                runCatching { SearchMatchKind.valueOf(kind) }.getOrNull()
            }
            val didYouMean = SearchQueryEnhancer.suggestDidYouMean(
                query = normalized,
                topBrand = top?.brandName,
                topKind = topKind,
            )

            PosSearchResponse(results = merged, didYouMean = didYouMean)
            }
        }

        val topKind = response.results.firstOrNull()?.matchKind?.let { kind ->
            runCatching { SearchMatchKind.valueOf(kind) }.getOrNull()
        }
        searchAnalytics.recordSearch(
            query = normalized,
            resultCount = response.results.size,
            didYouMean = response.didYouMean,
            matchKind = topKind,
            durationMs = elapsedMs,
        )

        cache.put(normalized.lowercase(), response)
        return response
    }

    override suspend fun findAlternatives(medicineId: Long): List<PosSearchResult> {
        val medicine = medicineDao.getById(medicineId) ?: return emptyList()
        val composition = medicine.composition.ifBlank { medicine.genericName }
        if (composition.isBlank()) return emptyList()

        return catalogRepository
            .searchByComposition(composition, excludeBrand = medicine.brandName, limit = 10)
            .map { it.toCatalogResult(score = 70, kind = SearchMatchKind.CONTAINS) }
    }

    override suspend fun resolveCatalogItem(catalogId: Long): PosSearchResult? {
        medicineDao.getByCatalogId(catalogId.toString())?.let { entity ->
            val qty = stockDao.getTotalQuantity(entity.id) ?: 0
            val batchPreview = batchDao.getBatchPreviewsForMedicines(listOf(entity.id))
                .let { buildFefoBatchMap(it, System.currentTimeMillis())[entity.id] }
            return entity.toSearchResult(
                stockQty = qty,
                score = 100,
                kind = SearchMatchKind.EXACT_BRAND,
                batchNumber = batchPreview?.first,
                expiryDate = batchPreview?.second,
            )
        }
        return catalogRepository.getById(catalogId)?.toCatalogResult(score = 80, kind = SearchMatchKind.STARTS_WITH)
    }

    override suspend fun lookupBarcode(barcode: String): PosSearchResult? {
        val trimmed = barcode.trim()
        if (trimmed.isBlank()) return null

        medicineDao.getByBarcode(trimmed)?.let { entity ->
            val qty = stockDao.getTotalQuantity(entity.id) ?: 0
            val batchPreview = batchPreviewFor(entity.id)
            return entity.toSearchResult(qty, 100, SearchMatchKind.BARCODE, batchPreview?.first, batchPreview?.second)
        }

        catalogRepository.findByBarcode(trimmed)?.let { catalog ->
            medicineDao.getByCatalogId(catalog.id.toString())?.let { entity ->
                val qty = stockDao.getTotalQuantity(entity.id) ?: 0
                val batchPreview = batchPreviewFor(entity.id)
                return entity.toSearchResult(qty, 100, SearchMatchKind.BARCODE, batchPreview?.first, batchPreview?.second)
            }
            medicineDao.getByBrandName(catalog.brandName)?.let { entity ->
                val qty = stockDao.getTotalQuantity(entity.id) ?: 0
                val batchPreview = batchPreviewFor(entity.id)
                return entity.toSearchResult(qty, 100, SearchMatchKind.BARCODE, batchPreview?.first, batchPreview?.second)
            }
            return catalog.toCatalogResult(score = 100, kind = SearchMatchKind.BARCODE)
        }
        return null
    }

    private suspend fun batchPreviewFor(medicineId: Long): Pair<String, Long>? =
        buildFefoBatchMap(
            batchDao.getBatchPreviewsForMedicines(listOf(medicineId)),
            System.currentTimeMillis(),
        )[medicineId]

    private fun buildFefoBatchMap(
        rows: List<MedicineBatchPreviewRow>,
        now: Long,
    ): Map<Long, Pair<String, Long>> {
        return rows.groupBy { it.medicineId }.mapValues { (_, batches) ->
            val sellable = batches.firstOrNull { it.expiryDate >= now } ?: batches.first()
            sellable.batchNumber to sellable.expiryDate
        }
    }

    private fun MedicineEntity.toSearchResult(
        stockQty: Int,
        score: Int,
        kind: SearchMatchKind,
        batchNumber: String? = null,
        expiryDate: Long? = null,
    ) = PosSearchResult(
        key = "inv-$id",
        catalogId = catalogUuid?.toLongOrNull(),
        medicineId = id,
        brandName = brandName,
        genericName = genericName,
        composition = composition,
        strength = strength,
        dosageForm = dosageForm,
        manufacturer = manufacturer,
        barcode = barcode,
        stockQuantity = stockQty,
        sellingPrice = sellingPricePaisa / 100.0,
        mrp = mrpPaisa / 100.0,
        batchNumber = batchNumber,
        expiryDate = expiryDate,
        reorderLevel = reorderLevel,
        inStock = stockQty > 0,
        requiresPrescription = requiresPrescription,
        scheduleCategory = scheduleCategory,
        source = PosSearchSource.INVENTORY,
        matchScore = score,
        matchKind = kind.name,
    )

    private fun CatalogMedicine.toCatalogResult(
        score: Int,
        kind: SearchMatchKind,
    ) = PosSearchResult(
        key = "cat-$id",
        catalogId = id,
        brandName = brandName,
        genericName = genericName,
        composition = composition,
        strength = strength,
        dosageForm = dosageForm,
        manufacturer = manufacturer,
        barcode = barcode,
        stockQuantity = 0,
        inStock = false,
        source = PosSearchSource.CATALOG,
        matchScore = score,
        matchKind = kind.name,
    )
}
