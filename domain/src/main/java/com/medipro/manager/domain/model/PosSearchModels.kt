package com.medipro.manager.domain.model

data class CatalogMedicine(
    val id: Long,
    val brandName: String,
    val genericName: String,
    val composition: String,
    val strength: String,
    val dosageForm: String,
    val manufacturer: String,
    val category: String,
    val barcode: String?,
)

data class PosSearchResult(
    val key: String,
    val catalogId: Long? = null,
    val medicineId: Long? = null,
    val brandName: String,
    val genericName: String,
    val composition: String,
    val strength: String,
    val dosageForm: String,
    val manufacturer: String,
    val barcode: String? = null,
    val stockQuantity: Int = 0,
    val sellingPrice: Double = 0.0,
    val mrp: Double = 0.0,
    val batchNumber: String? = null,
    val expiryDate: Long? = null,
    val reorderLevel: Int = 10,
    val inStock: Boolean = false,
    val requiresPrescription: Boolean = false,
    val scheduleCategory: String = "OTC",
    val source: PosSearchSource = PosSearchSource.INVENTORY,
    val matchScore: Int = 0,
    val matchKind: String? = null,
) {
    val isRxRequired: Boolean get() = requiresPrescription || scheduleCategory in RX_SCHEDULES
    val rxBadge: String? get() = when {
        scheduleCategory.uppercase() in NARCOTIC_SCHEDULES -> "Narcotic"
        scheduleCategory.uppercase() in SCHEDULE_H -> "Schedule H"
        requiresPrescription -> "RX"
        else -> null
    }
    val displaySubtitle: String
        get() = listOfNotNull(
            genericName.takeIf { it.isNotBlank() },
            composition.takeIf { it.isNotBlank() },
            manufacturer.takeIf { it.isNotBlank() },
        ).distinct().joinToString(" • ")

    val genericStrengthLabel: String
        get() = listOf(genericName, strength).filter { it.isNotBlank() }.joinToString(" ")

    val isLowStock: Boolean
        get() = inStock && stockQuantity in 1..reorderLevel

    val isExpiredStock: Boolean
        get() = inStock && expiryDate != null && expiryDate < System.currentTimeMillis()
}

enum class PosSearchSource {
    INVENTORY,
    CATALOG,
}

private val RX_SCHEDULES = setOf("H", "H1", "X", "NARCOTIC", "SCHEDULE_H", "SCHEDULE_X")
private val SCHEDULE_H = setOf("H", "H1", "SCHEDULE_H")
private val NARCOTIC_SCHEDULES = setOf("X", "NARCOTIC", "SCHEDULE_X")
