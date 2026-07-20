package com.medipro.manager.domain.model

data class Expense(
    val id: Long,
    val category: String,
    val description: String,
    val amount: Double,
    val paymentMethod: String,
    val expenseDate: Long,
)

object ExpenseCategories {
    val ALL = listOf(
        "TEA",
        "TRANSPORT",
        "ELECTRICITY",
        "SALARY_ADVANCE",
        "MISC",
    )

    fun label(category: String): String = when (category) {
        "TEA" -> "Tea & Refreshments"
        "TRANSPORT" -> "Transport"
        "ELECTRICITY" -> "Electricity"
        "SALARY_ADVANCE" -> "Salary Advance"
        "MISC" -> "Miscellaneous"
        else -> category.replace('_', ' ')
    }
}

data class CashBookEntry(
    val id: Long,
    val entryDate: Long,
    val description: String,
    val cashIn: Double,
    val cashOut: Double,
    val referenceType: String?,
)

data class DayClosingPreview(
    val closingDate: Long,
    val dateLabel: String,
    val isAlreadyClosed: Boolean,
    val openingCash: Double,
    val cashSales: Double,
    val cardSales: Double,
    val esewaSales: Double,
    val khaltiSales: Double,
    val imeSales: Double,
    val creditSales: Double,
    val customerReceipts: Double,
    val supplierPayments: Double,
    val expenses: Double,
    val returnsAmount: Double,
    val salesCount: Int,
    val returnCount: Int,
    val discountTotal: Double,
    val vatTotal: Double,
    val expectedCash: Double,
    val closedRecord: DayClosingRecord? = null,
)

data class DayClosingRecord(
    val id: Long,
    val uuid: String,
    val closingDate: Long,
    val dateLabel: String,
    val openingCash: Double,
    val cashSales: Double,
    val cardSales: Double,
    val esewaSales: Double,
    val khaltiSales: Double,
    val imeSales: Double,
    val creditSales: Double,
    val customerReceipts: Double,
    val supplierPayments: Double,
    val expenses: Double,
    val returnsAmount: Double,
    val salesCount: Int,
    val returnCount: Int,
    val discountTotal: Double,
    val vatTotal: Double,
    val expectedCash: Double,
    val actualCash: Double,
    val difference: Double,
    val differenceReason: String?,
    val remarks: String?,
    val reference: String,
    val pdfPath: String?,
    val closedAt: Long,
)

data class DayClosingResult(
    val record: DayClosingRecord,
    val pdfPath: String?,
)

object DayClosingReason {
    const val SHORT_CASH = "SHORT_CASH"
    const val EXCESS_CASH = "EXCESS_CASH"

    fun label(reason: String?): String? = when (reason) {
        SHORT_CASH -> "Short Cash"
        EXCESS_CASH -> "Excess Cash"
        else -> null
    }
}

data class PaymentVoucherInput(
    val type: String,
    val title: String,
    val reference: String,
    val partyName: String,
    val amount: Double,
    val paymentMethod: String,
    val notes: String?,
    val paymentDate: Long,
    val previousBalance: Double = 0.0,
    val remainingBalance: Double = 0.0,
)
