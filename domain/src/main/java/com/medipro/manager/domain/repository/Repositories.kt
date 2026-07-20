package com.medipro.manager.domain.repository

import com.medipro.manager.domain.model.DashboardSnapshot
import com.medipro.manager.domain.model.DashboardStats
import com.medipro.manager.domain.model.Medicine
import com.medipro.manager.domain.model.Sale
import kotlinx.coroutines.flow.Flow

interface MedicineRepository {
    fun observeMedicines(): Flow<List<Medicine>>
    fun searchMedicines(query: String): Flow<List<Medicine>>
    suspend fun getMedicineById(id: Long): Medicine?
    suspend fun getMedicineByBarcode(barcode: String): Medicine?
    suspend fun addMedicine(medicine: Medicine): Long
    suspend fun updateMedicine(medicine: Medicine)
    suspend fun deleteMedicine(id: Long)
}

interface DashboardRepository {
    fun observeStats(): Flow<DashboardStats>
    fun observeRecentSales(): Flow<List<Sale>>
    fun observeDashboard(): Flow<DashboardSnapshot>
}

interface LicenseRepository {
    fun observeLicense(): Flow<com.medipro.manager.domain.model.License?>
    suspend fun getLicense(): com.medipro.manager.domain.model.License?
    suspend fun saveLicense(license: com.medipro.manager.domain.model.License)
    suspend fun isLicenseValid(deviceId: String): Boolean
    suspend fun isLicenseExpired(): Boolean
    suspend fun activateFromServer(
        firebaseUid: String,
        idToken: String,
        mobileNumber: String,
        deviceId: String,
        pharmacyName: String,
        ownerName: String,
    ): Result<com.medipro.manager.domain.model.License>
    suspend fun syncWithServer(deviceId: String): Result<com.medipro.manager.domain.model.License>
    suspend fun shouldSyncWithServer(): Boolean
}

interface SettingsRepository {
    fun observeSettings(): Flow<com.medipro.manager.domain.model.PharmacySettings>
    suspend fun getSettings(): com.medipro.manager.domain.model.PharmacySettings
    suspend fun updateSettings(settings: com.medipro.manager.domain.model.PharmacySettings)
}

interface SupplierRepository {
    fun observeSuppliers(): Flow<List<com.medipro.manager.domain.model.Supplier>>
    suspend fun addSupplier(supplier: com.medipro.manager.domain.model.Supplier): Long
    suspend fun updateSupplier(supplier: com.medipro.manager.domain.model.Supplier)
}

interface CustomerRepository {
    fun observeCustomers(): Flow<List<com.medipro.manager.domain.model.Customer>>
    suspend fun addCustomer(customer: com.medipro.manager.domain.model.Customer): Long
    suspend fun updateCustomer(customer: com.medipro.manager.domain.model.Customer)
}

interface GlobalSearchRepository {
    suspend fun search(query: String): com.medipro.manager.domain.model.GlobalSearchResponse
}

interface SaleRepository {
    fun observeSales(): Flow<List<Sale>>
    suspend fun createSale(sale: Sale): Long
    suspend fun getSaleById(id: Long): Sale?
    suspend fun getSaleByInvoiceNumber(invoiceNumber: String): Sale?
    suspend fun getSaleByUuid(uuid: String): Sale?
    suspend fun resolveSale(invoiceRef: String): Sale?
    suspend fun cancelSale(saleId: Long): Result<Unit>
    suspend fun searchMedicinesForSale(query: String): List<Medicine>
    suspend fun getAvailableBatches(medicineId: Long): List<com.medipro.manager.domain.model.StockBatch>
    suspend fun generateInvoiceNumber(): String
}

interface SaleReturnRepository {
    suspend fun getReturnContext(saleId: Long): com.medipro.manager.domain.model.SaleReturnContext?
    suspend fun createSaleReturn(
        saleId: Long,
        reason: String,
        lines: List<com.medipro.manager.domain.model.SaleReturnLine>,
        notes: String? = null,
    ): Long
    suspend fun generateReturnNumber(): String
    fun observeSaleReturns(): Flow<List<com.medipro.manager.domain.model.SaleReturn>>
}

interface PurchaseRepository {
    fun observePurchases(): Flow<List<com.medipro.manager.domain.model.Purchase>>
    suspend fun createPurchase(purchase: com.medipro.manager.domain.model.Purchase): Long
    suspend fun getPurchaseById(id: Long): com.medipro.manager.domain.model.Purchase?
    suspend fun getPurchaseByInvoiceNumber(invoiceNumber: String): com.medipro.manager.domain.model.Purchase?
    suspend fun findBySupplierBillNumber(supplierBillNumber: String, supplierId: Long? = null): com.medipro.manager.domain.model.Purchase?
    suspend fun searchMedicinesForPurchase(query: String): List<Medicine>
    suspend fun generatePurchaseInvoiceNumber(): String
}

interface PurchaseReturnRepository {
    suspend fun getReturnContext(purchaseId: Long): com.medipro.manager.domain.model.PurchaseReturnContext?
    suspend fun createPurchaseReturn(
        purchaseId: Long,
        reason: String,
        lines: List<com.medipro.manager.domain.model.PurchaseReturnLine>,
        notes: String? = null,
    ): Long
    suspend fun generateReturnNumber(): String
    fun observePurchaseReturns(): Flow<List<com.medipro.manager.domain.model.PurchaseReturn>>
}

interface InventoryRepository {
    fun observeSummary(): Flow<com.medipro.manager.domain.model.InventorySummary>
    suspend fun searchMedicinesWithStock(query: String): List<com.medipro.manager.domain.model.InventoryMedicineStock>
    suspend fun getMedicineStock(medicineId: Long): com.medipro.manager.domain.model.InventoryMedicineStock?
    suspend fun getAdjustmentContext(batchId: Long): com.medipro.manager.domain.model.StockAdjustmentContext?
    suspend fun createAdjustment(
        batchId: Long,
        type: com.medipro.manager.domain.model.StockAdjustmentType,
        quantity: Int,
        reason: String,
        remarks: String? = null,
    ): Long
    fun observeAdjustments(): Flow<List<com.medipro.manager.domain.model.StockAdjustment>>
    fun observeDamageAdjustments(): Flow<List<com.medipro.manager.domain.model.StockAdjustment>>
    fun observeExpiryReport(): Flow<List<com.medipro.manager.domain.model.ExpiryReportRow>>
}

interface BackupRepository {
    suspend fun createBackup(password: CharArray, backupType: String = "MANUAL"): Result<com.medipro.manager.domain.model.BackupRecord>
    suspend fun restoreBackup(filePath: String, password: CharArray): Result<Unit>
    fun observeBackupHistory(): Flow<List<com.medipro.manager.domain.model.BackupRecord>>
    suspend fun getLatestBackup(): com.medipro.manager.domain.model.BackupRecord?
}
