package com.medipro.manager.data.repository

import androidx.room.withTransaction
import com.medipro.manager.core.database.MediProDatabase
import com.medipro.manager.core.database.dao.DayClosingDao
import com.medipro.manager.core.database.dao.ExpenseDao
import com.medipro.manager.core.database.dao.LedgerDao
import com.medipro.manager.core.database.dao.LicenseDao
import com.medipro.manager.core.database.dao.ReportDao
import com.medipro.manager.core.database.dao.RankedAggregateRow
import com.medipro.manager.core.database.dao.SaleDao
import com.medipro.manager.core.database.dao.SaleReturnDao
import com.medipro.manager.core.database.entity.AuditEventType
import com.medipro.manager.core.database.entity.DayClosingDifferenceReason
import com.medipro.manager.core.database.entity.DayClosingEntity
import com.medipro.manager.core.database.entity.ExpenseEntity
import com.medipro.manager.core.database.entity.LedgerEntity
import com.medipro.manager.core.database.dao.PaymentDao
import com.medipro.manager.data.document.report.DayClosingPdfGenerator
import com.medipro.manager.data.sync.pharmacyUuid
import com.medipro.manager.domain.model.AuditLog
import com.medipro.manager.domain.model.CashBookEntry
import com.medipro.manager.domain.model.DayClosingPreview
import com.medipro.manager.domain.model.DayClosingReason
import com.medipro.manager.domain.model.DayClosingRecord
import com.medipro.manager.domain.model.DayClosingResult
import com.medipro.manager.domain.model.Expense
import com.medipro.manager.domain.repository.AuditRepository
import com.medipro.manager.domain.repository.CashBookRepository
import com.medipro.manager.domain.repository.DayClosingRepository
import com.medipro.manager.domain.repository.ExpenseRepository
import com.medipro.manager.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val database: MediProDatabase,
    private val expenseDao: ExpenseDao,
    private val ledgerDao: LedgerDao,
    private val licenseDao: LicenseDao,
    private val auditRepository: AuditRepository,
) : ExpenseRepository {

    override fun observeExpenses(start: Long, end: Long): Flow<List<Expense>> =
        expenseDao.observeByDateRange(start, end).map { list -> list.map { it.toDomain() } }

    override fun observeTodayExpenses(): Flow<List<Expense>> {
        val (start, end) = dayRange(0)
        return observeExpenses(start, end)
    }

    override suspend fun recordExpense(
        category: String,
        description: String,
        amount: Double,
        paymentMethod: String,
    ): Long {
        require(amount > 0) { "Amount must be greater than zero" }
        val now = System.currentTimeMillis()
        val pharmacyUuid = licenseDao.pharmacyUuid()
        val deviceId = licenseDao.get()?.deviceId

        val expenseId = database.withTransaction {
            val id = expenseDao.insert(
                ExpenseEntity(
                    category = category,
                    description = description,
                    amount = amount,
                    paymentMethod = paymentMethod,
                    expenseDate = now,
                    createdAt = now,
                ),
            )

            if (paymentMethod == "CASH") {
                ledgerDao.insert(
                    LedgerEntity(
                        accountType = "CASH",
                        description = "Expense — ${ExpenseCategoriesLabel.label(category)}",
                        credit = amount,
                        referenceType = "EXPENSE",
                        referenceId = id,
                        pharmacyUuid = pharmacyUuid,
                        entryDate = now,
                        deviceId = deviceId,
                    ),
                )
            }
            ledgerDao.insert(
                LedgerEntity(
                    accountType = "EXPENSE",
                    description = description,
                    debit = amount,
                    referenceType = "EXPENSE",
                    referenceId = id,
                    pharmacyUuid = pharmacyUuid,
                    entryDate = now,
                    deviceId = deviceId,
                ),
            )
            id
        }

        auditRepository.log(
            AuditLog(
                uuid = UUID.randomUUID().toString(),
                eventType = AuditEventType.EXPENSE_RECORDED,
                entityType = "EXPENSE",
                entityUuid = expenseId.toString(),
                description = "Expense recorded: ${ExpenseCategoriesLabel.label(category)} — Rs. $amount",
                newValue = description,
            ),
        )
        return expenseId
    }

    private fun ExpenseEntity.toDomain() = Expense(
        id = id,
        category = category,
        description = description,
        amount = amount,
        paymentMethod = paymentMethod,
        expenseDate = expenseDate,
    )
}

