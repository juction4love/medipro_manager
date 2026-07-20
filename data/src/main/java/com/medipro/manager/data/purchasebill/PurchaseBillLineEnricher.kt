package com.medipro.manager.data.purchasebill

import com.medipro.manager.core.database.dao.MedicineDao
import com.medipro.manager.core.database.dao.PurchaseItemDao
import com.medipro.manager.core.database.entity.MedicineEntity
import com.medipro.manager.domain.model.PosSearchResult
import com.medipro.manager.domain.model.PosSearchSource
import com.medipro.manager.domain.model.PurchaseBillLineMatch
import com.medipro.manager.domain.model.isNearExpiry
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class PurchaseBillLineEnricher @Inject constructor(
    private val medicineDao: MedicineDao,
    private val purchaseItemDao: PurchaseItemDao,
) {

    suspend fun enrich(line: PurchaseBillLineMatch): PurchaseBillLineMatch {
        val medicineId = line.match?.medicineId ?: return line.copy(
            nearExpiry = line.parsed.isNearExpiry(),
        )
        val medicine = medicineDao.getById(medicineId) ?: return line.copy(
            nearExpiry = line.parsed.isNearExpiry(),
        )
        return enrichWithMedicine(line, medicine)
    }

    suspend fun enrichWithMedicine(line: PurchaseBillLineMatch, medicine: MedicineEntity): PurchaseBillLineMatch {
        val dbMrp = medicine.mrpPaisa / 100.0
        val ocrMrp = line.parsed.mrp
        val mrpChanged = dbMrp > 0 && ocrMrp > 0 && abs(ocrMrp - dbMrp) > 0.01

        val previousPrice = purchaseItemDao.getLastUnitPrice(medicine.id)
        val costIncreasePercent = previousPrice?.takeIf { it > 0 }?.let { prev ->
            val current = line.parsed.unitPrice
            if (current <= prev) return@let null
            val pct = ((current - prev) / prev) * 100.0
            if (pct >= 10.0) pct else null
        }

        val isControlled = medicine.controlledSubstance ||
            medicine.scheduleCategory.uppercase() in CONTROLLED_SCHEDULES

        return line.copy(
            databaseMrp = dbMrp.takeIf { it > 0 },
            mrpChanged = mrpChanged,
            previousUnitPrice = previousPrice,
            costIncreasePercent = costIncreasePercent,
            nearExpiry = line.parsed.isNearExpiry(),
            isControlled = isControlled,
        )
    }

    fun medicineToSearchResult(medicine: MedicineEntity): PosSearchResult =
        PosSearchResult(
            key = "medicine:${medicine.id}",
            medicineId = medicine.id,
            catalogId = medicine.catalogUuid?.toLongOrNull(),
            brandName = medicine.brandName,
            genericName = medicine.genericName,
            composition = medicine.composition,
            strength = medicine.strength,
            dosageForm = medicine.dosageForm,
            manufacturer = medicine.manufacturer,
            barcode = medicine.barcode,
            sellingPrice = medicine.sellingPricePaisa / 100.0,
            mrp = medicine.mrpPaisa / 100.0,
            requiresPrescription = medicine.requiresPrescription,
            scheduleCategory = medicine.scheduleCategory,
            source = PosSearchSource.INVENTORY,
            matchScore = 99,
            matchKind = "alias",
        )

    companion object {
        private val CONTROLLED_SCHEDULES = setOf("X", "NARCOTIC", "SCHEDULE_X", "H1", "H", "SCHEDULE_H")
    }
}
