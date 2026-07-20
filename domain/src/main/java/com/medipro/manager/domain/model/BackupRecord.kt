package com.medipro.manager.domain.model

data class BackupRecord(
    val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val isEncrypted: Boolean = true,
    val backupType: String = "MANUAL",
    val createdAt: Long = System.currentTimeMillis()
)
