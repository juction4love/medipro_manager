package com.medipro.manager.data.cache

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.medipro.manager.domain.model.AlertSeverity
import com.medipro.manager.domain.model.DashboardAlert
import com.medipro.manager.domain.model.DashboardSnapshot
import com.medipro.manager.domain.model.DashboardStats
import com.medipro.manager.domain.model.Purchase
import com.medipro.manager.domain.model.Sale
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dashboardCacheStore: DataStore<Preferences> by preferencesDataStore("dashboard_cache")

@Singleton
class DashboardCacheStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val cacheKey = stringPreferencesKey("dashboard_snapshot")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun read(): DashboardSnapshot? {
        val raw = context.dashboardCacheStore.data.map { it[cacheKey] }.first() ?: return null
        return runCatching {
            json.decodeFromString<DashboardCachePayload>(raw).toDomain()
        }.getOrNull()
    }

    suspend fun write(snapshot: DashboardSnapshot) {
        val payload = snapshot.toCachePayload()
        context.dashboardCacheStore.edit {
            it[cacheKey] = json.encodeToString(payload)
        }
    }

    suspend fun clear() {
        context.dashboardCacheStore.edit { it.remove(cacheKey) }
    }
}

private fun DashboardSnapshot.toCachePayload(): DashboardCachePayload = DashboardCachePayload(
    pharmacyName = pharmacyName,
    dateLabel = dateLabel,
    stats = CachedDashboardStats(
        todaySales = stats.todaySales,
        todayPurchase = stats.todayPurchase,
        todayExpense = stats.todayExpense,
        profit = stats.profit,
        cashInDrawer = stats.cashInDrawer,
        bankBalance = stats.bankBalance,
        todaySalesCount = stats.todaySalesCount,
        todayPurchaseCount = stats.todayPurchaseCount,
        lowStockCount = stats.lowStockCount,
        outOfStockCount = stats.outOfStockCount,
        nearExpiryCount = stats.nearExpiryCount,
        expiredCount = stats.expiredCount,
        inventoryValue = stats.inventoryValue,
        activeMedicineCount = stats.activeMedicineCount,
        totalStockUnits = stats.totalStockUnits,
        todayCustomers = stats.todayCustomers,
        pendingCustomerDue = stats.pendingCustomerDue,
        pendingCustomerDueCount = stats.pendingCustomerDueCount,
        collectedToday = stats.collectedToday,
        supplierDue = stats.supplierDue,
        supplierDueCount = stats.supplierDueCount,
        todaySupplierPayment = stats.todaySupplierPayment,
        bestSellingMedicine = stats.bestSellingMedicine,
        topCategory = stats.topCategory,
        todayTransactions = stats.todayTransactions,
        yesterdaySales = stats.yesterdaySales,
        last7DaysSales = stats.last7DaysSales,
        last30DaysSales = stats.last30DaysSales,
    ),
    alerts = alerts.map {
        CachedDashboardAlert(it.message, it.severity.name, it.route)
    },
    recentSales = recentSales.map {
        CachedRecentSale(it.id, it.invoiceNumber, it.totalAmount, it.saleDate)
    },
    recentPurchases = recentPurchases.map {
        CachedRecentPurchase(it.id, it.invoiceNumber, it.totalAmount, it.purchaseDate)
    },
    licenseMobile = licenseMobile,
    licensePlan = licensePlan,
    licenseDaysRemaining = licenseDaysRemaining,
    licenseValidUntil = licenseValidUntil,
    licenseLastVerified = licenseLastVerified,
    licenseExpired = licenseExpired,
    lastBackupLabel = lastBackupLabel,
    lastBackupEncrypted = lastBackupEncrypted,
    backupDueToday = backupDueToday,
    backupFailed = backupFailed,
    cloudBackupStatus = cloudBackupStatus,
    prescriptionModuleEnabled = prescriptionModuleEnabled,
    syncStatusLabel = syncStatusLabel,
    masterCatalogCount = masterCatalogCount,
    cachedAt = System.currentTimeMillis(),
)

private fun DashboardCachePayload.toDomain(): DashboardSnapshot = DashboardSnapshot(
    pharmacyName = pharmacyName,
    dateLabel = dateLabel,
    stats = DashboardStats(
        todaySales = stats.todaySales,
        todayPurchase = stats.todayPurchase,
        todayExpense = stats.todayExpense,
        profit = stats.profit,
        cashInDrawer = stats.cashInDrawer,
        bankBalance = stats.bankBalance,
        todaySalesCount = stats.todaySalesCount,
        todayPurchaseCount = stats.todayPurchaseCount,
        lowStockCount = stats.lowStockCount,
        outOfStockCount = stats.outOfStockCount,
        nearExpiryCount = stats.nearExpiryCount,
        expiredCount = stats.expiredCount,
        expiringCount = stats.nearExpiryCount,
        inventoryValue = stats.inventoryValue,
        activeMedicineCount = stats.activeMedicineCount,
        totalStockUnits = stats.totalStockUnits,
        todayCustomers = stats.todayCustomers,
        pendingCustomerDue = stats.pendingCustomerDue,
        pendingCustomerDueCount = stats.pendingCustomerDueCount,
        collectedToday = stats.collectedToday,
        supplierDue = stats.supplierDue,
        supplierDueCount = stats.supplierDueCount,
        todaySupplierPayment = stats.todaySupplierPayment,
        bestSellingMedicine = stats.bestSellingMedicine,
        topCategory = stats.topCategory,
        todayTransactions = stats.todayTransactions,
        yesterdaySales = stats.yesterdaySales,
        last7DaysSales = stats.last7DaysSales,
        last30DaysSales = stats.last30DaysSales,
    ),
    alerts = alerts.map {
        DashboardAlert(
            message = it.message,
            severity = runCatching { AlertSeverity.valueOf(it.severity) }.getOrDefault(AlertSeverity.INFO),
            route = it.route,
        )
    },
    recentSales = recentSales.map {
        Sale(id = it.id, invoiceNumber = it.invoiceNumber, totalAmount = it.totalAmount, saleDate = it.saleDate)
    },
    recentPurchases = recentPurchases.map {
        Purchase(id = it.id, supplierId = null, invoiceNumber = it.invoiceNumber, totalAmount = it.totalAmount, purchaseDate = it.purchaseDate)
    },
    licenseMobile = licenseMobile,
    licensePlan = licensePlan,
    licenseDaysRemaining = licenseDaysRemaining,
    licenseValidUntil = licenseValidUntil,
    licenseLastVerified = licenseLastVerified,
    licenseExpired = licenseExpired,
    lastBackupLabel = lastBackupLabel,
    lastBackupEncrypted = lastBackupEncrypted,
    backupDueToday = backupDueToday,
    backupFailed = backupFailed,
    cloudBackupConnected = false,
    cloudBackupStatus = cloudBackupStatus,
    prescriptionModuleEnabled = prescriptionModuleEnabled,
    syncStatusLabel = syncStatusLabel,
    masterCatalogCount = masterCatalogCount,
    isFromCache = true,
    cachedAt = cachedAt,
)
