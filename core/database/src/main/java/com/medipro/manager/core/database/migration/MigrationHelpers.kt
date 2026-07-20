package com.medipro.manager.core.database.migration

import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

internal object MigrationHelpers {

    fun SupportSQLiteDatabase.hasColumn(table: String, column: String): Boolean {
        query("PRAGMA table_info(`$table`)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == column) return true
            }
        }
        return false
    }

    fun SupportSQLiteDatabase.addColumnIfMissing(sql: String) {
        execSQL(sql)
    }

    fun SupportSQLiteDatabase.backfillUuidColumn(table: String, idColumn: String = "id") {
        if (!hasColumn(table, "uuid")) return
        query("SELECT `$idColumn` FROM `$table` WHERE uuid IS NULL OR uuid = ''").use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                execSQL(
                    "UPDATE `$table` SET uuid = ? WHERE `$idColumn` = ?",
                    arrayOf(UUID.randomUUID().toString(), id),
                )
            }
        }
    }

    fun SupportSQLiteDatabase.rebuildMedicinesFts() {
        execSQL("DROP TABLE IF EXISTS medicines_fts")
        execSQL(
            """
            CREATE VIRTUAL TABLE IF NOT EXISTS medicines_fts USING FTS4(
                brandName TEXT NOT NULL,
                genericName TEXT NOT NULL,
                composition TEXT NOT NULL,
                strength TEXT NOT NULL,
                manufacturer TEXT NOT NULL,
                barcode TEXT,
                content=`medicines`
            )
            """.trimIndent()
        )
        execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_medicines_fts_BEFORE_UPDATE
            BEFORE UPDATE ON `medicines` BEGIN
              DELETE FROM `medicines_fts` WHERE `docid`=OLD.`rowid`;
            END
            """.trimIndent()
        )
        execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_medicines_fts_BEFORE_DELETE
            BEFORE DELETE ON `medicines` BEGIN
              DELETE FROM `medicines_fts` WHERE `docid`=OLD.`rowid`;
            END
            """.trimIndent()
        )
        execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_medicines_fts_AFTER_UPDATE
            AFTER UPDATE ON `medicines` BEGIN
              INSERT INTO `medicines_fts`(
                `docid`, `brandName`, `genericName`, `composition`, `strength`, `manufacturer`, `barcode`
              ) VALUES (
                NEW.`rowid`, NEW.`brandName`, NEW.`genericName`, NEW.`composition`,
                NEW.`strength`, NEW.`manufacturer`, NEW.`barcode`
              );
            END
            """.trimIndent()
        )
        execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_medicines_fts_AFTER_INSERT
            AFTER INSERT ON `medicines` BEGIN
              INSERT INTO `medicines_fts`(
                `docid`, `brandName`, `genericName`, `composition`, `strength`, `manufacturer`, `barcode`
              ) VALUES (
                NEW.`rowid`, NEW.`brandName`, NEW.`genericName`, NEW.`composition`,
                NEW.`strength`, NEW.`manufacturer`, NEW.`barcode`
              );
            END
            """.trimIndent()
        )
        execSQL(
            """
            INSERT INTO medicines_fts(
                docid, brandName, genericName, composition, strength, manufacturer, barcode
            )
            SELECT rowid, brandName, genericName, composition, strength, manufacturer, barcode
            FROM medicines
            """.trimIndent()
        )
    }

    fun Double.toPaisa(): Long = (this * 100.0).toLong()
}
