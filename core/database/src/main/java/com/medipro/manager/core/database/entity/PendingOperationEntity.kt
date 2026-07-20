package com.medipro.manager.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "pending_operations",
    indices = [Index(value = ["status"]), Index(value = ["createdAt"])]
)
data class PendingOperationEntity(
    @PrimaryKey
    val uuid: String = UUID.randomUUID().toString(),
    val operationType: String,
    val entityType: String,
    val entityUuid: String,
    val payloadJson: String,
    val status: String = OperationStatus.PENDING.name,
    val retryCount: Int = 0,
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class OperationStatus {
    PENDING, IN_PROGRESS, COMPLETED, FAILED
}
