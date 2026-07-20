package com.medipro.manager.data.di

import android.content.Context
import androidx.room.Room
import com.medipro.manager.core.database.MediProDatabase
import com.medipro.manager.core.database.migration.DatabaseMigrations
import com.medipro.manager.core.database.dao.AuditLogDao
import com.medipro.manager.core.database.dao.BackupHistoryDao
import com.medipro.manager.core.database.dao.BatchDao
import com.medipro.manager.core.database.dao.CustomerDao
import com.medipro.manager.core.database.dao.DayClosingDao
import com.medipro.manager.core.database.dao.ExpenseDao
import com.medipro.manager.core.database.dao.IncomeDao
import com.medipro.manager.core.database.dao.LedgerDao
import com.medipro.manager.core.database.dao.LicenseDao
import com.medipro.manager.core.database.dao.OcrMedicineAliasDao
import com.medipro.manager.core.database.dao.OcrScanSessionDao
import com.medipro.manager.core.database.dao.MedicineDao
import com.medipro.manager.core.database.dao.PaymentDao
import com.medipro.manager.core.database.dao.PendingOperationDao
import com.medipro.manager.core.database.dao.ReportDao
import com.medipro.manager.core.database.dao.PurchaseDao
import com.medipro.manager.core.database.dao.PurchaseItemDao
import com.medipro.manager.core.database.dao.PurchaseReturnDao
import com.medipro.manager.core.database.dao.PurchaseReturnItemDao
import com.medipro.manager.core.database.dao.SaleDao
import com.medipro.manager.core.database.dao.SaleItemDao
import com.medipro.manager.core.database.dao.SaleReturnDao
import com.medipro.manager.core.database.dao.SaleReturnItemDao
import com.medipro.manager.core.database.dao.SettingsDao
import com.medipro.manager.core.database.dao.StockAdjustmentDao
import com.medipro.manager.core.database.dao.StockDao
import com.medipro.manager.core.database.dao.SupplierDao
import com.medipro.manager.data.repository.AccountingDocumentRepositoryImpl
import com.medipro.manager.data.repository.AccountingRepositoryImpl
import com.medipro.manager.data.repository.AuditRepositoryImpl
import com.medipro.manager.data.repository.BackupRepositoryImpl
import com.medipro.manager.data.repository.CatalogRepositoryImpl
import com.medipro.manager.data.repository.CustomerRepositoryImpl
import com.medipro.manager.data.repository.DashboardRepositoryImpl
import com.medipro.manager.data.repository.CashBookRepositoryImpl
import com.medipro.manager.data.repository.DayClosingRepositoryImpl
import com.medipro.manager.data.repository.ExpenseRepositoryImpl
import com.medipro.manager.data.repository.GlobalSearchRepositoryImpl
import com.medipro.manager.data.repository.InventoryRepositoryImpl
import com.medipro.manager.data.repository.InvoiceRepositoryImpl
import com.medipro.manager.data.repository.LicenseRepositoryImpl
import com.medipro.manager.data.repository.MedicineRepositoryImpl
import com.medipro.manager.data.repository.OcrAnalyticsRepositoryImpl
import com.medipro.manager.data.repository.OcrFeedbackRepositoryImpl
import com.medipro.manager.data.repository.OcrMedicineAliasRepositoryImpl
import com.medipro.manager.data.repository.PosSearchRepositoryImpl
import com.medipro.manager.data.repository.PrinterRepositoryImpl
import com.medipro.manager.data.repository.PurchaseReturnRepositoryImpl
import com.medipro.manager.data.repository.PurchaseBillRepositoryImpl
import com.medipro.manager.data.repository.PurchaseRepositoryImpl
import com.medipro.manager.data.repository.ReportRepositoryImpl
import com.medipro.manager.data.repository.SaleReturnRepositoryImpl
import com.medipro.manager.data.repository.SaleRepositoryImpl
import com.medipro.manager.data.repository.SettingsRepositoryImpl
import com.medipro.manager.data.repository.SupplierRepositoryImpl
import com.medipro.manager.data.repository.SyncQueueRepositoryImpl
import com.medipro.manager.domain.repository.AccountingDocumentRepository
import com.medipro.manager.domain.repository.AccountingRepository
import com.medipro.manager.domain.repository.AuditRepository
import com.medipro.manager.domain.repository.BackupRepository
import com.medipro.manager.domain.repository.CatalogRepository
import com.medipro.manager.domain.repository.CustomerRepository
import com.medipro.manager.domain.repository.DashboardRepository
import com.medipro.manager.domain.repository.CashBookRepository
import com.medipro.manager.domain.repository.DayClosingRepository
import com.medipro.manager.domain.repository.ExpenseRepository
import com.medipro.manager.domain.repository.GlobalSearchRepository
import com.medipro.manager.domain.repository.InventoryRepository
import com.medipro.manager.domain.repository.InvoiceRepository
import com.medipro.manager.domain.repository.LicenseRepository
import com.medipro.manager.domain.repository.MedicineRepository
import com.medipro.manager.domain.repository.OcrAnalyticsRepository
import com.medipro.manager.domain.repository.OcrFeedbackRepository
import com.medipro.manager.domain.repository.OcrMedicineAliasRepository
import com.medipro.manager.domain.repository.PosSearchRepository
import com.medipro.manager.domain.repository.PrinterRepository
import com.medipro.manager.domain.repository.PurchaseReturnRepository
import com.medipro.manager.domain.repository.PurchaseBillRepository
import com.medipro.manager.domain.repository.PurchaseRepository
import com.medipro.manager.domain.repository.ReportRepository
import com.medipro.manager.domain.repository.SaleReturnRepository
import com.medipro.manager.domain.repository.SaleRepository
import com.medipro.manager.domain.repository.SettingsRepository
import com.medipro.manager.domain.repository.SupplierRepository
import com.medipro.manager.domain.repository.SyncQueueRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MediProDatabase =
        Room.databaseBuilder(
            context,
            MediProDatabase::class.java,
            MediProDatabase.DATABASE_NAME
        )
            .addMigrations(*DatabaseMigrations.ALL)
            .build()

    @Provides fun provideMedicineDao(db: MediProDatabase): MedicineDao = db.medicineDao()
    @Provides fun provideSupplierDao(db: MediProDatabase): SupplierDao = db.supplierDao()
    @Provides fun provideCustomerDao(db: MediProDatabase): CustomerDao = db.customerDao()
    @Provides fun provideBatchDao(db: MediProDatabase): BatchDao = db.batchDao()
    @Provides fun provideStockDao(db: MediProDatabase): StockDao = db.stockDao()
    @Provides fun providePurchaseDao(db: MediProDatabase): PurchaseDao = db.purchaseDao()
    @Provides fun providePurchaseItemDao(db: MediProDatabase): PurchaseItemDao = db.purchaseItemDao()
    @Provides fun providePurchaseReturnDao(db: MediProDatabase): PurchaseReturnDao = db.purchaseReturnDao()
    @Provides fun providePurchaseReturnItemDao(db: MediProDatabase): PurchaseReturnItemDao = db.purchaseReturnItemDao()
    @Provides fun provideSaleDao(db: MediProDatabase): SaleDao = db.saleDao()
    @Provides fun provideSaleItemDao(db: MediProDatabase): SaleItemDao = db.saleItemDao()
    @Provides fun provideSaleReturnDao(db: MediProDatabase): SaleReturnDao = db.saleReturnDao()
    @Provides fun provideSaleReturnItemDao(db: MediProDatabase): SaleReturnItemDao = db.saleReturnItemDao()
    @Provides fun provideStockAdjustmentDao(db: MediProDatabase): StockAdjustmentDao = db.stockAdjustmentDao()
    @Provides fun provideExpenseDao(db: MediProDatabase): ExpenseDao = db.expenseDao()
    @Provides fun provideIncomeDao(db: MediProDatabase): IncomeDao = db.incomeDao()
    @Provides fun provideLedgerDao(db: MediProDatabase): LedgerDao = db.ledgerDao()
    @Provides fun providePaymentDao(db: MediProDatabase): PaymentDao = db.paymentDao()
    @Provides fun provideBackupHistoryDao(db: MediProDatabase): BackupHistoryDao = db.backupHistoryDao()
    @Provides fun provideLicenseDao(db: MediProDatabase): LicenseDao = db.licenseDao()
    @Provides fun provideSettingsDao(db: MediProDatabase): SettingsDao = db.settingsDao()
    @Provides fun provideAuditLogDao(db: MediProDatabase): AuditLogDao = db.auditLogDao()
    @Provides fun provideReportDao(db: MediProDatabase): ReportDao = db.reportDao()
    @Provides fun provideDayClosingDao(db: MediProDatabase): DayClosingDao = db.dayClosingDao()
    @Provides fun provideOcrMedicineAliasDao(db: MediProDatabase): OcrMedicineAliasDao = db.ocrMedicineAliasDao()
    @Provides fun provideOcrScanSessionDao(db: MediProDatabase): OcrScanSessionDao = db.ocrScanSessionDao()
    @Provides fun providePendingOperationDao(db: MediProDatabase): PendingOperationDao = db.pendingOperationDao()

    @Provides
    @Singleton
    fun provideSearchAnalytics(): com.medipro.manager.core.search.SearchAnalytics =
        com.medipro.manager.core.search.SearchAnalytics()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMedicineRepository(impl: MedicineRepositoryImpl): MedicineRepository

    @Binds
    @Singleton
    abstract fun bindGlobalSearchRepository(impl: GlobalSearchRepositoryImpl): GlobalSearchRepository

    @Binds
    @Singleton
    abstract fun bindDashboardRepository(impl: DashboardRepositoryImpl): DashboardRepository

    @Binds
    @Singleton
    abstract fun bindLicenseRepository(impl: LicenseRepositoryImpl): LicenseRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindSaleReturnRepository(impl: SaleReturnRepositoryImpl): SaleReturnRepository

    @Binds
    @Singleton
    abstract fun bindSaleRepository(impl: SaleRepositoryImpl): SaleRepository

    @Binds
    @Singleton
    abstract fun bindCatalogRepository(impl: CatalogRepositoryImpl): CatalogRepository

    @Binds
    @Singleton
    abstract fun bindPosSearchRepository(impl: PosSearchRepositoryImpl): PosSearchRepository

    @Binds
    @Singleton
    abstract fun bindCustomerRepository(impl: CustomerRepositoryImpl): CustomerRepository

    @Binds
    @Singleton
    abstract fun bindSupplierRepository(impl: SupplierRepositoryImpl): SupplierRepository

    @Binds
    @Singleton
    abstract fun bindPurchaseReturnRepository(impl: PurchaseReturnRepositoryImpl): PurchaseReturnRepository

    @Binds
    @Singleton
    abstract fun bindReportRepository(impl: ReportRepositoryImpl): ReportRepository

    @Binds
    @Singleton
    abstract fun bindPurchaseRepository(impl: PurchaseRepositoryImpl): PurchaseRepository

    @Binds
    @Singleton
    abstract fun bindPurchaseBillRepository(impl: PurchaseBillRepositoryImpl): PurchaseBillRepository

    @Binds
    @Singleton
    abstract fun bindOcrMedicineAliasRepository(impl: OcrMedicineAliasRepositoryImpl): OcrMedicineAliasRepository

    @Binds
    @Singleton
    abstract fun bindOcrAnalyticsRepository(impl: OcrAnalyticsRepositoryImpl): OcrAnalyticsRepository

    @Binds
    @Singleton
    abstract fun bindOcrFeedbackRepository(impl: OcrFeedbackRepositoryImpl): OcrFeedbackRepository

    @Binds
    @Singleton
    abstract fun bindInventoryRepository(impl: InventoryRepositoryImpl): InventoryRepository

    @Binds
    @Singleton
    abstract fun bindInvoiceRepository(impl: InvoiceRepositoryImpl): InvoiceRepository

    @Binds
    @Singleton
    abstract fun bindThermalPrinterAdapter(
        impl: com.medipro.manager.data.document.printing.BluetoothThermalPrinterAdapter,
    ): com.medipro.manager.data.document.printing.ThermalPrinterAdapter

    @Binds
    @Singleton
    abstract fun bindAccountingRepository(impl: AccountingRepositoryImpl): AccountingRepository

    @Binds
    @Singleton
    abstract fun bindAccountingDocumentRepository(
        impl: AccountingDocumentRepositoryImpl,
    ): AccountingDocumentRepository

    @Binds
    @Singleton
    abstract fun bindExpenseRepository(impl: ExpenseRepositoryImpl): ExpenseRepository

    @Binds
    @Singleton
    abstract fun bindDayClosingRepository(impl: DayClosingRepositoryImpl): DayClosingRepository

    @Binds
    @Singleton
    abstract fun bindCashBookRepository(impl: CashBookRepositoryImpl): CashBookRepository

    @Binds
    @Singleton
    abstract fun bindPrinterRepository(impl: PrinterRepositoryImpl): PrinterRepository

    @Binds
    @Singleton
    abstract fun bindAuditRepository(impl: AuditRepositoryImpl): AuditRepository

    @Binds
    @Singleton
    abstract fun bindSyncQueueRepository(impl: SyncQueueRepositoryImpl): SyncQueueRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository
}
