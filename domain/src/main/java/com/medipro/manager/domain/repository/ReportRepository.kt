package com.medipro.manager.domain.repository

import com.medipro.manager.domain.model.AuditReportRow
import com.medipro.manager.domain.model.CustomerReportDetail
import com.medipro.manager.domain.model.ExportFormat
import com.medipro.manager.domain.model.FinancialReportDetail
import com.medipro.manager.domain.model.InventoryReportDetail
import com.medipro.manager.domain.model.MedicineAnalyticsDetail
import com.medipro.manager.domain.model.PurchaseReportDetail
import com.medipro.manager.domain.model.ReportDashboardSummary
import com.medipro.manager.domain.model.ReportDateRange
import com.medipro.manager.domain.model.SalesReportDetail
import com.medipro.manager.domain.model.SupplierReportDetail
import java.io.File

interface ReportRepository {
    suspend fun getDashboardSummary(range: ReportDateRange): ReportDashboardSummary
    suspend fun getSalesReport(range: ReportDateRange): SalesReportDetail
    suspend fun getPurchaseReport(range: ReportDateRange): PurchaseReportDetail
    suspend fun getInventoryReport(): InventoryReportDetail
    suspend fun getFinancialReport(range: ReportDateRange): FinancialReportDetail
    suspend fun getCustomerReport(range: ReportDateRange): CustomerReportDetail
    suspend fun getSupplierReport(range: ReportDateRange): SupplierReportDetail
    suspend fun getMedicineAnalytics(range: ReportDateRange): MedicineAnalyticsDetail
    suspend fun getAuditReport(range: ReportDateRange): List<AuditReportRow>
    suspend fun exportReport(range: ReportDateRange, tabLabel: String, format: ExportFormat): File
}
