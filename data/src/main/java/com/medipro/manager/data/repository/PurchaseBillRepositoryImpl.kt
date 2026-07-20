package com.medipro.manager.data.repository



import android.content.Context

import android.graphics.Bitmap

import android.graphics.BitmapFactory

import android.net.Uri

import com.medipro.manager.core.database.dao.LicenseDao

import com.medipro.manager.core.database.dao.MedicineDao

import com.medipro.manager.core.database.dao.PurchaseDao

import com.medipro.manager.core.database.entity.MedicineEntity

import com.medipro.manager.core.database.entity.SyncStatus

import com.medipro.manager.data.ocr.BillImagePreprocessor

import com.medipro.manager.data.ocr.MlKitTextRecognizer

import com.medipro.manager.data.purchasebill.NepalWholesaleBillParser

import com.medipro.manager.data.purchasebill.PurchaseBillLineEnricher
import com.medipro.manager.data.purchasebill.PurchaseBillMatcher

import com.medipro.manager.data.purchasebill.parser.WholesaleBillParserRegistry

import com.medipro.manager.data.sync.pharmacyUuid

import com.medipro.manager.domain.model.BillScanProgress
import com.medipro.manager.domain.model.ParsedPurchaseBillLine
import com.medipro.manager.domain.model.PurchaseBillLineMatch
import com.medipro.manager.domain.model.PurchaseBillMatchStatus

import com.medipro.manager.domain.model.DuplicateSupplierBill

import com.medipro.manager.domain.model.OcrMedicineDraft

import com.medipro.manager.domain.model.PosSearchResult

import com.medipro.manager.domain.model.ScannedPurchaseBill

import com.medipro.manager.domain.model.Supplier

import com.medipro.manager.domain.repository.OcrMedicineAliasRepository

import com.medipro.manager.domain.repository.PurchaseBillRepository

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.withContext

import javax.inject.Inject

import javax.inject.Singleton

import kotlin.math.roundToLong



object SupplierBillNotes {

    fun format(billNumber: String): String = "Supplier Bill: ${billNumber.trim()}"

}



@Singleton

