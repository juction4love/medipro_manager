package com.medipro.manager.domain.usecase.accounting

import com.medipro.manager.domain.model.CashBookEntry
import com.medipro.manager.domain.model.ExportFormat
import com.medipro.manager.domain.repository.CashBookRepository
import java.io.File
import javax.inject.Inject

class ExportCashBookUseCase @Inject constructor(
    private val repository: CashBookRepository,
) {
    suspend operator fun invoke(
        entries: List<CashBookEntry>,
        openingBalance: Double,
        closingBalance: Double,
        dateLabel: String,
        format: ExportFormat,
    ): File = repository.exportCashBook(entries, openingBalance, closingBalance, dateLabel, format)
}
