package com.medipro.manager.domain.repository

import com.medipro.manager.domain.model.CashBookEntry
import com.medipro.manager.domain.model.DayClosingPreview
import com.medipro.manager.domain.model.DayClosingRecord
import com.medipro.manager.domain.model.DayClosingResult
import com.medipro.manager.domain.model.Expense
import com.medipro.manager.domain.model.ExportFormat
import kotlinx.coroutines.flow.Flow
import java.io.File

interface ExpenseRepository {
    fun observeExpenses(start: Long, end: Long): Flow<List<Expense>>
    fun observeTodayExpenses(): Flow<List<Expense>>
    suspend fun recordExpense(
        category: String,
        description: String,
        amount: Double,
        paymentMethod: String = "CASH",
    ): Long
}

interface DayClosingRepository {
    suspend fun getPreview(forDateMillis: Long = System.currentTimeMillis()): DayClosingPreview
    suspend fun closeDay(
        actualCash: Double,
        remarks: String? = null,
        differenceReason: String? = null,
        forDateMillis: Long = System.currentTimeMillis(),
    ): DayClosingResult
    suspend fun generateDayReportPdf(closingId: Long): String?
    fun observeRecentClosings(limit: Int = 30): Flow<List<DayClosingRecord>>
}

interface CashBookRepository {
    fun observeCashBook(start: Long, end: Long): Flow<List<CashBookEntry>>
    fun observeTodayCashBook(): Flow<List<CashBookEntry>>
    suspend fun exportCashBook(
        entries: List<CashBookEntry>,
        openingBalance: Double,
        closingBalance: Double,
        dateLabel: String,
        format: ExportFormat,
    ): File
}
