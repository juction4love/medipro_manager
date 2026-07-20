package com.medipro.manager.data.repository

import androidx.room.withTransaction
import com.medipro.manager.core.database.MediProDatabase
import com.medipro.manager.core.database.dao.CustomerDao
import com.medipro.manager.core.database.dao.IncomeDao
import com.medipro.manager.core.database.dao.LedgerDao
import com.medipro.manager.core.database.dao.LicenseDao
import com.medipro.manager.core.database.dao.PaymentDao
import com.medipro.manager.core.database.dao.SupplierDao
import com.medipro.manager.core.database.entity.IncomeEntity
import com.medipro.manager.core.database.entity.LedgerEntity
import com.medipro.manager.core.database.entity.PaymentEntity
import com.medipro.manager.core.database.entity.SyncStatus
import com.medipro.manager.data.mapper.toDomain
import com.medipro.manager.data.sync.SyncEnqueueHelper
import com.medipro.manager.data.sync.pharmacyUuid
import com.medipro.manager.domain.repository.AccountingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountingRepositoryImpl @Inject constructor(
    private val database: MediProDatabase,
    private val customerDao: CustomerDao,
    private val supplierDao: SupplierDao,
    private val paymentDao: PaymentDao,
    private val ledgerDao: LedgerDao,
    private val incomeDao: IncomeDao,
    private val licenseDao: LicenseDao,
    private val syncEnqueueHelper: SyncEnqueueHelper,
) : AccountingRepository {

    override fun observeCustomersWithDue() =
        customerDao.observeAll().map { list ->
            list.filter { it.outstandingBalance > 0 }.map { it.toDomain() }
        }

    override fun observeSuppliersWithDue() =
        supplierDao.observeAll().map { list ->
            list.filter { it.outstandingBalance > 0 }.map { it.toDomain() }
        }

    override suspend fun recordCustomerReceipt(
        customerId: Long,
        amount: Double,
        paymentMethod: String,
        notes: String?,
    ): String {
        require(amount > 0) { "Amount must be greater than zero" }
        val customer = customerDao.getById(customerId) ?: throw IllegalStateException("Customer not found")
        if (amount > customer.outstandingBalance + 0.01) {
            throw IllegalStateException("Amount exceeds outstanding balance")
        }

        val receiptRef = receiptReference("CR")
        val pharmacyUuid = licenseDao.pharmacyUuid()
        val deviceId = licenseDao.get()?.deviceId
        val now = System.currentTimeMillis()

        var paymentId = 0L
        database.withTransaction {
            paymentId = paymentDao.insert(
                PaymentEntity(
                    type = "CUSTOMER_RECEIPT",
                    referenceId = customerId,
                    referenceUuid = customer.uuid,
                    pharmacyUuid = pharmacyUuid,
                    amount = amount,
                    paymentMethod = paymentMethod,
                    notes = notes ?: "Receipt $receiptRef",
                    paymentDate = now,
                    deviceId = deviceId,
                ),
            )

            customerDao.update(
                customer.copy(
                    outstandingBalance = (customer.outstandingBalance - amount).coerceAtLeast(0.0),
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING,
                    syncVersion = customer.syncVersion + 1,
                ),
            )

            ledgerDao.insert(
                LedgerEntity(
                    accountType = "CUSTOMER",
                    accountId = customerId,
                    accountUuid = customer.uuid,
                    description = "Payment received — ${customer.name}",
                    credit = amount,
                    referenceType = "CUSTOMER_RECEIPT",
                    referenceId = paymentId,
                    pharmacyUuid = pharmacyUuid,
                    entryDate = now,
                    deviceId = deviceId,
                ),
            )
            ledgerDao.insert(
                LedgerEntity(
                    accountType = "CASH",
                    description = "Customer receipt $receiptRef",
                    debit = amount,
                    referenceType = "CUSTOMER_RECEIPT",
                    referenceId = paymentId,
                    pharmacyUuid = pharmacyUuid,
                    entryDate = now,
                    deviceId = deviceId,
                ),
            )

            incomeDao.insert(
                IncomeEntity(
                    category = "CUSTOMER_COLLECTION",
                    description = "${customer.name} — $receiptRef",
                    amount = amount,
                    paymentMethod = paymentMethod,
                    incomeDate = now,
                ),
            )
        }

        customerDao.getById(customerId)?.let { syncEnqueueHelper.enqueueCustomer(it) }
        paymentDao.getById(paymentId)?.let { payment ->
            syncEnqueueHelper.enqueuePayment(payment, customer.uuid)
        }
        return receiptRef
    }

    override suspend fun recordSupplierPayment(
        supplierId: Long,
        amount: Double,
        paymentMethod: String,
        notes: String?,
    ): String {
        require(amount > 0) { "Amount must be greater than zero" }
        val supplier = supplierDao.getById(supplierId) ?: throw IllegalStateException("Supplier not found")
        if (amount > supplier.outstandingBalance + 0.01) {
            throw IllegalStateException("Amount exceeds outstanding balance")
        }

        val receiptRef = receiptReference("SP")
        val pharmacyUuid = licenseDao.pharmacyUuid()
        val deviceId = licenseDao.get()?.deviceId
        val now = System.currentTimeMillis()

        var paymentId = 0L
        database.withTransaction {
            paymentId = paymentDao.insert(
                PaymentEntity(
                    type = "SUPPLIER_PAYMENT",
                    referenceId = supplierId,
                    referenceUuid = supplier.uuid,
                    pharmacyUuid = pharmacyUuid,
                    amount = amount,
                    paymentMethod = paymentMethod,
                    notes = notes ?: "Payment $receiptRef",
                    paymentDate = now,
                    deviceId = deviceId,
                ),
            )

            supplierDao.update(
                supplier.copy(
                    outstandingBalance = (supplier.outstandingBalance - amount).coerceAtLeast(0.0),
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING,
                    syncVersion = supplier.syncVersion + 1,
                ),
            )

            ledgerDao.insert(
                LedgerEntity(
                    accountType = "SUPPLIER",
                    accountId = supplierId,
                    accountUuid = supplier.uuid,
                    description = "Payment to ${supplier.name}",
                    debit = amount,
                    referenceType = "SUPPLIER_PAYMENT",
                    referenceId = paymentId,
                    pharmacyUuid = pharmacyUuid,
                    entryDate = now,
                    deviceId = deviceId,
                ),
            )
            ledgerDao.insert(
                LedgerEntity(
                    accountType = "CASH",
                    description = "Supplier payment $receiptRef",
                    credit = amount,
                    referenceType = "SUPPLIER_PAYMENT",
                    referenceId = paymentId,
                    pharmacyUuid = pharmacyUuid,
                    entryDate = now,
                    deviceId = deviceId,
                ),
            )
        }

        supplierDao.getById(supplierId)?.let { syncEnqueueHelper.enqueueSupplier(it) }
        paymentDao.getById(paymentId)?.let { payment ->
            syncEnqueueHelper.enqueuePayment(payment, supplier.uuid)
        }
        return receiptRef
    }

    private fun receiptReference(prefix: String): String {
        val date = SimpleDateFormat("yyMMdd", Locale.getDefault()).format(Date())
        val suffix = (System.currentTimeMillis() % 10000).toString().padStart(4, '0')
        return "$prefix-$date-$suffix"
    }
}
