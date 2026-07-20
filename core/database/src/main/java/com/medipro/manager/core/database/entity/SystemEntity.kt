package com.medipro.manager.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "backup_history")
data class BackupHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val isEncrypted: Boolean = true,
    val backupType: String = "MANUAL",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "license")
data class LicenseEntity(
    @PrimaryKey
    val id: Int = 1,
    val licenseId: String,
    val licenseKey: String,
    val mobileNumber: String,
    val pharmacyName: String,
    val ownerName: String,
    val deviceId: String,
    val plan: String = "FREE",
    val status: String = "ACTIVE",
    val activatedAt: Long,
    val expiresAt: Long,
    val lastVerifiedAt: Long? = null,
    val isActive: Boolean = true,
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val uuid: String = SETTINGS_UUID,
    val pharmacyUuid: String = "",
    val pharmacyName: String = "",
    val pharmacyAddress: String = "",
    val pharmacyPhone: String = "",
    val pharmacyEmail: String = "",
    val panNumber: String = "",
    val vatNumber: String = "",
    val currency: String = "NPR",
    val language: String = "ne",
    val theme: String = "SYSTEM",
    val printerName: String? = null,
    val autoBackupEnabled: Boolean = false,
    val autoBackupIntervalDays: Int = 7,
    val appLockEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val lowStockThreshold: Int = 10,
    val expiryAlertDays: Int = 90,
    val prescriptionModuleEnabled: Boolean = true,
    val requirePrescriptionDetails: Boolean = true,
    val ocrFeedbackOptIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = SyncStatus.PENDING,
    val syncVersion: Long = 0,
    val deviceId: String? = null,
) {
    companion object {
        const val SETTINGS_UUID = "pharmacy-settings"
    }
}