private object ExpenseCategoriesLabel {
    fun label(category: String): String = when (category) {
        "TEA" -> "Tea & Refreshments"
        "TRANSPORT" -> "Transport"
        "ELECTRICITY" -> "Electricity"
        "SALARY_ADVANCE" -> "Salary Advance"
        "MISC" -> "Miscellaneous"
        else -> category.replace('_', ' ')
    }
}

@Singleton
class CashBookRepositoryImpl @Inject constructor(
    private val ledgerDao: LedgerDao,
    private val cashBookExporter: com.medipro.manager.data.export.CashBookExporter,
) : CashBookRepository {

    override fun observeCashBook(start: Long, end: Long): Flow<List<CashBookEntry>> =
        ledgerDao.observeByDateRange(start, end).map { entries ->
            entries
                .filter { it.accountType == "CASH" }
                .map { entry ->
                    CashBookEntry(
                        id = entry.id,
                        entryDate = entry.entryDate,
                        description = entry.description,
                        cashIn = entry.debit,
                        cashOut = entry.credit,
                        referenceType = entry.referenceType,
                    )
                }
        }

    override fun observeTodayCashBook(): Flow<List<CashBookEntry>> {
        val (start, end) = dayRange(0)
        return observeCashBook(start, end)
    }

    override suspend fun exportCashBook(
        entries: List<CashBookEntry>,
        openingBalance: Double,
        closingBalance: Double,
        dateLabel: String,
        format: com.medipro.manager.domain.model.ExportFormat,
    ): java.io.File = when (format) {
        com.medipro.manager.domain.model.ExportFormat.PDF ->
            cashBookExporter.exportPdf(entries, openingBalance, closingBalance, dateLabel)
        com.medipro.manager.domain.model.ExportFormat.CSV ->
            cashBookExporter.exportCsv(entries, openingBalance, closingBalance, dateLabel)
    }
}

