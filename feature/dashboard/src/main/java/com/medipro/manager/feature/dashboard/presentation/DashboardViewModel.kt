package com.medipro.manager.feature.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medipro.manager.core.common.FormatUtils
import com.medipro.manager.domain.usecase.dashboard.ObserveDashboardUseCase
import com.medipro.manager.domain.usecase.license.ObserveLicenseAccessStateUseCase
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
class DashboardViewModel @Inject constructor(
    private val observeDashboard: ObserveDashboardUseCase,
    observeLicenseAccessState: ObserveLicenseAccessStateUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<DashboardEvent>()
    val events: SharedFlow<DashboardEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            observeLicenseAccessState().collect { accessState ->
                _state.update { it.copy(licenseAccessState = accessState) }
            }
        }
        viewModelScope.launch {
            observeDashboard().collect { snapshot ->
                val stats = snapshot.stats
                _state.update {
                    DashboardState(
                        isLoading = false,
                        isRefreshing = false,
                        isFromCache = snapshot.isFromCache,
                        pharmacyName = snapshot.pharmacyName,
                        dateLabel = snapshot.dateLabel,
                        syncStatusLabel = snapshot.syncStatusLabel,
                        licenseDaysRemaining = snapshot.licenseDaysRemaining,
                        todaySales = FormatUtils.formatCurrency(stats.todaySales),
                        todaySalesReturn = FormatUtils.formatCurrency(stats.todaySalesReturn),
                        netSales = FormatUtils.formatCurrency(stats.netSales),
                        todayAdjustments = stats.todayAdjustments.toString(),
                        todayPurchase = FormatUtils.formatCurrency(stats.todayPurchase),
                        todayExpense = FormatUtils.formatCurrency(stats.todayExpense),
                        profit = FormatUtils.formatCurrency(stats.profit),
                        cashInDrawer = FormatUtils.formatCurrency(stats.cashInDrawer),
                        bankBalance = FormatUtils.formatCurrency(stats.bankBalance),
                        todaySalesCount = stats.todaySalesCount,
                        todayPurchaseCount = stats.todayPurchaseCount,
                        lowStockCount = stats.lowStockCount,
                        outOfStockCount = stats.outOfStockCount,
                        nearExpiryCount = stats.nearExpiryCount,
                        expiredCount = stats.expiredCount,
                        inventoryValue = FormatUtils.formatCurrency(stats.inventoryValue),
                        activeMedicineCount = FormatUtils.formatCount(stats.activeMedicineCount),
                        masterCatalogCount = FormatUtils.formatCount(snapshot.masterCatalogCount),
                        totalStockUnits = FormatUtils.formatCount(stats.totalStockUnits),
                        todayCustomers = stats.todayCustomers,
                        pendingCustomerDue = FormatUtils.formatCurrency(stats.pendingCustomerDue),
                        pendingCustomerDueCount = stats.pendingCustomerDueCount,
                        collectedToday = FormatUtils.formatCurrency(stats.collectedToday),
                        supplierDue = FormatUtils.formatCurrency(stats.supplierDue),
                        supplierDueCount = stats.supplierDueCount,
                        todaySupplierPayment = FormatUtils.formatCurrency(stats.todaySupplierPayment),
                        bestSellingMedicine = stats.bestSellingMedicine ?: "—",
                        topCategory = stats.topCategory ?: "—",
                        todayTransactions = stats.todayTransactions,
                        yesterdaySales = FormatUtils.formatCurrency(stats.yesterdaySales),
                        last7DaysSales = FormatUtils.formatCurrency(stats.last7DaysSales),
                        last30DaysSales = FormatUtils.formatCurrency(stats.last30DaysSales),
                        ocrTodayBills = stats.ocrAnalytics.todayBillsScanned.toString(),
                        ocrAverageAccuracy = "${stats.ocrAnalytics.averageAccuracyPercent}%",
                        ocrLearnedAliases = stats.ocrAnalytics.learnedAliasesCount.toString(),
                        ocrManualCorrections = stats.ocrAnalytics.manualCorrectionsToday.toString(),
                        ocrSavedTime = stats.ocrAnalytics.savedTimeLabel(),
                        ocrLastScanTime = stats.ocrAnalytics.lastScan?.let {
                            FormatUtils.formatRelativeTime(it.scannedAt)
                        } ?: "—",
                        ocrLastScanSupplier = stats.ocrAnalytics.lastScan?.supplierName?.takeIf { it.isNotBlank() } ?: "—",
                        ocrLastScanAccuracy = stats.ocrAnalytics.lastScan?.let { "${it.accuracyPercent}%" } ?: "—",
                        alerts = snapshot.alerts.map { a ->
                            AlertUi(a.message, a.severity, a.route)
                        },
                        recentSales = snapshot.recentSales.map { sale ->
                            RecentSaleUi(
                                id = sale.id,
                                invoiceNumber = sale.invoiceNumber,
                                amount = FormatUtils.formatCurrency(sale.totalAmount),
                                date = FormatUtils.formatDateTime(sale.saleDate),
                            )
                        },
                        recentPurchases = snapshot.recentPurchases.map { purchase ->
                            RecentPurchaseUi(
                                id = purchase.id,
                                invoiceNumber = purchase.invoiceNumber,
                                amount = FormatUtils.formatCurrency(purchase.totalAmount),
                                date = FormatUtils.formatDateTime(purchase.purchaseDate),
                            )
                        },
                        prescriptionModuleEnabled = snapshot.prescriptionModuleEnabled,
                        lastBackupLabel = snapshot.lastBackupLabel,
                        lastBackupEncrypted = snapshot.lastBackupEncrypted,
                        cloudBackupStatus = snapshot.cloudBackupStatus,
                        licenseMobile = snapshot.licenseMobile,
                        licensePlan = snapshot.licensePlan ?: "Free",
                        licenseValidUntil = snapshot.licenseValidUntil,
                        licenseLastVerified = snapshot.licenseLastVerified,
                    )
                }
            }
        }
    }

    fun refresh() {
        _state.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            _events.emit(DashboardEvent.Refresh)
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    fun onFeatureClick(route: String) {
        viewModelScope.launch { _events.emit(DashboardEvent.NavigateToFeature(route)) }
    }

    fun onSaleClick(saleId: Long) {
        viewModelScope.launch { _events.emit(DashboardEvent.OpenInvoice(saleId)) }
    }
}
