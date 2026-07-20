package com.medipro.manager.domain.usecase.reports

import com.medipro.manager.core.common.DateRangeUtils
import com.medipro.manager.core.common.ReportPeriod
import com.medipro.manager.domain.model.ExportFormat
import com.medipro.manager.domain.model.ReportDashboardSummary
import com.medipro.manager.domain.model.ReportDateRange
import com.medipro.manager.domain.repository.ReportRepository
import javax.inject.Inject

class GetReportDashboardSummaryUseCase @Inject constructor(
    private val repository: ReportRepository,
) {
    suspend operator fun invoke(
        period: ReportPeriod,
        customStart: Long? = null,
        customEnd: Long? = null,
    ): ReportDashboardSummary =
        repository.getDashboardSummary(period.toReportDateRange(customStart, customEnd))
}

class GetSalesReportUseCase @Inject constructor(
    private val repository: ReportRepository,
) {
    suspend operator fun invoke(
        period: ReportPeriod,
        customStart: Long? = null,
        customEnd: Long? = null,
    ) = withRange(period, customStart, customEnd) { repository.getSalesReport(it) }
}

class GetPurchaseReportUseCase @Inject constructor(
    private val repository: ReportRepository,
) {
    suspend operator fun invoke(
        period: ReportPeriod,
        customStart: Long? = null,
        customEnd: Long? = null,
    ) = withRange(period, customStart, customEnd) { repository.getPurchaseReport(it) }
}

class GetInventoryReportUseCase @Inject constructor(
    private val repository: ReportRepository,
) {
    suspend operator fun invoke() = repository.getInventoryReport()
}

class GetFinancialReportUseCase @Inject constructor(
    private val repository: ReportRepository,
) {
    suspend operator fun invoke(
        period: ReportPeriod,
        customStart: Long? = null,
        customEnd: Long? = null,
    ) = withRange(period, customStart, customEnd) { repository.getFinancialReport(it) }
}

class GetCustomerReportUseCase @Inject constructor(
    private val repository: ReportRepository,
) {
    suspend operator fun invoke(
        period: ReportPeriod,
        customStart: Long? = null,
        customEnd: Long? = null,
    ) = withRange(period, customStart, customEnd) { repository.getCustomerReport(it) }
}

class GetSupplierReportUseCase @Inject constructor(
    private val repository: ReportRepository,
) {
    suspend operator fun invoke(
        period: ReportPeriod,
        customStart: Long? = null,
        customEnd: Long? = null,
    ) = withRange(period, customStart, customEnd) { repository.getSupplierReport(it) }
}

class GetMedicineAnalyticsUseCase @Inject constructor(
    private val repository: ReportRepository,
) {
    suspend operator fun invoke(
        period: ReportPeriod,
        customStart: Long? = null,
        customEnd: Long? = null,
    ) = withRange(period, customStart, customEnd) { repository.getMedicineAnalytics(it) }
}

class GetAuditReportUseCase @Inject constructor(
    private val repository: ReportRepository,
) {
    suspend operator fun invoke(
        period: ReportPeriod,
        customStart: Long? = null,
        customEnd: Long? = null,
    ): List<com.medipro.manager.domain.model.AuditReportRow> =
        withRange(period, customStart, customEnd) { repository.getAuditReport(it) }
}

class ExportReportUseCase @Inject constructor(
    private val repository: ReportRepository,
) {
    suspend operator fun invoke(
        period: ReportPeriod,
        tabLabel: String,
        format: ExportFormat,
        customStart: Long? = null,
        customEnd: Long? = null,
    ) = withRange(period, customStart, customEnd) { repository.exportReport(it, tabLabel, format) }
}

private inline fun <T> withRange(
    period: ReportPeriod,
    customStart: Long?,
    customEnd: Long?,
    block: (ReportDateRange) -> T,
): T = block(period.toReportDateRange(customStart, customEnd))

private fun ReportPeriod.toReportDateRange(customStart: Long?, customEnd: Long?): ReportDateRange {
    val (start, end) = toRange(customStart, customEnd)
    val label = if (this == ReportPeriod.CUSTOM && customStart != null && customEnd != null) {
        "${DateRangeUtils.formatShortDate(customStart)} – ${DateRangeUtils.formatShortDate(customEnd)}"
    } else {
        this.label
    }
    return ReportDateRange(this, start, end, label)
}
