package com.medipro.manager.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.medipro.manager.core.database.entity.OcrMedicineAliasEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OcrMedicineAliasDao {
    @Query(
        """
        SELECT * FROM ocr_medicine_aliases
        WHERE normalizedText = :normalized AND isEnabled = 1
        LIMIT 1
        """,
    )
    suspend fun findEnabledByNormalized(normalized: String): OcrMedicineAliasEntity?

    @Query("SELECT * FROM ocr_medicine_aliases WHERE normalizedText = :normalized LIMIT 1")
    suspend fun findByNormalized(normalized: String): OcrMedicineAliasEntity?

    @Query("SELECT * FROM ocr_medicine_aliases WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): OcrMedicineAliasEntity?

    @Query("SELECT * FROM ocr_medicine_aliases ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<OcrMedicineAliasEntity>>

    @Query("SELECT COUNT(*) FROM ocr_medicine_aliases WHERE isEnabled = 1")
    suspend fun countEnabled(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: OcrMedicineAliasEntity): Long

    @Query("DELETE FROM ocr_medicine_aliases WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM ocr_medicine_aliases")
    suspend fun deleteAll()

    @Query(
        """
        UPDATE ocr_medicine_aliases
        SET isEnabled = :enabled, updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun setEnabled(id: Long, enabled: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query(
        """
        UPDATE ocr_medicine_aliases
        SET hitCount = hitCount + 1, updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun incrementHit(id: Long, updatedAt: Long = System.currentTimeMillis())
}
