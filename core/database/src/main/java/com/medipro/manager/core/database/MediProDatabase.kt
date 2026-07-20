package com.medipro.manager.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.medipro.manager.core.database.dao.AuditLogDao
import com.medipro.manager.core.database.dao.BackupHistoryDao
import com.medipro.manager.core.database.dao.BatchDao
import com.medipro.manager.core.database.dao.CustomerDao
import com.medipro.manager.core.database.dao.DayClosingDao
import com.medipro.manager.core.database.dao.ExpenseDao
import com.medipro.manager.core.database.dao.IncomeDao
import com.medipro.manager.core.database.dao.LedgerDao
import com.medipro.manager.core.database.dao.LicenseDao
import com.medipro.manager.core.database.dao.MedicineDao
import com.medipro.manager.core.database.dao.OcrMedicineAliasDao
import com.medipro.manager.core.database.entity.MedicineEntity
import com.medipro.manager.core.database.dao.PaymentDao
import com.medipro.manager.core.database.dao.PendingOperationDao
import com.medipro.manager.core.database.dao.ReportDao
import com.medipro.manager.core.database.dao.PurchaseDao
import com.medipro.manager.core.database.dao.PurchaseReturnDao
import com.medipro.manager.core.database.dao.PurchaseReturnItemDao
import com.medipro.manager.core.database.dao.PurchaseItemDao
import com.medipro.manager.core.database.dao.SaleReturnDao
import com.medipro.manager.core.database.dao.SaleReturnItemDao
import com.medipro.manager.core.database.dao.SaleDao
import com.medipro.manager.core.database.dao.SaleItemDao
import com.medipro.manager.core.database.dao.SettingsDao
import com.medipro.manager.core.database.dao.StockAdjustmentDao
import com.medipro.manager.core.database.dao.StockDao
import com.medipro.manager.core.database.dao.SupplierDao
import com.medipro.manager.core.database.entity.AuditLogEntity
import com.medipro.manager.core.database.entity.BackupHistoryEntity
import com.medipro.manager.core.database.entity.BatchEntity
import com.medipro.manager.core.database.entity.CustomerEntity
import com.medipro.manager.core.database.entity.DayClosingEntity
import com.medipro.manager.core.database.entity.ExpenseEntity
import com.medipro.manager.core.database.entity.IncomeEntity
import com.medipro.manager.core.database.entity.LedgerEntity
import com.medipro.manager.core.database.entity.LicenseEntity
import com.medipro.manager.core.database.entity.OcrMedicineAliasEntity
import com.medipro.manager.core.database.dao.OcrScanSessionDao
import com.medipro.manager.core.database.entity.OcrScanSessionEntity
import com.medipro.manager.core.database.entity.MedicineFtsEntity
import com.medipro.manager.core.database.entity.PaymentEntity
import com.medipro.manager.core.database.entity.PendingOperationEntity
import com.medipro.manager.core.database.entity.PurchaseEntity
import com.medipro.manager.core.database.entity.PurchaseReturnEntity
import com.medipro.manager.core.database.entity.PurchaseReturnItemEntity
import com.medipro.manager.core.database.entity.PurchaseItemEntity
import com.medipro.manager.core.database.entity.ReturnEntity
import com.medipro.manager.core.database.entity.SaleReturnEntity
import com.medipro.manager.core.database.entity.SaleReturnItemEntity
import com.medipro.manager.core.database.entity.SaleEntity
import com.medipro.manager.core.database.entity.SaleItemEntity
import com.medipro.manager.core.database.entity.SettingsEntity
import com.medipro.manager.core.database.entity.StockAdjustmentEntity
import com.medipro.manager.core.database.entity.StockEntity
import com.medipro.manager.core.database.entity.SupplierEntity

@Database(
    entities = [
        MedicineEntity::class,
        MedicineFtsEntity::class,
        SupplierEntity::class,
        CustomerEntity::class,
        BatchEntity::class,
        StockEntity::class,
        PurchaseEntity::class,
        PurchaseItemEntity::class,
        PurchaseReturnEntity::class,
        PurchaseReturnItemEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        SaleReturnEntity::class,
        SaleReturnItemEntity::class,
        ReturnEntity::class,
        ExpenseEntity::class,
        IncomeEntity::class,
        LedgerEntity::class,
        PaymentEntity::class,
        BackupHistoryEntity::class,
        LicenseEntity::class,
        SettingsEntity::class,
        StockAdjustmentEntity::class,
        AuditLogEntity::class,
        PendingOperationEntity::class,
        DayClosingEntity::class,
        OcrMedicineAliasEntity::class,
        OcrScanSessionEntity::class,
    ],
    version = 15,
    exportSchema = true
)
abstract class MediProDatabase : RoomDatabase() {
    abstract fun medicineDao(): MedicineDao
    abstract fun supplierDao(): SupplierDao
    abstract fun customerDao(): CustomerDao
    abstract fun batchDao(): BatchDao
    abstract fun stockDao(): StockDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun purchaseItemDao(): PurchaseItemDao
    abstract fun purchaseReturnDao(): PurchaseReturnDao
    abstract fun purchaseReturnItemDao(): PurchaseReturnItemDao
    abstract fun saleDao(): SaleDao
    abstract fun saleItemDao(): SaleItemDao
    abstract fun saleReturnDao(): SaleReturnDao
    abstract fun saleReturnItemDao(): SaleReturnItemDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun incomeDao(): IncomeDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun paymentDao(): PaymentDao
    abstract fun backupHistoryDao(): BackupHistoryDao
    abstract fun licenseDao(): LicenseDao
    abstract fun settingsDao(): SettingsDao
    abstract fun stockAdjustmentDao(): StockAdjustmentDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun reportDao(): ReportDao
    abstract fun pendingOperationDao(): PendingOperationDao
    abstract fun dayClosingDao(): DayClosingDao
    abstract fun ocrMedicineAliasDao(): OcrMedicineAliasDao
    abstract fun ocrScanSessionDao(): OcrScanSessionDao

    companion object {
        const val DATABASE_NAME = "medipro.db"
    }
}
