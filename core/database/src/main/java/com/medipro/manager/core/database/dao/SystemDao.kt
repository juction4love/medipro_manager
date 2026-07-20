package com.medipro.manager.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.medipro.manager.core.database.entity.BackupHistoryEntity
import com.medipro.manager.core.database.entity.ExpenseEntity
import com.medipro.manager.core.database.entity.IncomeEntity
import com.medipro.manager.core.database.entity.LedgerEntity
import com.medipro.manager.core.database.entity.LicenseEntity
import com.medipro.manager.core.database.entity.PaymentEntity
import com.medipro.manager.core.database.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY expenseDate DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query(
        """
        SELECT * FROM expenses
        WHERE expenseDate >= :start AND expenseDate <= :end
        ORDER BY expenseDate DESC
        """
    )
    fun observeByDateRange(start: Long, end: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE expenseDate >= :start AND expenseDate <= :end")
    suspend fun getTotal(start: Long, end: Long): Double

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM expenses
        WHERE expenseDate >= :start AND expenseDate <= :end AND paymentMethod = 'CASH'
        """
    )
    suspend fun getCashTotal(start: Long, end: Long): Double

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity): Long
}

@Dao
interface IncomeDao {
    @Query("SELECT * FROM income ORDER BY incomeDate DESC")
    fun observeAll(): Flow<List<IncomeEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM income WHERE incomeDate >= :start AND incomeDate <= :end")
    suspend fun getTotal(start: Long, end: Long): Double

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(income: IncomeEntity): Long
}

@Dao
interface LedgerDao {
    @Query("SELECT * FROM ledger ORDER BY entryDate DESC")
    fun observeAll(): Flow<List<LedgerEntity>>

    @Query(
        """
        SELECT * FROM ledger
        WHERE entryDate >= :start AND entryDate <= :end
        ORDER BY entryDate ASC
        """
    )
    fun observeByDateRange(start: Long, end: Long): Flow<List<LedgerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: LedgerEntity): Long

    @Query("SELECT COALESCE(SUM(debit - credit), 0) FROM ledger WHERE accountType = :accountType")
    suspend fun getAccountBalance(accountType: String): Double

    @Query("SELECT * FROM ledger WHERE referenceType = :referenceType AND referenceId = :referenceId")
    suspend fun getByReference(referenceType: String, referenceId: Long): List<LedgerEntity>

    @Query("SELECT * FROM ledger WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): LedgerEntity?

    @Query(
        """
        UPDATE ledger SET syncStatus = :status, syncVersion = :version,
        updatedAt = :updatedAt WHERE id = :id
        """
    )
    suspend fun updateSyncState(id: Long, status: String, version: Long, updatedAt: Long)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): PaymentEntity?

    @Query("SELECT * FROM payments WHERE type = :type AND referenceId = :referenceId ORDER BY paymentDate DESC")
    suspend fun getByReference(type: String, referenceId: Long): List<PaymentEntity>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM payments
        WHERE type = :type AND paymentDate >= :start AND paymentDate <= :end
        """
    )
    suspend fun getTotalByType(type: String, start: Long, end: Long): Double

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM payments
        WHERE type = :type AND paymentMethod = 'CASH'
        AND paymentDate >= :start AND paymentDate <= :end
        """
    )
    suspend fun getCashTotalByType(type: String, start: Long, end: Long): Double

    @Query("SELECT * FROM payments ORDER BY paymentDate DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: PaymentEntity): Long

    @Query("SELECT * FROM payments WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): PaymentEntity?

    @Query(
        """
        UPDATE payments SET syncStatus = :status, syncVersion = :version,
        updatedAt = :updatedAt WHERE id = :id
        """
    )
    suspend fun updateSyncState(id: Long, status: String, version: Long, updatedAt: Long)
}

@Dao
interface BackupHistoryDao {
    @Query("SELECT * FROM backup_history ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BackupHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(backup: BackupHistoryEntity): Long

    @Query("DELETE FROM backup_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM backup_history ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatest(): BackupHistoryEntity?
}

@Dao
interface LicenseDao {
    @Query("SELECT * FROM license WHERE id = 1 LIMIT 1")
    fun observe(): Flow<LicenseEntity?>

    @Query("SELECT * FROM license WHERE id = 1 LIMIT 1")
    suspend fun get(): LicenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(license: LicenseEntity)

    @Update
    suspend fun update(license: LicenseEntity)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    fun observe(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    suspend fun get(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: SettingsEntity)

    @Update
    suspend fun update(settings: SettingsEntity)

    @Query("SELECT * FROM settings WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): SettingsEntity?

    @Query(
        """
        UPDATE settings SET syncStatus = :status, syncVersion = :version,
        updatedAt = :updatedAt WHERE id = :id
        """
    )
    suspend fun updateSyncState(id: Int, status: String, version: Long, updatedAt: Long)
}