class PurchaseBillRepositoryImpl @Inject constructor(

    @ApplicationContext private val context: Context,

    private val textRecognizer: MlKitTextRecognizer,

    private val matcher: PurchaseBillMatcher,

    private val medicineDao: MedicineDao,

    private val purchaseDao: PurchaseDao,

    private val catalogRepository: com.medipro.manager.domain.repository.CatalogRepository,

    private val licenseDao: LicenseDao,

    private val aliasRepository: OcrMedicineAliasRepository,

    private val lineEnricher: PurchaseBillLineEnricher,

) : PurchaseBillRepository {



    override suspend fun scanBillFromImages(

        imageUris: List<String>,

        suppliers: List<Supplier>,

        onProgress: suspend (BillScanProgress) -> Unit,

    ): ScannedPurchaseBill = withContext(Dispatchers.Default) {

        if (imageUris.isEmpty()) {

            return@withContext emptyScan(warnings = listOf("No bill images captured"))

        }

        val totalPages = imageUris.size

        val ocrParts = imageUris.mapIndexedNotNull { index, uriString ->

            onProgress(BillScanProgress("Reading bill…", index + 1, totalPages))

            runCatching {

                val bitmap = loadBitmap(Uri.parse(uriString)) ?: return@runCatching null

                try {

                    val preprocessed = BillImagePreprocessor.preprocess(bitmap)

                    textRecognizer.recognize(preprocessed).also {

                        if (!preprocessed.isRecycled) preprocessed.recycle()

                    }

                } finally {

                    if (!bitmap.isRecycled) bitmap.recycle()

                }

            }.getOrNull()

        }

        if (ocrParts.isEmpty()) {

            return@withContext emptyScan(

                warnings = listOf("Could not read bill image(s)"),

                sourceImageUris = imageUris,

            )

        }

        val mergedText = ocrParts.joinToString("\n\n--- PAGE ---\n\n")

        scanBillFromText(mergedText, suppliers, onProgress).copy(

            pageCount = imageUris.size,

            sourceImageUris = imageUris,

        )

    }



    override suspend fun scanBillFromText(

        ocrText: String,

        suppliers: List<Supplier>,

        onProgress: suspend (BillScanProgress) -> Unit,

    ): ScannedPurchaseBill {

        if (ocrText.isBlank()) {

            return emptyScan(warnings = listOf("No text found on bill"))

        }

        onProgress(BillScanProgress("Parsing items…"))

        val (parsed, parserName) = WholesaleBillParserRegistry.parse(ocrText)

        val matchedSupplier = NepalWholesaleBillParser.matchSupplier(parsed.supplierName, suppliers)

        val lineMatches = matcher.matchLines(parsed.lines) { current, total ->

            onProgress(BillScanProgress("Matching medicines…", current, total))

        }

        val duplicate = parsed.invoiceNumber?.trim()?.takeIf { it.isNotBlank() }?.let { billNo ->

            purchaseDao.findBySupplierBillNumber(billNo, matchedSupplier?.id)?.let { entity ->

                DuplicateSupplierBill(

                    purchaseId = entity.id,

                    internalInvoiceNumber = entity.invoiceNumber,

                    supplierBillNumber = billNo,

                )

            }

        }

        return ScannedPurchaseBill(

            supplierName = parsed.supplierName,

            matchedSupplierId = matchedSupplier?.id,

            matchedSupplierName = matchedSupplier?.name,

            invoiceNumber = parsed.invoiceNumber,

            invoiceDate = parsed.invoiceDate,

            lines = lineMatches,

            netTotal = parsed.netTotal,

            rawOcrText = ocrText,

            parseWarnings = parsed.warnings,

            parserUsed = parserName,

            duplicateBill = duplicate,

        )

    }



    override suspend fun saveOcrAlias(ocrDescription: String, medicineId: Long, medicineName: String) {

        val medicine = medicineDao.getById(medicineId)

        aliasRepository.saveMapping(

            ocrDescription = ocrDescription,

            medicineId = medicineId,

            medicineName = medicineName,

            medicineUuid = medicine?.uuid,

        )

    }



    override suspend fun updateMedicineMrp(medicineId: Long, newMrp: Double) {

        val medicine = medicineDao.getById(medicineId) ?: return

        val now = System.currentTimeMillis()

        medicineDao.update(

            medicine.copy(

                mrpPaisa = (newMrp * 100).roundToLong(),

                sellingPricePaisa = maxOf(medicine.sellingPricePaisa, (newMrp * 100).roundToLong()),

                updatedAt = now,

                syncStatus = SyncStatus.PENDING,

            ),

        )

    }



    override suspend fun enrichLineMatch(line: PurchaseBillLineMatch): PurchaseBillLineMatch = matcher.enrich(line)



    override suspend fun lineMatchForMedicine(

        parsed: ParsedPurchaseBillLine,

        medicineId: Long,

        viaAlias: Boolean,

    ): PurchaseBillLineMatch {

        val medicine = medicineDao.getById(medicineId)

            ?: throw IllegalStateException("Medicine not found")

        val base = PurchaseBillLineMatch(

            parsed = parsed,

            match = lineEnricher.medicineToSearchResult(medicine),

            status = PurchaseBillMatchStatus.MATCHED,

            confidence = if (viaAlias) 99 else 95,

            matchedViaAlias = viaAlias,

        )

        return matcher.enrich(base)

    }



    override suspend fun resolveMedicineId(match: PosSearchResult?, fallbackBrandName: String): Long? {

        match?.medicineId?.let { return it }



        val catalogId = match?.catalogId

        if (catalogId != null) {

            medicineDao.getByCatalogId(catalogId.toString())?.id?.let { return it }

            val catalog = catalogRepository.getById(catalogId) ?: return null

            val now = System.currentTimeMillis()

            return medicineDao.insert(

                MedicineEntity(

                    catalogUuid = catalog.id.toString(),

                    pharmacyUuid = licenseDao.pharmacyUuid(),

                    brandName = catalog.brandName,

                    genericName = catalog.genericName,

                    composition = catalog.composition,

                    strength = catalog.strength,

                    dosageForm = catalog.dosageForm,

                    manufacturer = catalog.manufacturer,

                    category = catalog.category,

                    barcode = catalog.barcode,

                    mrpPaisa = (match.mrp * 100).roundToLong(),

                    createdAt = now,

                    updatedAt = now,

                    syncStatus = SyncStatus.PENDING,

                ),

            )

        }



        val local = medicineDao.getByBrandName(fallbackBrandName.trim())

        return local?.id

    }



    override suspend fun createMedicineFromDraft(draft: OcrMedicineDraft): Long {

        val now = System.currentTimeMillis()

        return medicineDao.insert(

            MedicineEntity(

                pharmacyUuid = licenseDao.pharmacyUuid(),

                brandName = draft.brandName.trim(),

                genericName = draft.genericName.ifBlank { draft.brandName.trim() },

                strength = draft.strength,

                dosageForm = draft.dosageForm,

                manufacturer = draft.manufacturer,

                unit = draft.unit,

                purchasePricePaisa = (draft.purchasePrice * 100).roundToLong(),

                mrpPaisa = (draft.mrp * 100).roundToLong(),

                sellingPricePaisa = (draft.mrp * 100).roundToLong(),

                createdAt = now,

                updatedAt = now,

                syncStatus = SyncStatus.PENDING,

            ),

        )

    }



    private fun loadBitmap(uri: Uri): Bitmap? = runCatching {

        context.contentResolver.openInputStream(uri)?.use { stream ->

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }

            BitmapFactory.decodeStream(stream, null, bounds)

            context.contentResolver.openInputStream(uri)?.use { fullStream ->

                val options = BitmapFactory.Options().apply {

                    inSampleSize = calculateInSampleSize(bounds, 2048, 2048)

                }

                BitmapFactory.decodeStream(fullStream, null, options)

            }

        }

    }.getOrNull()



    private fun calculateInSampleSize(options: BitmapFactory.Options, maxWidth: Int, maxHeight: Int): Int {

        val (height, width) = options.outHeight to options.outWidth

        var inSampleSize = 1

        if (height > maxHeight || width > maxWidth) {

            var halfHeight = height / 2

            var halfWidth = width / 2

            while (halfHeight / inSampleSize >= maxHeight && halfWidth / inSampleSize >= maxWidth) {

                inSampleSize *= 2

            }

        }

        return inSampleSize

    }



    private fun emptyScan(warnings: List<String>, sourceImageUris: List<String> = emptyList()) =

        ScannedPurchaseBill(

            supplierName = null,

            invoiceNumber = null,

            invoiceDate = null,

            lines = emptyList(),

            parseWarnings = warnings,

            sourceImageUris = sourceImageUris,

        )

}

