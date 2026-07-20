package com.medipro.manager.data.repository



import com.medipro.manager.core.database.dao.BackupHistoryDao

import com.medipro.manager.core.database.dao.BatchDao

import com.medipro.manager.core.database.dao.CustomerDao

import com.medipro.manager.core.database.dao.ExpenseDao

import com.medipro.manager.core.database.dao.LedgerDao

import com.medipro.manager.core.database.dao.LicenseDao

import com.medipro.manager.core.database.dao.MedicineDao

import com.medipro.manager.core.database.dao.PurchaseDao

import com.medipro.manager.core.database.dao.PurchaseReturnDao

import com.medipro.manager.core.database.dao.SaleDao

import com.medipro.manager.core.database.dao.SaleReturnDao

import com.medipro.manager.core.database.dao.SaleItemDao

import com.medipro.manager.core.database.dao.SettingsDao

import com.medipro.manager.core.database.dao.StockAdjustmentDao

import com.medipro.manager.core.database.dao.StockDao

import com.medipro.manager.core.database.dao.SupplierDao

import com.medipro.manager.data.cache.DashboardCacheStore

import com.medipro.manager.data.mapper.toDomain

import com.medipro.manager.domain.model.AlertSeverity

import com.medipro.manager.domain.model.DashboardAlert

import com.medipro.manager.domain.model.DashboardSnapshot

import com.medipro.manager.domain.model.DashboardStats

import com.medipro.manager.domain.model.Sale

import com.medipro.manager.domain.repository.FirestoreSyncRepository
import com.medipro.manager.domain.repository.DashboardRepository

import kotlinx.coroutines.flow.Flow

import kotlinx.coroutines.flow.combine

import kotlinx.coroutines.flow.flatMapLatest

import kotlinx.coroutines.flow.flow

import kotlinx.coroutines.flow.map

import kotlinx.coroutines.flow.onEach

import kotlinx.coroutines.flow.onStart

import java.text.SimpleDateFormat

import java.util.Calendar

import java.util.Date

import java.util.Locale

import javax.inject.Inject

import javax.inject.Singleton



@Singleton

