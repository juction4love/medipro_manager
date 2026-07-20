package com.medipro.manager.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.medipro.manager.core.database.entity.SettingsEntity
import com.medipro.manager.core.database.migration.MigrationHelpers.backfillUuidColumn
import com.medipro.manager.core.database.migration.MigrationHelpers.hasColumn
import com.medipro.manager.core.database.migration.MigrationHelpers.rebuildMedicinesFts
import com.medipro.manager.core.database.migration.MigrationHelpers.toPaisa
import java.util.UUID

object DatabaseMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            migrateMedicinesV1ToV2(db)
            migrateSalesPrescriptionColumns(db)
            migrateSettingsPrescriptionFlags(db)
            createAuditLogs(db)
            createPendingOperations(db)
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            migrateLicenseV2ToV3(db)
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            addPurchaseAndSaleSyncColumns(db)
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            addCloudSyncColumnsV5(db)
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createPurchaseReturnTables(db)
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createSaleReturnTables(db)
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createStockAdjustmentsTable(db)
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_batches_expiryDate_medicineId` ON batches (`expiryDate`, `medicineId`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_purchases_purchaseDate_deletedAt` ON purchases (`purchaseDate`, `deletedAt`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_sales_saleDate_deletedAt` ON sales (`saleDate`, `deletedAt`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_sale_items_medicineId_deletedAt` ON sale_items (`medicineId`, `deletedAt`)"
            )
        }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()
            db.execSQL(
                "ALTER TABLE settings ADD COLUMN uuid TEXT NOT NULL DEFAULT '${SettingsEntity.SETTINGS_UUID}'"
            )
            db.execSQL("ALTER TABLE settings ADD COLUMN pharmacyUuid TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE settings ADD COLUMN createdAt INTEGER NOT NULL DEFAULT $now")
            db.execSQL("ALTER TABLE settings ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
            db.execSQL("ALTER TABLE settings ADD COLUMN syncVersion INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE settings ADD COLUMN deviceId TEXT")
            db.execSQL(
                "UPDATE settings SET createdAt = updatedAt WHERE createdAt = 0 OR createdAt IS NULL"
            )
        }
    }

    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            if (!db.hasColumn("sales", "printCount")) {
                db.execSQL("ALTER TABLE sales ADD COLUMN printCount INTEGER NOT NULL DEFAULT 0")
            }
            if (!db.hasColumn("sales", "lastPrintedAt")) {
                db.execSQL("ALTER TABLE sales ADD COLUMN lastPrintedAt INTEGER")
            }
        }
    }

    private fun migrateMedicinesV1ToV2(db: SupportSQLiteDatabase) {
        db.execSQL("DROP INDEX IF EXISTS index_medicines_name")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS medicines_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                uuid TEXT NOT NULL,
                brandName TEXT NOT NULL,
                genericName TEXT NOT NULL,
                composition TEXT NOT NULL,
                strength TEXT NOT NULL,
                dosageForm TEXT NOT NULL,
                manufacturer TEXT NOT NULL,
                category TEXT NOT NULL,
                barcode TEXT,
                unit TEXT NOT NULL,
                purchasePricePaisa INTEGER NOT NULL,
                sellingPricePaisa INTEGER NOT NULL,
                mrpPaisa INTEGER NOT NULL,
                vatPercent REAL NOT NULL,
                reorderLevel INTEGER NOT NULL,
                description TEXT,
                requiresPrescription INTEGER NOT NULL,
                controlledSubstance INTEGER NOT NULL,
                scheduleCategory TEXT NOT NULL,
                isActive INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                deletedAt INTEGER,
                syncStatus TEXT NOT NULL,
                syncVersion INTEGER NOT NULL,
                deviceId TEXT
            )
            """.trimIndent()
        )

        db.query("SELECT * FROM medicines").use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                val brandName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val genericName = cursor.getString(cursor.getColumnIndexOrThrow("genericName"))
                val manufacturer = cursor.getString(cursor.getColumnIndexOrThrow("company"))
                val category = cursor.getString(cursor.getColumnIndexOrThrow("category"))
                val barcode = cursor.getString(cursor.getColumnIndexOrThrow("barcode"))
                val unit = cursor.getString(cursor.getColumnIndexOrThrow("unit"))
                val purchasePrice = cursor.getDouble(cursor.getColumnIndexOrThrow("purchasePrice"))
                val sellingPrice = cursor.getDouble(cursor.getColumnIndexOrThrow("sellingPrice"))
                val mrp = cursor.getDouble(cursor.getColumnIndexOrThrow("mrp"))
                val vatPercent = cursor.getDouble(cursor.getColumnIndexOrThrow("vatPercent"))
                val reorderLevel = cursor.getInt(cursor.getColumnIndexOrThrow("reorderLevel"))
                val description = cursor.getString(cursor.getColumnIndexOrThrow("description"))
                val isActive = cursor.getInt(cursor.getColumnIndexOrThrow("isActive"))
                val createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt"))
                val updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updatedAt"))

                db.execSQL(
                    """
                    INSERT INTO medicines_new (
                        id, uuid, brandName, genericName, composition, strength, dosageForm,
                        manufacturer, category, barcode, unit, purchasePricePaisa, sellingPricePaisa,
                        mrpPaisa, vatPercent, reorderLevel, description, requiresPrescription,
                        controlledSubstance, scheduleCategory, isActive, createdAt, updatedAt,
                        deletedAt, syncStatus, syncVersion, deviceId
                    ) VALUES (?, ?, ?, ?, '', '', 'Tablet', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, 'OTC', ?, ?, ?, NULL, 'PENDING', 0, NULL)
                    """.trimIndent(),
                    arrayOf(
                        id,
                        UUID.randomUUID().toString(),
                        brandName,
                        genericName,
                        manufacturer,
                        category,
                        barcode,
                        unit,
                        purchasePrice.toPaisa(),
                        sellingPrice.toPaisa(),
                        mrp.toPaisa(),
                        vatPercent,
                        reorderLevel,
                        description,
                        isActive,
                        createdAt,
                        updatedAt,
                    ),
                )
            }
        }

        db.execSQL("DROP TABLE medicines")
        db.execSQL("ALTER TABLE medicines_new RENAME TO medicines")

        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_medicines_uuid ON medicines (uuid)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_medicines_barcode ON medicines (barcode)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_medicines_brandName ON medicines (brandName)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_medicines_genericName ON medicines (genericName)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_medicines_composition ON medicines (composition)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_medicines_manufacturer ON medicines (manufacturer)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_medicines_syncStatus ON medicines (syncStatus)")

        db.rebuildMedicinesFts()
    }

    private fun migrateSalesPrescriptionColumns(db: SupportSQLiteDatabase) {
        if (!db.hasColumn("sales", "prescriptionNumber")) {
            db.execSQL("ALTER TABLE sales ADD COLUMN prescriptionNumber TEXT")
        }
        if (!db.hasColumn("sales", "doctorName")) {
            db.execSQL("ALTER TABLE sales ADD COLUMN doctorName TEXT")
        }
        if (!db.hasColumn("sales", "patientName")) {
            db.execSQL("ALTER TABLE sales ADD COLUMN patientName TEXT")
        }
    }

    private fun migrateSettingsPrescriptionFlags(db: SupportSQLiteDatabase) {
        if (!db.hasColumn("settings", "prescriptionModuleEnabled")) {
            db.execSQL("ALTER TABLE settings ADD COLUMN prescriptionModuleEnabled INTEGER NOT NULL DEFAULT 1")
        }
        if (!db.hasColumn("settings", "requirePrescriptionDetails")) {
            db.execSQL("ALTER TABLE settings ADD COLUMN requirePrescriptionDetails INTEGER NOT NULL DEFAULT 1")
        }
    }

    private fun createAuditLogs(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS audit_logs (
                uuid TEXT NOT NULL,
                eventType TEXT NOT NULL,
                entityType TEXT NOT NULL,
                entityUuid TEXT,
                entityLocalId INTEGER,
                description TEXT NOT NULL,
                oldValue TEXT,
                newValue TEXT,
                deviceId TEXT,
                userId TEXT,
                createdAt INTEGER NOT NULL,
                PRIMARY KEY(uuid)
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_audit_logs_entityType_entityUuid ON audit_logs (entityType, entityUuid)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_audit_logs_eventType ON audit_logs (eventType)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_audit_logs_createdAt ON audit_logs (createdAt)")
    }

    private fun createPendingOperations(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS pending_operations (
                uuid TEXT NOT NULL,
                operationType TEXT NOT NULL,
                entityType TEXT NOT NULL,
                entityUuid TEXT NOT NULL,
                payloadJson TEXT NOT NULL,
                status TEXT NOT NULL,
                retryCount INTEGER NOT NULL,
                lastError TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                PRIMARY KEY(uuid)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_pending_operations_status ON pending_operations (status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_pending_operations_createdAt ON pending_operations (createdAt)")
    }

    private fun migrateLicenseV2ToV3(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS license_new (
                id INTEGER NOT NULL,
                licenseId TEXT NOT NULL,
                licenseKey TEXT NOT NULL,
                mobileNumber TEXT NOT NULL,
                pharmacyName TEXT NOT NULL,
                ownerName TEXT NOT NULL,
                deviceId TEXT NOT NULL,
                plan TEXT NOT NULL,
                status TEXT NOT NULL,
                activatedAt INTEGER NOT NULL,
                expiresAt INTEGER NOT NULL,
                lastVerifiedAt INTEGER,
                isActive INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO license_new (
                id, licenseId, licenseKey, mobileNumber, pharmacyName, ownerName, deviceId,
                plan, status, activatedAt, expiresAt, lastVerifiedAt, isActive
            )
            SELECT
                id,
                COALESCE(NULLIF(licenseKey, ''), 'legacy-' || id),
                licenseKey,
                '',
                pharmacyName,
                ownerName,
                deviceId,
                'FREE',
                'ACTIVE',
                activatedAt,
                COALESCE(expiresAt, activatedAt + 31536000000),
                NULL,
                isActive
            FROM license
            """.trimIndent()
        )
        db.execSQL("DROP TABLE license")
        db.execSQL("ALTER TABLE license_new RENAME TO license")
    }

    private fun addPurchaseAndSaleSyncColumns(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE purchases ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE purchases ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE purchases ADD COLUMN deletedAt INTEGER")
        db.execSQL("ALTER TABLE purchases ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'")
        db.execSQL("ALTER TABLE purchases ADD COLUMN syncVersion INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE purchases ADD COLUMN deviceId TEXT")
        db.execSQL("UPDATE purchases SET updatedAt = purchaseDate WHERE updatedAt = 0")
        db.backfillUuidColumn("purchases")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_purchases_uuid ON purchases (uuid)")

        db.execSQL("ALTER TABLE sales ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE sales ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE sales ADD COLUMN deletedAt INTEGER")
        db.execSQL("ALTER TABLE sales ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'")
        db.execSQL("ALTER TABLE sales ADD COLUMN syncVersion INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE sales ADD COLUMN deviceId TEXT")
        db.execSQL("UPDATE sales SET updatedAt = saleDate WHERE updatedAt = 0")
        db.backfillUuidColumn("sales")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sales_uuid ON sales (uuid)")
    }

    private fun addCloudSyncColumnsV5(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE medicines ADD COLUMN catalogUuid TEXT")
        db.execSQL("ALTER TABLE medicines ADD COLUMN pharmacyUuid TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE medicines ADD COLUMN branchUuid TEXT")
        db.execSQL("ALTER TABLE medicines ADD COLUMN createdBy TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_medicines_catalogUuid ON medicines (catalogUuid)")

        addEntitySyncColumns(db, "suppliers")
        addEntitySyncColumns(db, "customers")
        addEntitySyncColumns(db, "batches", extraColumns = listOf("barcode TEXT"))
        db.execSQL("ALTER TABLE purchases ADD COLUMN pharmacyUuid TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE purchases ADD COLUMN branchUuid TEXT")
        db.execSQL("ALTER TABLE purchases ADD COLUMN supplierUuid TEXT")
        db.execSQL("ALTER TABLE purchases ADD COLUMN createdBy TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_purchases_supplierUuid ON purchases (supplierUuid)")

        addLineItemSyncColumns(db, "purchase_items")
        db.execSQL("ALTER TABLE sales ADD COLUMN pharmacyUuid TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE sales ADD COLUMN branchUuid TEXT")
        db.execSQL("ALTER TABLE sales ADD COLUMN customerUuid TEXT")
        db.execSQL("ALTER TABLE sales ADD COLUMN createdBy TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_customerUuid ON sales (customerUuid)")

        addLineItemSyncColumns(db, "sale_items")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_sale_items_batchId ON sale_items (batchId)")

        addReturnSyncColumns(db)
        addLedgerPaymentSyncColumns(db)
        addAuditLogSyncColumnsV5(db)
    }

    private fun addEntitySyncColumns(
        db: SupportSQLiteDatabase,
        table: String,
        extraColumns: List<String> = emptyList(),
    ) {
        db.execSQL("ALTER TABLE `$table` ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `$table` ADD COLUMN pharmacyUuid TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `$table` ADD COLUMN branchUuid TEXT")
        db.execSQL("ALTER TABLE `$table` ADD COLUMN deletedAt INTEGER")
        db.execSQL("ALTER TABLE `$table` ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'")
        db.execSQL("ALTER TABLE `$table` ADD COLUMN syncVersion INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `$table` ADD COLUMN deviceId TEXT")
        db.execSQL("ALTER TABLE `$table` ADD COLUMN createdBy TEXT")
        extraColumns.forEach { db.execSQL("ALTER TABLE `$table` ADD COLUMN $it") }
        db.backfillUuidColumn(table)
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_${table}_uuid ON `$table` (uuid)")
    }

    private fun addLineItemSyncColumns(db: SupportSQLiteDatabase, table: String) {
        db.execSQL("ALTER TABLE `$table` ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `$table` ADD COLUMN pharmacyUuid TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `$table` ADD COLUMN branchUuid TEXT")
        db.execSQL("ALTER TABLE `$table` ADD COLUMN medicineUuid TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `$table` ADD COLUMN batchUuid TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `$table` ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `$table` ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `$table` ADD COLUMN deletedAt INTEGER")
        db.execSQL("ALTER TABLE `$table` ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'")
        db.execSQL("ALTER TABLE `$table` ADD COLUMN syncVersion INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `$table` ADD COLUMN deviceId TEXT")
        db.execSQL("ALTER TABLE `$table` ADD COLUMN createdBy TEXT")
        db.backfillUuidColumn(table)
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_${table}_uuid ON `$table` (uuid)")
    }

    private fun addReturnSyncColumns(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE returns ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE returns ADD COLUMN pharmacyUuid TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE returns ADD COLUMN branchUuid TEXT")
        db.execSQL("ALTER TABLE returns ADD COLUMN referenceUuid TEXT")
        db.execSQL("ALTER TABLE returns ADD COLUMN medicineUuid TEXT")
        db.execSQL("ALTER TABLE returns ADD COLUMN batchUuid TEXT")
        db.execSQL("ALTER TABLE returns ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE returns ADD COLUMN deletedAt INTEGER")
        db.execSQL("ALTER TABLE returns ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'")
        db.execSQL("ALTER TABLE returns ADD COLUMN syncVersion INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE returns ADD COLUMN deviceId TEXT")
        db.execSQL("ALTER TABLE returns ADD COLUMN createdBy TEXT")
        db.backfillUuidColumn("returns")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_returns_uuid ON returns (uuid)")
    }

    private fun addLedgerPaymentSyncColumns(db: SupportSQLiteDatabase) {
        for (table in listOf("ledger", "payments")) {
            db.execSQL("ALTER TABLE `$table` ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `$table` ADD COLUMN pharmacyUuid TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `$table` ADD COLUMN branchUuid TEXT")
            if (table == "ledger") {
                db.execSQL("ALTER TABLE `$table` ADD COLUMN accountUuid TEXT")
            }
            db.execSQL("ALTER TABLE `$table` ADD COLUMN referenceUuid TEXT")
            db.execSQL("ALTER TABLE `$table` ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `$table` ADD COLUMN deletedAt INTEGER")
            db.execSQL("ALTER TABLE `$table` ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'")
            db.execSQL("ALTER TABLE `$table` ADD COLUMN syncVersion INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `$table` ADD COLUMN deviceId TEXT")
            db.execSQL("ALTER TABLE `$table` ADD COLUMN createdBy TEXT")
            db.backfillUuidColumn(table)
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_${table}_uuid ON `$table` (uuid)")
        }
    }

    private fun addAuditLogSyncColumnsV5(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE audit_logs ADD COLUMN pharmacyUuid TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE audit_logs ADD COLUMN branchUuid TEXT")
        db.execSQL("ALTER TABLE audit_logs ADD COLUMN createdBy TEXT")
        db.execSQL("ALTER TABLE audit_logs ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE audit_logs ADD COLUMN deletedAt INTEGER")
        db.execSQL("ALTER TABLE audit_logs ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'")
        db.execSQL("ALTER TABLE audit_logs ADD COLUMN syncVersion INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE audit_logs SET updatedAt = createdAt WHERE updatedAt = 0")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_audit_logs_uuid ON audit_logs (uuid)")
    }

    private fun createPurchaseReturnTables(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS purchase_returns (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                uuid TEXT NOT NULL,
                pharmacyUuid TEXT NOT NULL,
                branchUuid TEXT,
                purchaseId INTEGER NOT NULL,
                purchaseUuid TEXT NOT NULL,
                supplierId INTEGER,
                supplierUuid TEXT,
                returnNumber TEXT NOT NULL,
                reason TEXT NOT NULL,
                returnDate INTEGER NOT NULL,
                subtotalPaisa INTEGER NOT NULL,
                vatPaisa INTEGER NOT NULL,
                discountPaisa INTEGER NOT NULL,
                grandTotalPaisa INTEGER NOT NULL,
                notes TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                deletedAt INTEGER,
                syncStatus TEXT NOT NULL,
                syncVersion INTEGER NOT NULL,
                deviceId TEXT,
                createdBy TEXT,
                FOREIGN KEY(purchaseId) REFERENCES purchases(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(supplierId) REFERENCES suppliers(id) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_purchase_returns_uuid ON purchase_returns (uuid)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_purchase_returns_purchaseUuid ON purchase_returns (purchaseUuid)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_purchase_returns_purchaseId ON purchase_returns (purchaseId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_purchase_returns_returnDate ON purchase_returns (returnDate)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS purchase_return_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                uuid TEXT NOT NULL,
                pharmacyUuid TEXT NOT NULL,
                branchUuid TEXT,
                purchaseReturnId INTEGER NOT NULL,
                purchaseReturnUuid TEXT NOT NULL,
                purchaseItemId INTEGER NOT NULL,
                purchaseItemUuid TEXT NOT NULL,
                medicineId INTEGER NOT NULL,
                medicineUuid TEXT NOT NULL,
                batchId INTEGER NOT NULL,
                batchUuid TEXT NOT NULL,
                quantity INTEGER NOT NULL,
                costPricePaisa INTEGER NOT NULL,
                amountPaisa INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                deletedAt INTEGER,
                syncStatus TEXT NOT NULL,
                syncVersion INTEGER NOT NULL,
                deviceId TEXT,
                createdBy TEXT,
                FOREIGN KEY(purchaseReturnId) REFERENCES purchase_returns(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(purchaseItemId) REFERENCES purchase_items(id) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_purchase_return_items_uuid ON purchase_return_items (uuid)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_purchase_return_items_purchaseReturnId ON purchase_return_items (purchaseReturnId)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_purchase_return_items_purchaseItemUuid ON purchase_return_items (purchaseItemUuid)"
        )
    }

    private fun createSaleReturnTables(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS sale_returns (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                uuid TEXT NOT NULL,
                pharmacyUuid TEXT NOT NULL,
                branchUuid TEXT,
                saleId INTEGER NOT NULL,
                invoiceUuid TEXT NOT NULL,
                customerId INTEGER,
                customerUuid TEXT,
                returnNumber TEXT NOT NULL,
                reason TEXT NOT NULL,
                returnDate INTEGER NOT NULL,
                subtotalPaisa INTEGER NOT NULL,
                discountPaisa INTEGER NOT NULL,
                vatPaisa INTEGER NOT NULL,
                grandTotalPaisa INTEGER NOT NULL,
                notes TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                deletedAt INTEGER,
                syncStatus TEXT NOT NULL,
                syncVersion INTEGER NOT NULL,
                deviceId TEXT,
                createdBy TEXT,
                FOREIGN KEY(saleId) REFERENCES sales(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(customerId) REFERENCES customers(id) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sale_returns_uuid ON sale_returns (uuid)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_sale_returns_invoiceUuid ON sale_returns (invoiceUuid)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_sale_returns_saleId ON sale_returns (saleId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_sale_returns_returnDate ON sale_returns (returnDate)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS sale_return_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                uuid TEXT NOT NULL,
                pharmacyUuid TEXT NOT NULL,
                branchUuid TEXT,
                saleReturnId INTEGER NOT NULL,
                saleReturnUuid TEXT NOT NULL,
                saleItemId INTEGER NOT NULL,
                invoiceItemUuid TEXT NOT NULL,
                medicineId INTEGER NOT NULL,
                medicineUuid TEXT NOT NULL,
                batchId INTEGER NOT NULL,
                batchUuid TEXT NOT NULL,
                quantity INTEGER NOT NULL,
                sellingPricePaisa INTEGER NOT NULL,
                amountPaisa INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                deletedAt INTEGER,
                syncStatus TEXT NOT NULL,
                syncVersion INTEGER NOT NULL,
                deviceId TEXT,
                createdBy TEXT,
                FOREIGN KEY(saleReturnId) REFERENCES sale_returns(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(saleItemId) REFERENCES sale_items(id) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sale_return_items_uuid ON sale_return_items (uuid)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_sale_return_items_saleReturnId ON sale_return_items (saleReturnId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_sale_return_items_invoiceItemUuid ON sale_return_items (invoiceItemUuid)")
    }

    private fun createStockAdjustmentsTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS stock_adjustments (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                uuid TEXT NOT NULL,
                pharmacyUuid TEXT NOT NULL,
                branchUuid TEXT,
                adjustmentNumber TEXT NOT NULL,
                medicineId INTEGER NOT NULL,
                medicineUuid TEXT NOT NULL,
                batchId INTEGER NOT NULL,
                batchUuid TEXT NOT NULL,
                type TEXT NOT NULL,
                oldQty INTEGER NOT NULL,
                adjustQty INTEGER NOT NULL,
                newQty INTEGER NOT NULL,
                reason TEXT NOT NULL,
                remarks TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                deletedAt INTEGER,
                syncStatus TEXT NOT NULL,
                syncVersion INTEGER NOT NULL,
                deviceId TEXT,
                createdBy TEXT,
                FOREIGN KEY(medicineId) REFERENCES medicines(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(batchId) REFERENCES batches(id) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_stock_adjustments_uuid ON stock_adjustments (uuid)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_adjustments_medicineUuid ON stock_adjustments (medicineUuid)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_adjustments_batchUuid ON stock_adjustments (batchUuid)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_adjustments_type ON stock_adjustments (type)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_adjustments_createdAt ON stock_adjustments (createdAt)")
    }

    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS day_closings (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    uuid TEXT NOT NULL,
                    pharmacyUuid TEXT NOT NULL,
                    closingDate INTEGER NOT NULL,
                    openingCash REAL NOT NULL,
                    cashSales REAL NOT NULL,
                    cardSales REAL NOT NULL,
                    esewaSales REAL NOT NULL,
                    khaltiSales REAL NOT NULL,
                    imeSales REAL NOT NULL,
                    creditSales REAL NOT NULL,
                    customerReceipts REAL NOT NULL,
                    supplierPayments REAL NOT NULL,
                    expenses REAL NOT NULL,
                    returnsAmount REAL NOT NULL,
                    salesCount INTEGER NOT NULL,
                    returnCount INTEGER NOT NULL,
                    discountTotal REAL NOT NULL,
                    vatTotal REAL NOT NULL,
                    expectedCash REAL NOT NULL,
                    actualCash REAL NOT NULL,
                    difference REAL NOT NULL,
                    differenceReason TEXT,
                    remarks TEXT,
                    reference TEXT NOT NULL,
                    pdfPath TEXT,
                    closedAt INTEGER NOT NULL,
                    deviceId TEXT,
                    createdAt INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_day_closings_closingDate ON day_closings (closingDate)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_day_closings_uuid ON day_closings (uuid)")
        }
    }

    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS ocr_medicine_aliases (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    normalizedText TEXT NOT NULL,
                    ocrText TEXT NOT NULL,
                    medicineId INTEGER NOT NULL,
                    medicineUuid TEXT,
                    medicineName TEXT NOT NULL,
                    hitCount INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_ocr_medicine_aliases_normalizedText ON ocr_medicine_aliases (normalizedText)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_ocr_medicine_aliases_medicineId ON ocr_medicine_aliases (medicineId)")
        }
    }

    val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE ocr_medicine_aliases ADD COLUMN isEnabled INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE settings ADD COLUMN ocrFeedbackOptIn INTEGER NOT NULL DEFAULT 0")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS ocr_scan_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    scannedAt INTEGER NOT NULL,
                    pageCount INTEGER NOT NULL,
                    totalLines INTEGER NOT NULL,
                    matchedLines INTEGER NOT NULL,
                    aliasMatchedLines INTEGER NOT NULL,
                    manualCorrections INTEGER NOT NULL,
                    avgConfidence INTEGER NOT NULL,
                    parserUsed TEXT NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_ocr_scan_sessions_scannedAt ON ocr_scan_sessions (scannedAt)")
        }
    }

    val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            if (!db.hasColumn("ocr_scan_sessions", "supplierName")) {
                db.execSQL("ALTER TABLE ocr_scan_sessions ADD COLUMN supplierName TEXT")
            }
        }
    }

    val ALL: Array<Migration> = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9,
        MIGRATION_9_10,
        MIGRATION_10_11,
        MIGRATION_11_12,
        MIGRATION_12_13,
        MIGRATION_13_14,
        MIGRATION_14_15,
    )
}
