package com.medipro.manager.domain.usecase.purchase

import com.medipro.manager.domain.model.PurchaseReturn
import com.medipro.manager.domain.model.PurchaseReturnContext
import com.medipro.manager.domain.model.PurchaseReturnLine
import com.medipro.manager.domain.repository.PurchaseReturnRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPurchaseReturnContextUseCase @Inject constructor(
    private val repository: PurchaseReturnRepository,
) {
    suspend operator fun invoke(purchaseId: Long): PurchaseReturnContext? =
        repository.getReturnContext(purchaseId)
}

class CreatePurchaseReturnUseCase @Inject constructor(
    private val repository: PurchaseReturnRepository,
) {
    suspend operator fun invoke(
        purchaseId: Long,
        reason: String,
        lines: List<PurchaseReturnLine>,
        notes: String? = null,
    ): Long = repository.createPurchaseReturn(purchaseId, reason, lines, notes)
}

class GeneratePurchaseReturnNumberUseCase @Inject constructor(
    private val repository: PurchaseReturnRepository,
) {
    suspend operator fun invoke(): String = repository.generateReturnNumber()
}

class ObservePurchaseReturnsUseCase @Inject constructor(
    private val repository: PurchaseReturnRepository,
) {
    operator fun invoke(): Flow<List<PurchaseReturn>> = repository.observePurchaseReturns()
}
