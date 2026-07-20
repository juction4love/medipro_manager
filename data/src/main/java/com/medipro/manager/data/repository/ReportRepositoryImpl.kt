package com.medipro.manager.data.repository

import com.medipro.manager.core.database.dao.BatchDao
import com.medipro.manager.core.database.dao.CustomerDao
import com.medipro.manager.core.database.dao.DailyAggregateRow
import com.medipro.manager.core.database.dao.ExpenseDao
import com.medipro.manager.core.database.dao.LedgerDao
import com.medipro.manager.core.database.dao.PurchaseDao
import com.medipro.manager.core.database.dao.PurchaseReturnDao
import com.medipro.manager.core.database.dao.RankedAggregateRow
import com.medipro.manager.core.database.dao.ReportDao
import com.medipro.manager.core.database.dao.SaleDao
import com.medipro.manager.core.database.dao.SaleReturnDao
import com.medipro.manager.core.database.dao.StockDao
import com.medipro.manager.core.database.dao.SupplierDao
import com.medipro.manager.data.export.ReportExporter
import com.medipro.manager.domain.model.AuditReportRow
import com.medipro.manager.domain.model.CashFlowSummary
import com.medipro.manager.domain.model.CustomerReportDetail
import com.medipro.manager.domain.model.DailyAmountRow
import com.medipro.manager.domain.model.ExportFormat
import com.medipro.manager.domain.model.FinancialReportDetail
import com.medipro.manager.domain.model.InventoryReportDetail
import com.medipro.manager.domain.model.MedicineAnalyticsDetail
import com.medipro.manager.domain.model.PurchaseReportDetail
import com.medipro.manager.domain.model.RankedRow
import com.medipro.manager.domain.model.ReportDashboardSummary
import com.medipro.manager.domain.model.ReportDateRange
import com.medipro.manager.domain.model.SalesReportDetail
import com.medipro.manager.domain.model.SupplierReportDetail
import com.medipro.manager.domain.model.VatSummary
import com.medipro.manager.domain.repository.ReportRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepositoryImpl @Inject constructor(
    private val reportDao: ReportDao,
    private val saleDao: SaleDao,
    private val saleReturnDao: SaleReturnDao,
    private val purchaseDao: PurchaseDao,
    private val purchaseReturnDao: PurchaseReturnDao,
    private val expenseDao: ExpenseDao,
    private val ledgerDao: LedgerDao,
    private val stockDao: StockDao,
    private val batchDao: BatchDao,
    private val customerDao: CustomerDao,
    private val supplierDao: SupplierDao,
    private val reportExporter: ReportExporter,
) : ReportRepository {

    override suspend fun getDashboardSummary(range: ReportDateRange): ReportDashboardSummary {
        val start = range.startMillis
        val end = range.endMillis
        val sales = reportDao.getSalesTotal(start, end)
        val salesReturn = saleReturnDao.getTotalReturnPaisaForDay(start, end) / 100.0
        val purchase = reportDao.getPurchaseTotal(start, end)
        val purchaseReturn = purchaseReturnDao.getTotalReturnPaisaForDay(start, end) / 100.0
        val expense = expenseDao.getTotal(start, end)
        val grossProfit = reportDao.getGrossProfitEstimate(start, end)
        val netSales = sales - salesReturn
        val netPurchase = purchase - purchaseReturn
        val netProfit = netSales - netPurchase - expense
        val marginPercent = if (netSales > 0) (grossProfit / netSales) * 100.0 else 0.0
        val now = System.currentTimeMillis()

        return ReportDashboardSummary(
            sales = sales,
            salesReturn = salesReturn,
            netSales = netSales,
            purchase = purchase,
            purchaseReturn = purchaseReturn,
            netPurchase = netPurchase,
            expense = expense,
            grossProfit = grossProfit,
            netProfit = netProfit,
            marginPercent = marginPercent,
            cashBalance = ledgerDao.getAccountBalance("CASH"),
            creditSales = saleDao.getCreditSales(start, end),
            cashSales = saleDao.getCashSales(start, end),
            customerDue = customerDao.getTotalOutstanding(),
            supplierDue = supplierDao.getTotalOutstanding(),
            vatCollected = reportDao.getSalesVatTotal(start, end),
            vatPaid = reportDao.getPurchaseVatTotal(start, end),
            discountGiven = reportDao.getSalesDiscountTotal(start, end),
            salesCount = saleDao.countForDay(start, end),
            purchaseCount = purchaseDao.countForDay(start, end),
            inventoryValue = stockDao.getInventoryValue(),
            lowStockCount = stockDao.countLowStock(),
            outOfStockCount = stockDao.countOutOfStock(),
            nearExpiryCount = batchDao.countExpiringWithin(now + DAY_MS * 30, now),
            expiredCount = batchDao.countExpired(now),
        )
    }

    override suspend fun getSalesReport(range: ReportDateRange): SalesReportDetail {
        val start = range.startMillis
        val end = range.endMillis
        val summary = getDashboardSummary(range)
        return SalesReportDetail(
            summary = summary,
            dailyBreakdown = reportDao.getDailySales(start, end).toDailyRows(),
            medicineWise = reportDao.getMedicineWiseSales(start, end).toRankedRows(),
            categoryWise = reportDao.getCategoryWiseSales(start, end).toRankedRows(),
            manufacturerWise = reportDao.getManufacturerWiseSales(start, end).toRankedRows(),
            paymentMethodWise = reportDao.getSalesByPaymentMethod(start, end).toRankedRows(),
        )
    }

    override suspend fun getPurchaseReport(range: ReportDateRange): PurchaseReportDetail {
        val start = range.startMillis
        val end = range.endMillis
        return PurchaseReportDetail(
            summary = getDashboardSummary(range),
            supplierWise = reportDao.getSupplierWisePurchase(start, end).toRankedRows(),
            dailyBreakdown = reportDao.getDailyPurchases(start, end).toDailyRows(),
        )
    }

    override suspend fun getInventoryReport(): InventoryReportDetail {
        val now = System.currentTimeMillis()
        val fastMoving = reportDao.getMedicineWiseSales(
            now - DAY_MS * 30,
            now,
            limit = 10,
        ).toRankedRows()
        return InventoryReportDetail(
            inventoryValue = stockDao.getInventoryValue(),
            totalUnits = stockDao.getTotalUnits(),
            lowStockCount = stockDao.countLowStock(),
            outOfStockCount = stockDao.countOutOfStock(),
            nearExpiry30 = batchDao.countExpiringWithin(now + DAY_MS * 30, now),
            nearExpiry60 = batchDao.countExpiringWithin(now + DAY_MS * 60, now),
            nearExpiry90 = batchDao.countExpiringWithin(now + DAY_MS * 90, now),
            expiredCount = batchDao.countExpired(now),
            fastMoving = fastMoving,
            slowMoving = reportDao.getSlowMovingMedicines(now - DAY_MS * 90, now).toRankedRows(),
            topValueMedicines = reportDao.getTopInventoryValue().toRankedRows(),
        )
    }

    override suspend fun getFinancialReport(range: ReportDateRange): FinancialReportDetail {
        val start = range.startMillis
        val end = range.endMillis
        val summary = getDashboardSummary(range)
        val salesVat = summary.vatCollected
        val purchaseVat = summary.vatPaid
        val cashIn = summary.cashSales + saleDao.getTotalCollectedToday(start, end)
        val cashOut = summary.purchase + summary.expense
        return FinancialReportDetail(
            summary = summary,
            vatSummary = VatSummary(
                salesVat = salesVat,
                purchaseVat = purchaseVat,
                netVat = salesVat - purchaseVat,
            ),
            cashFlow = CashFlowSummary(
                cashIn = cashIn,
                cashOut = cashOut,
                netCash = cashIn - cashOut,
            ),
        )
    }

    override suspend fun getCustomerReport(range: ReportDateRange): CustomerReportDetail {
        val start = range.startMillis
        val end = range.endMillis
        return CustomerReportDetail(
            topCustomers = reportDao.getTopCustomers(start, end).toRankedRows(),
            creditCustomers = reportDao.getCreditCustomers().toRankedRows(),
            totalOutstanding = customerDao.getTotalOutstanding(),
        )
    }

    override suspend fun getSupplierReport(range: ReportDateRange): SupplierReportDetail {
        val start = range.startMillis
        val end = range.endMillis
        return SupplierReportDetail(
            supplierDueList = reportDao.getSuppliersWithDue().toRankedRows(),
            totalOutstanding = supplierDao.getTotalOutstanding(),
            recentPurchases = reportDao.getSupplierWisePurchase(start, end, limit = 10).toRankedRows(),
        )
    }

    override suspend fun getMedicineAnalytics(range: ReportDateRange): MedicineAnalyticsDetail {
        val start = range.startMillis
        val end = range.endMillis
        return MedicineAnalyticsDetail(
            topSelling = reportDao.getMedicineWiseSales(start, end, limit = 10).toRankedRows(),
            leastSelling = reportDao.getLeastSellingMedicines(start, end).toRankedRows(),
            mostReturned = reportDao.getMostReturnedMedicines(start, end).toRankedRows(),
        )
    }

    override suspend fun getAuditReport(range: ReportDateRange): List<AuditReportRow> =
        reportDao.getAuditByEventType(range.startMillis, range.endMillis).map {
            AuditReportRow(eventType = it.name, count = it.quantity)
        }

    override suspend fun exportReport(
        range: ReportDateRange,
        tabLabel: String,
        format: ExportFormat,
    ): File {
        val summary = getDashboardSummary(range)
        return when (format) {
            ExportFormat.CSV -> reportExporter.exportCsv(range, tabLabel, summary)
            ExportFormat.PDF -> reportExporter.exportPdf(range, tabLabel, summary)
        }
    }

    private fun List<DailyAggregateRow>.toDailyRows() = map {
        DailyAmountRow(dayMillis = it.dayMillis, amount = it.amount, count = it.count)
    }

    private fun List<RankedAggregateRow>.toRankedRows() = map {
        RankedRow(name = it.name, quantity = it.quantity, amount = it.amount, subtitle = it.subtitle)
    }

    private companion object {
        const val DAY_MS = 86_400_000L
    }
}
