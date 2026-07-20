package com.medipro.manager.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "returns",
    indices = [Index(value = ["referenceId"]), Index(value = ["returnDate"]), Index(value = ["uuid"], unique = true)]
)
data class ReturnEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val pharmacyUuid: String = "",
    val branchUuid: String? = null,
    val type: String,
    val referenceId: Long,
    val referenceUuid: String? = null,
    val medicineId: Long,
    val medicineUuid: String? = null,
    val batchId: Long? = null,
    val batchUuid: String? = null,
    val quantity: Int,
    val amount: Double,
    val reason: String? = null,
    val returnDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = SyncStatus.PENDING,
    val syncVersion: Long = 0,
    val deviceId: String? = null,
    val createdBy: String? = null,
)

@Entity(
    tableName = "expenses",
    indices = [Index(value = ["expenseDate"]), Index(value = ["category"])]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String,
    val description: String,
    val amount: Double,
    val paymentMethod: String = "CASH",
    val expenseDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "income",
    indices = [Index(value = ["incomeDate"]), Index(value = ["category"])]
)
data class IncomeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String,
    val description: String,
    val amount: Double,
    val paymentMethod: String = "CASH",
    val incomeDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "ledger",
    indices = [
        Index(value = ["entryDate"]),
        Index(value = ["accountType"]),
        Index(value = ["uuid"], unique = true),
    ]
)
data class LedgerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val pharmacyUuid: String = "",
    val branchUuid: String? = null,
    val accountType: String,
    val accountId: Long? = null,
    val accountUuid: String? = null,
    val description: String,
    val debit: Double = 0.0,
    val credit: Double = 0.0,
    val balance: Double = 0.0,
    val referenceType: String? = null,
    val referenceId: Long? = null,
    val referenceUuid: String? = null,
    val entryDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = SyncStatus.PENDING,
    val syncVersion: Long = 0,
    val deviceId: String? = null,
    val createdBy: String? = null,
)

@Entity(
    tableName = "payments",
    indices = [
        Index(value = ["referenceId"]),
        Index(value = ["paymentDate"]),
        Index(value = ["uuid"], unique = true),
    ]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val pharmacyUuid: String = "",
    val branchUuid: String? = null,
    val type: String,
    val referenceId: Long,
    val referenceUuid: String? = null,
    val amount: Double,
    val paymentMethod: String = "CASH",
    val paymentDate: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = SyncStatus.PENDING,
    val syncVersion: Long = 0,
    val deviceId: String? = null,
    val createdBy: String? = null,
)