@Singleton
class DayClosingRepositoryImpl @Inject constructor(
    private val database: MediProDatabase,
    private val dayClosingDao: DayClosingDao,
    private val saleDao: SaleDao,
    private val saleReturnDao: SaleReturnDao,
    private val paymentDao: PaymentDao,
    private val expenseDao: ExpenseDao,
    private val reportDao: ReportDao,
    private val licenseDao: LicenseDao,
    private val settingsRepository: SettingsRepository,
    private val auditRepository: AuditRepository,
    private val pdfGenerator: DayClosingPdfGenerator,
) : DayClosingRepository {

    override suspend fun getPreview(forDateMillis: Long): DayClosingPreview {
        val (dayStart, dayEnd) = dayRangeFor(forDateMillis)
        val existing = dayClosingDao.getByDate(dayStart)
        val metrics = computeMetrics(dayStart, dayEnd)
        return DayClosingPreview(
            closingDate = dayStart,
            dateLabel = DayClosingPdfGenerator.formatDateLabel(dayStart),
            isAlreadyClosed = existing != null,
            openingCash = metrics.openingCash,
            cashSales = metrics.cashSales,
            cardSales = metrics.cardSales,
            esewaSales = metrics.esewaSales,
            khaltiSales = metrics.khaltiSales,
            imeSales = metrics.imeSales,
            creditSales = metrics.creditSales,
            customerReceipts = metrics.customerReceipts,
            supplierPayments = metrics.supplierPayments,
            expenses = metrics.expenses,
            returnsAmount = metrics.returnsAmount,
            salesCount = metrics.salesCount,
            returnCount = metrics.returnCount,
            discountTotal = metrics.discountTotal,
            vatTotal = metrics.vatTotal,
            expectedCash = metrics.expectedCash,
            closedRecord = existing?.toDomain(),
        )
    }

    override suspend fun closeDay(
        actualCash: Double,
        remarks: String?,
        differenceReason: String?,
        forDateMillis: Long,
    ): DayClosingResult {
        val (dayStart, dayEnd) = dayRangeFor(forDateMillis)
        if (dayClosingDao.getByDate(dayStart) != null) {
            throw IllegalStateException("Day already closed")
        }

        val metrics = computeMetrics(dayStart, dayEnd)
        val difference = actualCash - metrics.expectedCash

        if (kotlin.math.abs(difference) > 0.01) {
            require(!differenceReason.isNullOrBlank()) {
                "Select short cash or excess cash when difference is not zero"
            }
            val validReason = differenceReason == DayClosingReason.SHORT_CASH ||
                differenceReason == DayClosingReason.EXCESS_CASH
            require(validReason) { "Invalid difference reason" }
        }

        val reference = closingReference()
        val pharmacyUuid = licenseDao.pharmacyUuid()
        val deviceId = licenseDao.get()?.deviceId
        val now = System.currentTimeMillis()

        val entity = DayClosingEntity(
            pharmacyUuid = pharmacyUuid,
            closingDate = dayStart,
            openingCash = metrics.openingCash,
            cashSales = metrics.cashSales,
            cardSales = metrics.cardSales,
            esewaSales = metrics.esewaSales,
            khaltiSales = metrics.khaltiSales,
            imeSales = metrics.imeSales,
            creditSales = metrics.creditSales,
            customerReceipts = metrics.customerReceipts,
            supplierPayments = metrics.supplierPayments,
            expenses = metrics.expenses,
            returnsAmount = metrics.returnsAmount,
            salesCount = metrics.salesCount,
            returnCount = metrics.returnCount,
            discountTotal = metrics.discountTotal,
            vatTotal = metrics.vatTotal,
            expectedCash = metrics.expectedCash,
            actualCash = actualCash,
            difference = difference,
            differenceReason = differenceReason?.takeIf { kotlin.math.abs(difference) > 0.01 },
            remarks = remarks?.takeIf { it.isNotBlank() },
            reference = reference,
            closedAt = now,
            deviceId = deviceId,
            createdAt = now,
        )

        val closingId = dayClosingDao.insert(entity)
        val saved = dayClosingDao.getByDate(dayStart) ?: entity.copy(id = closingId)
        val record = saved.toDomain()

        auditRepository.log(
            AuditLog(
                uuid = UUID.randomUUID().toString(),
                eventType = AuditEventType.DAY_CLOSED,
                entityType = "DAY_CLOSING",
                entityUuid = saved.uuid,
                description = "Day closed: ${record.dateLabel} — diff ${String.format(Locale.US, "%.2f", difference)}",
                oldValue = "expected=${metrics.expectedCash}",
                newValue = "actual=$actualCash",
            ),
        )

        val pdfPath = runCatching {
            generateDayReportPdf(closingId)
        }.getOrNull()

        return DayClosingResult(record = record.copy(pdfPath = pdfPath), pdfPath = pdfPath)
    }

    override suspend fun generateDayReportPdf(closingId: Long): String? {
        val entity = dayClosingDao.getById(closingId) ?: return null
        val settings = settingsRepository.getSettings()
        val file = pdfGenerator.generate(
            com.medipro.manager.data.document.report.DayClosingRenderInput(
                record = entity.toDomain(),
                settings = settings,
            ),
        )
        val path = file.absolutePath
        dayClosingDao.update(entity.copy(pdfPath = path))
        return path
    }

    override fun observeRecentClosings(limit: Int): Flow<List<DayClosingRecord>> =
        dayClosingDao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    private data class DayMetrics(
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
    )

    private suspend fun computeMetrics(dayStart: Long, dayEnd: Long): DayMetrics {
        val openingCash = dayClosingDao.getLatestBefore(dayStart)?.actualCash ?: 0.0
        val salesByMethod = reportDao.getSalesByPaymentMethod(dayStart, dayEnd)
        val cashSales = salesByMethod.amountFor("CASH")
        val cardSales = salesByMethod.amountFor("CARD")
        val esewaSales = salesByMethod.amountFor("ESEWA")
        val khaltiSales = salesByMethod.amountFor("KHALTI")
        val imeSales = salesByMethod.amountFor("IME_PAY")
        val creditSales = saleDao.getCreditSales(dayStart, dayEnd)
        val customerReceipts = paymentDao.getCashTotalByType("CUSTOMER_RECEIPT", dayStart, dayEnd)
        val supplierPayments = paymentDao.getCashTotalByType("SUPPLIER_PAYMENT", dayStart, dayEnd)
        val expenses = expenseDao.getCashTotal(dayStart, dayEnd)
        val returnsAmount = saleReturnDao.getTotalReturnPaisaForDay(dayStart, dayEnd) / 100.0
        val salesCount = saleDao.countForDay(dayStart, dayEnd)
        val returnCount = saleReturnDao.countForDay(dayStart, dayEnd)
        val discountTotal = reportDao.getSalesDiscountTotal(dayStart, dayEnd)
        val vatTotal = reportDao.getSalesVatTotal(dayStart, dayEnd)

        val expectedCash = openingCash + cashSales + customerReceipts - supplierPayments - expenses - returnsAmount

        return DayMetrics(
            openingCash = openingCash,
            cashSales = cashSales,
            cardSales = cardSales,
            esewaSales = esewaSales,
            khaltiSales = khaltiSales,
            imeSales = imeSales,
            creditSales = creditSales,
            customerReceipts = customerReceipts,
            supplierPayments = supplierPayments,
            expenses = expenses,
            returnsAmount = returnsAmount,
            salesCount = salesCount,
            returnCount = returnCount,
            discountTotal = discountTotal,
            vatTotal = vatTotal,
            expectedCash = expectedCash,
        )
    }

    private fun List<RankedAggregateRow>.amountFor(method: String): Double =
        find { it.name.equals(method, ignoreCase = true) }?.amount ?: 0.0

    private fun DayClosingEntity.toDomain() = DayClosingRecord(
        id = id,
        uuid = uuid,
        closingDate = closingDate,
        dateLabel = DayClosingPdfGenerator.formatDateLabel(closingDate),
        openingCash = openingCash,
        cashSales = cashSales,
        cardSales = cardSales,
        esewaSales = esewaSales,
        khaltiSales = khaltiSales,
        imeSales = imeSales,
        creditSales = creditSales,
        customerReceipts = customerReceipts,
        supplierPayments = supplierPayments,
        expenses = expenses,
        returnsAmount = returnsAmount,
        salesCount = salesCount,
        returnCount = returnCount,
        discountTotal = discountTotal,
        vatTotal = vatTotal,
        expectedCash = expectedCash,
        actualCash = actualCash,
        difference = difference,
        differenceReason = differenceReason,
        remarks = remarks,
        reference = reference,
        pdfPath = pdfPath,
        closedAt = closedAt,
    )

    private fun closingReference(): String {
        val date = SimpleDateFormat("yyMMdd", Locale.getDefault()).format(Date())
        val suffix = (System.currentTimeMillis() % 10000).toString().padStart(4, '0')
        return "DC-$date-$suffix"
    }
}

private fun dayRange(offsetDays: Int): Pair<Long, Long> {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_MONTH, offsetDays)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val start = calendar.timeInMillis
    calendar.add(Calendar.DAY_OF_MONTH, 1)
    val end = calendar.timeInMillis - 1
    return start to end
}

private fun dayRangeFor(epochMs: Long): Pair<Long, Long> {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = epochMs
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val start = calendar.timeInMillis
    calendar.add(Calendar.DAY_OF_MONTH, 1)
    val end = calendar.timeInMillis - 1
    return start to end
}
