package com.medipro.manager.domain.repository

import com.medipro.manager.domain.model.BillScanProgress
import com.medipro.manager.domain.model.OcrMedicineDraft
import com.medipro.manager.domain.model.PosSearchResult
import com.medipro.manager.domain.model.ScannedPurchaseBill
import com.medipro.manager.domain.model.Supplier

interface PurchaseBillRepository {
    suspend fun scanBillFromImages(
        imageUris: List<String>,
        suppliers: List<Supplier>,
        onProgress: suspend (BillScanProgress) -> Unit = {},
    ): ScannedPurchaseBill

    suspend fun scanBillFromText(
        ocrText: String,
        suppliers: List<Supplier>,
        onProgress: suspend (BillScanProgress) -> Unit = {},
    ): ScannedPurchaseBill

    suspend fun resolveMedicineId(match: PosSearchResult?, fallbackBrandName: String): Long?
    suspend fun createMedicineFromDraft(draft: OcrMedicineDraft): Long
    suspend fun saveOcrAlias(ocrDescription: String, medicineId: Long, medicineName: String)
    suspend fun updateMedicineMrp(medicineId: Long, newMrp: Double)
    suspend fun enrichLineMatch(line: com.medipro.manager.domain.model.PurchaseBillLineMatch): com.medipro.manager.domain.model.PurchaseBillLineMatch
    suspend fun lineMatchForMedicine(
        parsed: com.medipro.manager.domain.model.ParsedPurchaseBillLine,
        medicineId: Long,
        viaAlias: Boolean = true,
    ): com.medipro.manager.domain.model.PurchaseBillLineMatch
}
