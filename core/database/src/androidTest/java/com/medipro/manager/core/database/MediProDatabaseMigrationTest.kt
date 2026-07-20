package com.medipro.manager.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.medipro.manager.core.database.migration.DatabaseMigrations
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediProDatabaseMigrationTest {

    private val testDb = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MediProDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate1To2() {
        helper.createDatabase(testDb, 1).close()
        helper.runMigrationsAndValidate(testDb, 2, true, DatabaseMigrations.MIGRATION_1_2)
    }

    @Test
    fun migrate2To3() {
        helper.createDatabase(testDb, 2).close()
        helper.runMigrationsAndValidate(testDb, 3, true, DatabaseMigrations.MIGRATION_2_3)
    }

    @Test
    fun migrate3To4() {
        helper.createDatabase(testDb, 3).close()
        helper.runMigrationsAndValidate(testDb, 4, true, DatabaseMigrations.MIGRATION_3_4)
    }

    @Test
    fun migrate4To5() {
        helper.createDatabase(testDb, 4).close()
        helper.runMigrationsAndValidate(testDb, 5, true, DatabaseMigrations.MIGRATION_4_5)
    }

    @Test
    fun migrate5To6() {
        helper.createDatabase(testDb, 5).close()
        helper.runMigrationsAndValidate(testDb, 6, true, DatabaseMigrations.MIGRATION_5_6)
    }

    @Test
    fun migrate6To7() {
        helper.createDatabase(testDb, 6).close()
        helper.runMigrationsAndValidate(testDb, 7, true, DatabaseMigrations.MIGRATION_6_7)
    }

    @Test
    fun migrate7To8() {
        helper.createDatabase(testDb, 7).close()
        helper.runMigrationsAndValidate(testDb, 8, true, DatabaseMigrations.MIGRATION_7_8)
    }

    @Test
    fun migrate8To9() {
        helper.createDatabase(testDb, 8).close()
        helper.runMigrationsAndValidate(testDb, 9, true, DatabaseMigrations.MIGRATION_8_9)
    }

    @Test
    fun migrate9To10() {
        helper.createDatabase(testDb, 9).close()
        helper.runMigrationsAndValidate(testDb, 10, true, DatabaseMigrations.MIGRATION_9_10)
    }

    @Test
    fun migrate10To11() {
        helper.createDatabase(testDb, 10).close()
        helper.runMigrationsAndValidate(testDb, 11, true, DatabaseMigrations.MIGRATION_10_11)
    }

    @Test
    fun migrate11To12() {
        helper.createDatabase(testDb, 11).close()
        helper.runMigrationsAndValidate(testDb, 12, true, DatabaseMigrations.MIGRATION_11_12)
    }

    @Test
    fun migrate1To10_preservesDataThroughMedicineReshape() {
        val db = helper.createDatabase(testDb, 1)
        db.execSQL(
            """
            INSERT INTO medicines (
                name, genericName, brand, company, category, unit,
                purchasePrice, sellingPrice, mrp, vatPercent, reorderLevel, isActive, createdAt, updatedAt
            ) VALUES (
                'Paracetamol 500', 'Paracetamol', 'BrandX', 'CompanyY', 'General', 'pcs',
                10.5, 15.0, 20.0, 13.0, 10, 1, 1000, 2000
            )
            """.trimIndent()
        )
        db.close()

        val migrated = helper.runMigrationsAndValidate(testDb, 10, true, *DatabaseMigrations.ALL)
        migrated.query("SELECT brandName, purchasePricePaisa FROM medicines WHERE id = 1").use { cursor ->
            assert(cursor.moveToFirst())
            assert(cursor.getString(0) == "Paracetamol 500")
            assert(cursor.getLong(1) == 1050L)
        }
        migrated.close()
    }
}
