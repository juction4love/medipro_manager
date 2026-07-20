package com.medipro.manager.domain.usecase.sales

import com.medipro.manager.domain.model.toSaleError
import com.medipro.manager.domain.repository.SaleRepository
import javax.inject.Inject

class CancelSaleUseCase @Inject constructor(
    private val saleRepository: SaleRepository,
) {
    suspend operator fun invoke(saleId: Long): Result<Unit> = saleRepository.cancelSale(saleId)

    /** Maps a failed [invoke] result to a user-facing [SaleError], if present. */
    fun failureMessage(result: Result<Unit>): String? = result.toSaleError()?.userMessage
}
