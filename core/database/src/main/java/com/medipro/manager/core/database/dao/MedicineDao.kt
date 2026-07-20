package com.medipro.manager.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.medipro.manager.core.database.entity.MedicineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    @Query("SELECT * FROM medicines WHERE isActive = 1 AND deletedAt IS NULL ORDER BY brandName ASC")
    fun observeAll(): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines WHERE id = :id")
    suspend fun getById(id: Long): MedicineEntity?

    @Query("SELECT * FROM medicines WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): MedicineEntity?

    @Query("SELECT * FROM medicines WHERE barcode = :barcode AND deletedAt IS NULL LIMIT 1")
    suspend fun getByBarcode(barcode: String): MedicineEntity?

    @Query("SELECT * FROM medicines WHERE catalogUuid = :catalogUuid AND deletedAt IS NULL LIMIT 1")
    suspend fun getByCatalogId(catalogUuid: String): MedicineEntity?

    @Query("SELECT * FROM medicines WHERE brandName = :brandName AND deletedAt IS NULL LIMIT 1")
    suspend fun getByBrandName(brandName: String): MedicineEntity?

    @Query(
        """
        SELECT * FROM medicines
        WHERE isActive = 1 AND deletedAt IS NULL
        AND brandName = :brand COLLATE NOCASE
        LIMIT 20
        """
    )
    suspend fun searchExactBrand(brand: String): List<MedicineEntity>

    @Query(
        """
        SELECT * FROM medicines
        WHERE isActive = 1 AND deletedAt IS NULL
        AND genericName = :generic COLLATE NOCASE
        LIMIT 20
        """
    )
    suspend fun searchExactGeneric(generic: String): List<MedicineEntity>

    @Query(
        """
        SELECT * FROM medicines
        WHERE isActive = 1 AND deletedAt IS NULL AND (
            brandName LIKE :prefix || '%' COLLATE NOCASE OR
            genericName LIKE :prefix || '%' COLLATE NOCASE
        )
        ORDER BY brandName ASC
        LIMIT :limit
        """
    )
    suspend fun searchStartsWith(prefix: String, limit: Int = 50): List<MedicineEntity>

    @Query(
        """
        SELECT * FROM medicines
        WHERE isActive = 1 AND deletedAt IS NULL AND (
            brandName LIKE '%' || :query || '%' COLLATE NOCASE OR
            genericName LIKE '%' || :query || '%' COLLATE NOCASE OR
            composition LIKE '%' || :query || '%' COLLATE NOCASE OR
            strength LIKE '%' || :query || '%' COLLATE NOCASE OR
            manufacturer LIKE '%' || :query || '%' COLLATE NOCASE OR
            dosageForm LIKE '%' || :query || '%' COLLATE NOCASE
        )
        ORDER BY brandName ASC
        LIMIT :limit
        """
    )
    suspend fun searchContains(query: String, limit: Int = 50): List<MedicineEntity>

    @Query(
        """
        SELECT * FROM medicines
        WHERE isActive = 1 AND deletedAt IS NULL
        AND composition LIKE '%' || :token || '%' COLLATE NOCASE
        ORDER BY brandName ASC
        LIMIT :limit
        """
    )
    suspend fun searchByCompositionToken(token: String, limit: Int = 50): List<MedicineEntity>

    @Query(
        """
        SELECT m.* FROM medicines m
        INNER JOIN medicines_fts ON m.rowid = medicines_fts.rowid
        WHERE medicines_fts MATCH :query
        AND m.isActive = 1 AND m.deletedAt IS NULL
        ORDER BY m.brandName ASC
        LIMIT :limit
        """
    )
    suspend fun searchFtsOnce(query: String, limit: Int = 50): List<MedicineEntity>

    @Query(
        """
        SELECT m.* FROM medicines m
        INNER JOIN medicines_fts ON m.rowid = medicines_fts.rowid
        WHERE medicines_fts MATCH :query
        AND m.isActive = 1 AND m.deletedAt IS NULL
        ORDER BY m.brandName ASC
        LIMIT 50
        """
    )
    fun searchFts(query: String): Flow<List<MedicineEntity>>

    @Query(
        """
        SELECT * FROM medicines
        WHERE isActive = 1 AND deletedAt IS NULL AND (
            brandName LIKE '%' || :query || '%' OR
            genericName LIKE '%' || :query || '%' OR
            composition LIKE '%' || :query || '%' OR
            strength LIKE '%' || :query || '%' OR
            dosageForm LIKE '%' || :query || '%' OR
            manufacturer LIKE '%' || :query || '%' OR
            barcode LIKE '%' || :query || '%'
        )
        ORDER BY brandName ASC
        LIMIT 50
        """
    )
    fun search(query: String): Flow<List<MedicineEntity>>

    @Query(
        """
        SELECT * FROM medicines
        WHERE isActive = 1 AND deletedAt IS NULL AND (
            brandName LIKE :prefix || '%' OR
            genericName LIKE :prefix || '%'
        )
        ORDER BY brandName ASC
        LIMIT :limit
        """
    )
    suspend fun searchByPrefix(prefix: String, limit: Int = 120): List<MedicineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medicine: MedicineEntity): Long

    @Update
    suspend fun update(medicine: MedicineEntity)

    @Query(
        """
        UPDATE medicines SET isActive = 0, deletedAt = :deletedAt,
        updatedAt = :deletedAt, syncStatus = 'DELETED', syncVersion = syncVersion + 1
        WHERE id = :id
        """
    )
    suspend fun softDelete(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query(
        """
        UPDATE medicines SET syncStatus = :status, syncVersion = :version,
        updatedAt = :updatedAt WHERE id = :id
        """
    )
    suspend fun updateSyncState(id: Long, status: String, version: Long, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM medicines WHERE isActive = 1 AND deletedAt IS NULL")
    suspend fun count(): Int
}
