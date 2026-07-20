package com.medipro.manager.domain.usecase.purchase

import com.medipro.manager.domain.model.BillScanProgress
import com.medipro.manager.domain.model.OcrMedicineDraft
import com.medipro.manager.domain.model.PosSearchResult
import com.medipro.manager.domain.model.Purchase
import com.medipro.manager.domain.model.ScannedPurchaseBill
import com.medipro.manager.domain.model.Supplier
import com.medipro.manager.domain.repository.PurchaseBillRepository
import com.medipro.manager.domain.repository.PurchaseRepository
import javax.inject.Inject

class ScanPurchaseBillUseCase @Inject constructor(
    private val repository: PurchaseBillRepository,
) {
    suspend operator fun invoke(
        imageUris: List<String>,
        suppliers: List<Supplier>,
        onProgress: suspend (BillScanProgress) -> Unit = {},
    ): ScannedPurchaseBill = repository.scanBillFromImages(imageUris, suppliers, onProgress)
}

class ScanPurchaseBillFromTextUseCase @Inject constructor(
    private val repository: PurchaseBillRepository,
) {
    suspend operator fun invoke(
        ocrText: String,
        suppliers: List<Supplier>,
        onProgress: suspend (BillScanProgress) -> Unit = {},
    ): ScannedPurchaseBill = repository.scanBillFromText(ocrText, suppliers, onProgress)
}

class ResolveMedicineForPurchaseUseCase @Inject constructor(
    private val repository: PurchaseBillRepository,
) {
    suspend operator fun invoke(match: PosSearchResult?, fallbackBrandName: String): Long? =
        repository.resolveMedicineId(match, fallbackBrandName)
}

class CreateMedicineFromBillDraftUseCase @Inject constructor(
    private val repository: PurchaseBillRepository,
) {
    suspend operator fun invoke(draft: OcrMedicineDraft): Long = repository.createMedicineFromDraft(draft)
}

class SaveOcrMedicineAliasUseCase @Inject constructor(
    private val repository: PurchaseBillRepository,
) {
    suspend operator fun invoke(ocrDescription: String, medicineId: Long, medicineName: String) =
        repository.saveOcrAlias(ocrDescription, medicineId, medicineName)
}

class UpdateMedicineMrpFromBillUseCase @Inject constructor(
    private val repository: PurchaseBillRepository,
) {
    suspend operator fun invoke(medicineId: Long, newMrp: Double) =
        repository.updateMedicineMrp(medicineId, newMrp)
}

class LineMatchForMedicineUseCase @Inject constructor(
    private val repository: PurchaseBillRepository,
) {
    suspend operator fun invoke(
        parsed: com.medipro.manager.domain.model.ParsedPurchaseBillLine,
        medicineId: Long,
        viaAlias: Boolean = true,
    ) = repository.lineMatchForMedicine(parsed, medicineId, viaAlias)
}

class FindDuplicateSupplierBillUseCase @Inject constructor(
    private val purchaseRepository: PurchaseRepository,
) {
    suspend operator fun invoke(supplierBillNumber: String, supplierId: Long?): Purchase? =
        purchaseRepository.findBySupplierBillNumber(supplierBillNumber, supplierId)
}
