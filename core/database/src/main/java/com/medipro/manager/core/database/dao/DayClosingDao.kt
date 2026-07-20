package com.medipro.manager.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.medipro.manager.core.database.entity.DayClosingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DayClosingDao {
    @Query("SELECT * FROM day_closings WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DayClosingEntity?

    @Query("SELECT * FROM day_closings WHERE closingDate = :dayStart LIMIT 1")
    suspend fun getByDate(dayStart: Long): DayClosingEntity?

    @Query("SELECT * FROM day_closings WHERE closingDate < :dayStart ORDER BY closingDate DESC LIMIT 1")
    suspend fun getLatestBefore(dayStart: Long): DayClosingEntity?

    @Query("SELECT * FROM day_closings ORDER BY closingDate DESC LIMIT 1")
    suspend fun getLatest(): DayClosingEntity?

    @Query("SELECT * FROM day_closings ORDER BY closingDate DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<DayClosingEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(closing: DayClosingEntity): Long

    @Update
    suspend fun update(closing: DayClosingEntity)
}
