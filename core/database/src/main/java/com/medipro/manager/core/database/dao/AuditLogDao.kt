package com.medipro.manager.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.medipro.manager.core.database.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: AuditLogEntity)

    @Query("SELECT * FROM audit_logs ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE entityUuid = :entityUuid ORDER BY createdAt DESC")
    fun observeByEntity(entityUuid: String): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): AuditLogEntity?

    @Query(
        """
        UPDATE audit_logs SET syncStatus = :status, syncVersion = :version,
        updatedAt = :updatedAt WHERE uuid = :uuid
        """
    )
    suspend fun updateSyncState(uuid: String, status: String, version: Long, updatedAt: Long)
}
