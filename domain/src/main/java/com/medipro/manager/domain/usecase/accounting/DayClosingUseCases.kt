package com.medipro.manager.domain.usecase.accounting

import com.medipro.manager.domain.repository.CashBookRepository
import com.medipro.manager.domain.repository.DayClosingRepository
import com.medipro.manager.domain.repository.ExpenseRepository
import javax.inject.Inject

class ObserveTodayExpensesUseCase @Inject constructor(
    private val repository: ExpenseRepository,
) {
    operator fun invoke() = repository.observeTodayExpenses()
}

class RecordExpenseUseCase @Inject constructor(
    private val repository: ExpenseRepository,
) {
    suspend operator fun invoke(
        category: String,
        description: String,
        amount: Double,
        paymentMethod: String = "CASH",
    ) = repository.recordExpense(category, description, amount, paymentMethod)
}

class GetDayClosingPreviewUseCase @Inject constructor(
    private val repository: DayClosingRepository,
) {
    suspend operator fun invoke(forDateMillis: Long = System.currentTimeMillis()) =
        repository.getPreview(forDateMillis)
}

class CloseDayUseCase @Inject constructor(
    private val repository: DayClosingRepository,
) {
    suspend operator fun invoke(
        actualCash: Double,
        remarks: String? = null,
        differenceReason: String? = null,
    ) = repository.closeDay(actualCash, remarks, differenceReason)
}

class GenerateDayClosingPdfUseCase @Inject constructor(
    private val repository: DayClosingRepository,
) {
    suspend operator fun invoke(closingId: Long) = repository.generateDayReportPdf(closingId)
}

class ObserveTodayCashBookUseCase @Inject constructor(
    private val repository: CashBookRepository,
) {
    operator fun invoke() = repository.observeTodayCashBook()
}
