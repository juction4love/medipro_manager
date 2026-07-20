package com.medipro.manager.feature.reports.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medipro.manager.core.common.DateRangeUtils
import com.medipro.manager.core.common.FormatUtils
import com.medipro.manager.core.common.ReportPeriod
import com.medipro.manager.domain.model.ExportFormat
import com.medipro.manager.domain.model.ReportDashboardSummary
import com.medipro.manager.domain.usecase.reports.ExportReportUseCase
import com.medipro.manager.domain.usecase.reports.GetAuditReportUseCase
import com.medipro.manager.domain.usecase.reports.GetCustomerReportUseCase
import com.medipro.manager.domain.usecase.reports.GetFinancialReportUseCase
import com.medipro.manager.domain.usecase.reports.GetInventoryReportUseCase
import com.medipro.manager.domain.usecase.reports.GetMedicineAnalyticsUseCase
import com.medipro.manager.domain.usecase.reports.GetPurchaseReportUseCase
import com.medipro.manager.domain.usecase.reports.GetReportDashboardSummaryUseCase
import com.medipro.manager.domain.usecase.reports.GetSalesReportUseCase
import com.medipro.manager.domain.usecase.reports.GetSupplierReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val getDashboardSummary: GetReportDashboardSummaryUseCase,
    private val getSalesReport: GetSalesReportUseCase,
    private val getPurchaseReport: GetPurchaseReportUseCase,
    private val getInventoryReport: GetInventoryReportUseCase,
    private val getFinancialReport: GetFinancialReportUseCase,
    private val getCustomerReport: GetCustomerReportUseCase,
    private val getSupplierReport: GetSupplierReportUseCase,
    private val getMedicineAnalytics: GetMedicineAnalyticsUseCase,
    private val getAuditReport: GetAuditReportUseCase,
    private val exportReport: ExportReportUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ReportsState())
    val state: StateFlow<ReportsState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ReportsEvent>()
    val events: SharedFlow<ReportsEvent> = _events.asSharedFlow()

    init {
        loadOverview()
    }

    fun onPeriodSelected(period: ReportPeriod) {
        if (period == ReportPeriod.CUSTOM) {
            val (start, _) = DateRangeUtils.dayRange(-6)
            val (_, end) = DateRangeUtils.dayRange(0)
            _state.update {
                it.copy(
                    selectedPeriod = period,
                    customStartMillis = it.customStartMillis ?: start,
                    customEndMillis = it.customEndMillis ?: end,
                    showCustomDatePicker = true,
                )
            }
            return
        }
        if (period == _state.value.selectedPeriod) return
        _state.update {
            it.copy(
                selectedPeriod = period,
                showCustomDatePicker = false,
                salesReport = null,
                purchaseReport = null,
                financialReport = null,
                customerReport = null,
                supplierReport = null,
                medicineAnalytics = null,
                auditRows = emptyList(),
            )
        }
        loadOverview()
        loadTab(_state.value.selectedTab, force = true)
    }

    fun onCustomRangeSelected(startMillis: Long, endMillis: Long) {
        _state.update {
            it.copy(
                selectedPeriod = ReportPeriod.CUSTOM,
                customStartMillis = startMillis,
                customEndMillis = endMillis,
                showCustomDatePicker = false,
                salesReport = null,
                purchaseReport = null,
                financialReport = null,
                customerReport = null,
                supplierReport = null,
                medicineAnalytics = null,
                auditRows = emptyList(),
            )
        }
        loadOverview()
        loadTab(_state.value.selectedTab, force = true)
    }

    fun dismissCustomDatePicker() = _state.update { it.copy(showCustomDatePicker = false) }

    fun onTabSelected(tab: ReportTab) {
        _state.update { it.copy(selectedTab = tab) }
        loadTab(tab)
    }

    fun exportCsv() = export(ExportFormat.CSV)

    fun exportPdf() = export(ExportFormat.PDF)

    private fun loadOverview() {
        val current = _state.value
        if (current.selectedPeriod == ReportPeriod.CUSTOM &&
            (current.customStartMillis == null || current.customEndMillis == null)
        ) {
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                getDashboardSummary(current.selectedPeriod, current.customStartMillis, current.customEndMillis)
            }.onSuccess { summary ->
                _state.update {
                    it.copy(isLoading = false, overviewKpis = summary.toKpis())
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(isLoading = false, errorMessage = error.message ?: "Failed to load reports")
                }
            }
        }
    }

    private fun loadTab(tab: ReportTab, force: Boolean = false) {
        if (!force && tabDataLoaded(tab)) return
        val current = _state.value
        if (current.selectedPeriod == ReportPeriod.CUSTOM &&
            (current.customStartMillis == null || current.customEndMillis == null)
        ) {
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isTabLoading = true, errorMessage = null) }
            val period = current.selectedPeriod
            val start = current.customStartMillis
            val end = current.customEndMillis
            runCatching {
                when (tab) {
                    ReportTab.OVERVIEW -> getDashboardSummary(period, start, end)
                    ReportTab.SALES -> getSalesReport(period, start, end)
                    ReportTab.PURCHASE -> getPurchaseReport(period, start, end)
                    ReportTab.INVENTORY -> getInventoryReport()
                    ReportTab.FINANCIAL -> getFinancialReport(period, start, end)
                    ReportTab.CUSTOMERS -> getCustomerReport(period, start, end)
                    ReportTab.SUPPLIERS -> getSupplierReport(period, start, end)
                    ReportTab.MEDICINE -> getMedicineAnalytics(period, start, end)
                    ReportTab.AUDIT -> getAuditReport(period, start, end)
                }
            }.onSuccess { result ->
                _state.update { s ->
                    when (tab) {
                        ReportTab.OVERVIEW -> s.copy(
                            isTabLoading = false,
                            overviewKpis = (result as ReportDashboardSummary).toKpis(),
                        )
                        ReportTab.SALES -> s.copy(isTabLoading = false, salesReport = result as com.medipro.manager.domain.model.SalesReportDetail)
                        ReportTab.PURCHASE -> s.copy(isTabLoading = false, purchaseReport = result as com.medipro.manager.domain.model.PurchaseReportDetail)
                        ReportTab.INVENTORY -> s.copy(isTabLoading = false, inventoryReport = result as com.medipro.manager.domain.model.InventoryReportDetail)
                        ReportTab.FINANCIAL -> s.copy(isTabLoading = false, financialReport = result as com.medipro.manager.domain.model.FinancialReportDetail)
                        ReportTab.CUSTOMERS -> s.copy(isTabLoading = false, customerReport = result as com.medipro.manager.domain.model.CustomerReportDetail)
                        ReportTab.SUPPLIERS -> s.copy(isTabLoading = false, supplierReport = result as com.medipro.manager.domain.model.SupplierReportDetail)
                        ReportTab.MEDICINE -> s.copy(isTabLoading = false, medicineAnalytics = result as com.medipro.manager.domain.model.MedicineAnalyticsDetail)
                        ReportTab.AUDIT -> s.copy(isTabLoading = false, auditRows = result as List<com.medipro.manager.domain.model.AuditReportRow>)
                    }
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(isTabLoading = false, errorMessage = error.message ?: "Failed to load tab")
                }
            }
        }
    }

    private fun export(format: ExportFormat) {
        val current = _state.value
        viewModelScope.launch {
            runCatching {
                exportReport(
                    current.selectedPeriod,
                    current.selectedTab.label,
                    format,
                    current.customStartMillis,
                    current.customEndMillis,
                )
            }.onSuccess { file ->
                val mime = when (format) {
                    ExportFormat.CSV -> "text/csv"
                    ExportFormat.PDF -> "application/pdf"
                }
                _events.emit(ReportsEvent.ShareExport(file.absolutePath, mime))
            }.onFailure { error ->
                _state.update { it.copy(errorMessage = error.message ?: "Export failed") }
            }
        }
    }

    private fun tabDataLoaded(tab: ReportTab): Boolean = when (tab) {
        ReportTab.OVERVIEW -> _state.value.overviewKpis.isNotEmpty()
        ReportTab.SALES -> _state.value.salesReport != null
        ReportTab.PURCHASE -> _state.value.purchaseReport != null
        ReportTab.INVENTORY -> _state.value.inventoryReport != null
        ReportTab.FINANCIAL -> _state.value.financialReport != null
        ReportTab.CUSTOMERS -> _state.value.customerReport != null
        ReportTab.SUPPLIERS -> _state.value.supplierReport != null
        ReportTab.MEDICINE -> _state.value.medicineAnalytics != null
        ReportTab.AUDIT -> _state.value.auditRows.isNotEmpty()
    }

    private fun ReportDashboardSummary.toKpis(): List<KpiUi> = listOf(
        KpiUi("Sales", FormatUtils.formatCurrency(sales)),
        KpiUi("Sales Return", FormatUtils.formatCurrency(salesReturn)),
        KpiUi("Net Sales", FormatUtils.formatCurrency(netSales)),
        KpiUi("Purchase", FormatUtils.formatCurrency(purchase)),
        KpiUi("Purchase Return", FormatUtils.formatCurrency(purchaseReturn)),
        KpiUi("Net Purchase", FormatUtils.formatCurrency(netPurchase)),
        KpiUi("Expense", FormatUtils.formatCurrency(expense)),
        KpiUi("Gross Profit", FormatUtils.formatCurrency(grossProfit)),
        KpiUi("Net Profit", FormatUtils.formatCurrency(netProfit)),
        KpiUi("Margin %", String.format("%.1f%%", marginPercent)),
        KpiUi("Cash Balance", FormatUtils.formatCurrency(cashBalance)),
        KpiUi("Credit Sales", FormatUtils.formatCurrency(creditSales)),
        KpiUi("Customer Due", FormatUtils.formatCurrency(customerDue)),
        KpiUi("Supplier Due", FormatUtils.formatCurrency(supplierDue)),
        KpiUi("VAT Collected", FormatUtils.formatCurrency(vatCollected)),
        KpiUi("VAT Paid", FormatUtils.formatCurrency(vatPaid)),
        KpiUi("Discount", FormatUtils.formatCurrency(discountGiven)),
        KpiUi("Inventory Value", FormatUtils.formatCurrency(inventoryValue)),
        KpiUi("Low Stock", lowStockCount.toString()),
        KpiUi("Out of Stock", outOfStockCount.toString()),
        KpiUi("Near Expiry", nearExpiryCount.toString()),
        KpiUi("Expired", expiredCount.toString()),
    )
}
