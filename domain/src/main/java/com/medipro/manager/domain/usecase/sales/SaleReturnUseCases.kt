package com.medipro.manager.domain.usecase.sales

import com.medipro.manager.domain.model.SaleReturn
import com.medipro.manager.domain.model.SaleReturnContext
import com.medipro.manager.domain.model.SaleReturnLine
import com.medipro.manager.domain.repository.SaleReturnRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSaleReturnContextUseCase @Inject constructor(
    private val repository: SaleReturnRepository,
) {
    suspend operator fun invoke(saleId: Long): SaleReturnContext? =
        repository.getReturnContext(saleId)
}

class CreateSaleReturnUseCase @Inject constructor(
    private val repository: SaleReturnRepository,
) {
    suspend operator fun invoke(
        saleId: Long,
        reason: String,
        lines: List<SaleReturnLine>,
        notes: String? = null,
    ): Long = repository.createSaleReturn(saleId, reason, lines, notes)
}

class ObserveSaleReturnsUseCase @Inject constructor(
    private val repository: SaleReturnRepository,
) {
    operator fun invoke(): Flow<List<SaleReturn>> = repository.observeSaleReturns()
}