class DashboardRepositoryImpl @Inject constructor(

    private val saleDao: SaleDao,

    private val saleReturnDao: SaleReturnDao,

    private val saleItemDao: SaleItemDao,

    private val purchaseDao: PurchaseDao,

    private val purchaseReturnDao: PurchaseReturnDao,

    private val stockDao: StockDao,

    private val stockAdjustmentDao: StockAdjustmentDao,

    private val batchDao: BatchDao,

    private val expenseDao: ExpenseDao,

    private val medicineDao: MedicineDao,

    private val customerDao: CustomerDao,

    private val supplierDao: SupplierDao,

    private val settingsDao: SettingsDao,

    private val licenseDao: LicenseDao,

    private val backupHistoryDao: BackupHistoryDao,

    private val ledgerDao: LedgerDao,

    private val dashboardCache: DashboardCacheStore,

    private val firestoreSync: FirestoreSyncRepository,

    private val ocrAnalyticsRepository: com.medipro.manager.domain.repository.OcrAnalyticsRepository,

) : DashboardRepository {



    /** Active pharmacy data only — never loads catalog.db (271K). */

    override fun observeDashboard(): Flow<DashboardSnapshot> {

        val (todayStart, todayEnd) = dayRange(0)

        val core = combine(

            saleDao.observeByDateRange(todayStart, todayEnd),

            purchaseDao.observeByDateRange(todayStart, todayEnd),

            stockDao.observeLowStock(),

        ) { _, _, lowStock -> lowStock.size }



        val recent = combine(

            saleDao.observeRecent(4),

            purchaseDao.observeRecent(3),

        ) { sales, purchases -> sales to purchases }



        val meta = combine(

            settingsDao.observe(),

            licenseDao.observe(),

        ) { settings, license -> settings to license }



        return combine(

            combine(core, recent, meta) { lowStockCount, recentPair, metaPair ->

                DashboardBuildInput(

                    lowStockCount = lowStockCount,

                    settings = metaPair.first,

                    license = metaPair.second,

                    recentSales = recentPair.first,

                    recentPurchases = recentPair.second,

                )

            },

            firestoreSync.observeSyncStatus(),

        ) { input, syncStatus -> input to syncStatus }

        .flatMapLatest { (input, syncStatus) ->

            flow { emit(buildSnapshot(input, syncStatus)) }

        }.onStart {

            dashboardCache.read()?.let { cached ->

                emit(cached.copy(isFromCache = true))

            }

        }.onEach { snapshot ->

            if (!snapshot.isFromCache) {

                dashboardCache.write(snapshot)

            }

        }.map { snapshot ->

            if (snapshot.isFromCache) snapshot else snapshot.copy(isFromCache = false)

        }

    }



    override fun observeStats(): Flow<DashboardStats> =

        observeDashboard().map { it.stats }



    override fun observeRecentSales(): Flow<List<Sale>> =

        saleDao.observeRecent(10).map { sales -> sales.map { it.toDomain() } }



    private suspend fun buildSnapshot(
        input: DashboardBuildInput,
        syncStatus: com.medipro.manager.domain.model.SyncStatusSnapshot,
    ): DashboardSnapshot {

        val settings = input.settings

        val licenseEntity = input.license

        val recentSales = input.recentSales

        val recentPurchases = input.recentPurchases

        val lowStockCount = input.lowStockCount

        val now = System.currentTimeMillis()

        val (start, end) = dayRange(0)

        val nearExpiry = batchDao.countExpiringWithin(now + 30L * DAY_MS, now)

        val expired = batchDao.countExpired(now)

        val todaySalesCount = saleDao.countForDay(start, end)

        val todayPurchaseCount = purchaseDao.countForDay(start, end)

        val cashBalance = ledgerDao.getAccountBalance("CASH")

        val bankBalance = ledgerDao.getAccountBalance("BANK")

        val todaySales = saleDao.getTotalSales(start, end)

        val todaySalesReturn = saleReturnDao.getTotalReturnPaisaForDay(start, end) / 100.0

        val todayExpense = expenseDao.getTotal(start, end)

        val licenseExpired = licenseEntity?.expiresAt?.let { now > it } ?: false



        val stats = DashboardStats(

            todaySales = todaySales,

            todaySalesReturn = todaySalesReturn,

            netSales = todaySales - todaySalesReturn,

            todayPurchase = purchaseDao.getTotalPurchase(start, end),

            todayPurchaseReturn = purchaseReturnDao.getTotalReturnPaisaForDay(start, end) / 100.0,

            todayExpense = todayExpense,

            profit = calculateProfit(start, end),

            cashInDrawer = if (cashBalance != 0.0) cashBalance else saleDao.getCashSales(start, end),

            bankBalance = bankBalance,

            todaySalesCount = todaySalesCount,

            todayPurchaseCount = todayPurchaseCount,

            cashSales = saleDao.getCashSales(start, end),

            creditSales = saleDao.getCreditSales(start, end),

            lowStockCount = lowStockCount,

            outOfStockCount = stockDao.countOutOfStock(),

            nearExpiryCount = nearExpiry,

            expiredCount = expired,

            expiringCount = nearExpiry,

            inventoryValue = stockDao.getInventoryValue(),

            activeMedicineCount = medicineDao.count(),

            totalStockUnits = stockDao.getTotalUnits(),

            todayAdjustments = stockAdjustmentDao.countForDay(start, end),

            todayCustomers = customerDao.countDistinctCustomersForDay(start, end),

            pendingCustomerDue = customerDao.getTotalOutstanding(),

            pendingCustomerDueCount = customerDao.countWithOutstanding(),

            collectedToday = saleDao.getTotalCollectedToday(start, end),

            supplierDue = supplierDao.getTotalOutstanding(),

            supplierDueCount = supplierDao.countWithOutstanding(),

            todaySupplierPayment = purchaseDao.getTotalPaidToday(start, end),

            bestSellingMedicine = saleItemDao.getBestSellingToday(start, end)?.name,

            topCategory = saleItemDao.getTopCategoryToday(start, end)?.category,

            todayTransactions = todaySalesCount + todayPurchaseCount,

            pendingPurchaseCount = supplierDao.countWithOutstanding(),

            pendingPayments = purchaseDao.getPendingPayments(),

            yesterdaySales = salesForDay(-1),

            last7DaysSales = salesForRange(-6, 0),

            last30DaysSales = salesForRange(-29, 0),

            ocrAnalytics = ocrAnalyticsRepository.getAnalytics(),

        )



        val latestBackup = backupHistoryDao.getLatest()

        val backupToday = latestBackup?.let { isSameDay(it.createdAt, now) } ?: false



        return DashboardSnapshot(

            pharmacyName = settings?.pharmacyName?.takeIf { it.isNotBlank() }

                ?: licenseEntity?.pharmacyName?.takeIf { it.isNotBlank() }

                ?: "My Pharmacy",

            dateLabel = formatDateLabel(now),

            stats = stats,

            alerts = buildAlerts(stats, licenseExpired, backupFailed = false, backupDue = !backupToday),

            recentSales = recentSales.map { it.toDomain() },

            recentPurchases = recentPurchases.map { it.toDomain() },

            licenseMobile = licenseEntity?.mobileNumber,

            licensePlan = licenseEntity?.plan ?: "Free",

            licenseDaysRemaining = licenseEntity?.expiresAt?.let { exp ->

                ((exp - now) / DAY_MS).toInt().coerceAtLeast(0)

            },

            licenseValidUntil = licenseEntity?.expiresAt?.let { formatDisplayDate(it) },

            licenseLastVerified = licenseEntity?.lastVerifiedAt?.let { formatRelative(it, now) },

            licenseExpired = licenseExpired,

            lastBackupLabel = latestBackup?.let { formatRelative(it.createdAt, now) },

            lastBackupEncrypted = latestBackup?.isEncrypted ?: false,

            backupDueToday = !backupToday,

            backupFailed = false,

            cloudBackupConnected = false,

            cloudBackupStatus = "Not Connected",

            prescriptionModuleEnabled = settings?.prescriptionModuleEnabled ?: false,

            syncStatusLabel = when {
                syncStatus.pendingCount > 0 -> "Syncing (${syncStatus.pendingCount} pending)"
                syncStatus.isCloudEnabled -> "Live Sync"
                else -> "Offline Ready"
            },

            masterCatalogCount = MASTER_CATALOG_COUNT,

            cachedAt = now,

        )

    }



    private fun buildAlerts(

        stats: DashboardStats,

        licenseExpired: Boolean,

        backupFailed: Boolean,

        backupDue: Boolean,

    ): List<DashboardAlert> {

        val critical = mutableListOf<DashboardAlert>()

        val warning = mutableListOf<DashboardAlert>()

        val info = mutableListOf<DashboardAlert>()



        if (licenseExpired) {

            critical += DashboardAlert("License Expired", AlertSeverity.CRITICAL, "license")

        }

        if (stats.expiredCount > 0) {

            critical += DashboardAlert("Expired (${stats.expiredCount})", AlertSeverity.CRITICAL, "expiry")

        }

        if (backupFailed) {

            critical += DashboardAlert("Backup Failed", AlertSeverity.CRITICAL, "backup")

        }

        if (stats.outOfStockCount > 0) {

            critical += DashboardAlert("Out Of Stock (${stats.outOfStockCount})", AlertSeverity.CRITICAL, "inventory")

        }



        if (stats.nearExpiryCount > 0) {

            warning += DashboardAlert("Near Expiry (${stats.nearExpiryCount})", AlertSeverity.WARNING, "expiry")

        }

        if (stats.lowStockCount > 0) {

            warning += DashboardAlert("Low Stock (${stats.lowStockCount})", AlertSeverity.WARNING, "inventory")

        }

        if (stats.supplierDue > 0) {

            warning += DashboardAlert(

                "Supplier Due (${stats.supplierDueCount})",

                AlertSeverity.WARNING,

                "purchase",

            )

        }



        info += DashboardAlert("Offline Ready", AlertSeverity.INFO)

        if (backupDue && !backupFailed) {

            info += DashboardAlert("Backup not created today", AlertSeverity.INFO, "backup")

        }

        if (critical.isEmpty() && warning.isEmpty()) {

            info += DashboardAlert("Everything looks good", AlertSeverity.OK)

        }



        return critical + warning + info

    }



    private suspend fun calculateProfit(start: Long, end: Long): Double {

        val sales = saleDao.getTotalSales(start, end)

        val purchases = purchaseDao.getTotalPurchase(start, end)

        val expenses = expenseDao.getTotal(start, end)

        return sales - purchases - expenses

    }



    private suspend fun salesForDay(offsetDays: Int): Double {

        val (start, end) = dayRange(offsetDays)

        return saleDao.getTotalSales(start, end)

    }



    private suspend fun salesForRange(fromOffset: Int, toOffset: Int): Double {

        val (start, _) = dayRange(fromOffset)

        val (_, end) = dayRange(toOffset)

        return saleDao.getTotalSales(start, end)

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



    private fun isSameDay(epochMs: Long, now: Long): Boolean {

        val a = Calendar.getInstance().apply { timeInMillis = epochMs }

        val b = Calendar.getInstance().apply { timeInMillis = now }

        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&

            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    }



    private fun formatDateLabel(epochMs: Long): String =

        SimpleDateFormat("EEEE, d MMM yyyy", Locale.getDefault()).format(Date(epochMs))



    private fun formatDisplayDate(epochMs: Long): String =

        SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(epochMs))



    private fun formatRelative(epochMs: Long, now: Long): String {

        val diffDays = ((now - epochMs) / DAY_MS).toInt()

        return when {

            diffDays <= 0 -> "Today"

            diffDays == 1 -> "Yesterday"

            else -> "$diffDays days ago"

        }

    }



    companion object {

        private const val DAY_MS = 24L * 60 * 60 * 1000

        private const val MASTER_CATALOG_COUNT = 271_044

    }



    private data class DashboardBuildInput(

        val lowStockCount: Int,

        val settings: com.medipro.manager.core.database.entity.SettingsEntity?,

        val license: com.medipro.manager.core.database.entity.LicenseEntity?,

        val recentSales: List<com.medipro.manager.core.database.entity.SaleEntity>,

        val recentPurchases: List<com.medipro.manager.core.database.entity.PurchaseEntity>,

    )

}


