package com.medipro.manager.data.mapper

import com.medipro.manager.core.database.entity.BackupHistoryEntity
import com.medipro.manager.core.database.entity.CustomerEntity
import com.medipro.manager.core.database.entity.LicenseEntity
import com.medipro.manager.core.database.entity.MedicineEntity
import com.medipro.manager.core.database.entity.PurchaseEntity
import com.medipro.manager.core.database.entity.PurchaseItemEntity
import com.medipro.manager.core.database.entity.SaleEntity
import com.medipro.manager.core.database.entity.SaleItemEntity
import com.medipro.manager.core.database.entity.SettingsEntity
import com.medipro.manager.core.database.entity.SupplierEntity
import com.medipro.manager.domain.model.BackupRecord
import com.medipro.manager.domain.model.Customer
import com.medipro.manager.domain.model.License
import com.medipro.manager.domain.model.Medicine
import com.medipro.manager.domain.model.PharmacySettings
import com.medipro.manager.domain.model.Purchase
import com.medipro.manager.domain.model.PurchaseItem
import com.medipro.manager.domain.model.Sale
import com.medipro.manager.domain.model.SaleItem
import com.medipro.manager.domain.model.Supplier

fun MedicineEntity.toDomain(stockQuantity: Int = 0) = Medicine(
    id = id,
    uuid = uuid,
    brandName = brandName,
    genericName = genericName,
    composition = composition,
    strength = strength,
    dosageForm = dosageForm,
    manufacturer = manufacturer,
    category = category,
    barcode = barcode,
    unit = unit,
    purchasePrice = purchasePricePaisa / 100.0,
    sellingPrice = sellingPricePaisa / 100.0,
    mrp = mrpPaisa / 100.0,
    vatPercent = vatPercent,
    reorderLevel = reorderLevel,
    description = description,
    requiresPrescription = requiresPrescription,
    controlledSubstance = controlledSubstance,
    scheduleCategory = scheduleCategory,
    isActive = isActive,
    stockQuantity = stockQuantity,
    syncStatus = syncStatus,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Medicine.toEntity() = MedicineEntity(
    id = id,
    uuid = uuid.ifBlank { java.util.UUID.randomUUID().toString() },
    brandName = brandName,
    genericName = genericName,
    composition = composition,
    strength = strength,
    dosageForm = dosageForm,
    manufacturer = manufacturer,
    category = category,
    barcode = barcode,
    unit = unit,
    purchasePricePaisa = kotlin.math.round(purchasePrice * 100).toLong(),
    sellingPricePaisa = kotlin.math.round(sellingPrice * 100).toLong(),
    mrpPaisa = kotlin.math.round(mrp * 100).toLong(),
    vatPercent = vatPercent,
    reorderLevel = reorderLevel,
    description = description,
    requiresPrescription = requiresPrescription,
    controlledSubstance = controlledSubstance,
    scheduleCategory = scheduleCategory,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus
)

fun SupplierEntity.toDomain() = Supplier(
    id = id,
    uuid = uuid,
    name = name,
    contactPerson = contactPerson,
    phone = phone,
    email = email,
    address = address,
    panNumber = panNumber,
    creditLimit = creditLimit,
    outstandingBalance = outstandingBalance,
    isActive = isActive
)

fun Supplier.toEntity() = SupplierEntity(
    id = id,
    uuid = uuid.ifBlank { java.util.UUID.randomUUID().toString() },
    name = name,
    contactPerson = contactPerson,
    phone = phone,
    email = email,
    address = address,
    panNumber = panNumber,
    creditLimit = creditLimit,
    outstandingBalance = outstandingBalance,
    isActive = isActive
)

fun CustomerEntity.toDomain() = Customer(
    id = id,
    uuid = uuid,
    name = name,
    phone = phone,
    email = email,
    address = address,
    creditLimit = creditLimit,
    outstandingBalance = outstandingBalance,
    isActive = isActive
)

fun Customer.toEntity() = CustomerEntity(
    id = id,
    uuid = uuid.ifBlank { java.util.UUID.randomUUID().toString() },
    name = name,
    phone = phone,
    email = email,
    address = address,
    creditLimit = creditLimit,
    outstandingBalance = outstandingBalance,
    isActive = isActive
)

fun SaleEntity.toDomain(items: List<SaleItem> = emptyList(), customerName: String? = null) = Sale(
    id = id,
    uuid = uuid,
    customerId = customerId,
    customerName = customerName,
    invoiceNumber = invoiceNumber,
    saleDate = saleDate,
    subtotal = subtotal,
    discount = discount,
    vatAmount = vatAmount,
    totalAmount = totalAmount,
    paidAmount = paidAmount,
    paymentStatus = paymentStatus,
    paymentMethod = paymentMethod,
    isCredit = isCredit,
    prescriptionNumber = prescriptionNumber,
    doctorName = doctorName,
    patientName = patientName,
    items = items,
    printCount = printCount,
    lastPrintedAt = lastPrintedAt,
    syncVersion = syncVersion,
    isCancelled = deletedAt != null,
)

fun SaleItemEntity.toDomain(
    medicineName: String = "",
    batchNumber: String = "",
    expiryDate: Long? = null,
) = SaleItem(
    id = id,
    uuid = uuid,
    saleId = saleId,
    medicineId = medicineId,
    medicineUuid = medicineUuid,
    medicineName = medicineName,
    batchId = batchId,
    batchUuid = batchUuid,
    batchNumber = batchNumber,
    expiryDate = expiryDate,
    quantity = quantity,
    unitPrice = unitPrice,
    discount = discount,
    vatPercent = vatPercent,
    totalPrice = totalPrice
)

fun Sale.toEntity() = SaleEntity(
    id = id,
    uuid = uuid.ifBlank { java.util.UUID.randomUUID().toString() },
    customerId = customerId,
    invoiceNumber = invoiceNumber,
    saleDate = saleDate,
    subtotal = subtotal,
    discount = discount,
    vatAmount = vatAmount,
    totalAmount = totalAmount,
    paidAmount = paidAmount,
    paymentStatus = paymentStatus,
    paymentMethod = paymentMethod,
    isCredit = isCredit,
    prescriptionNumber = prescriptionNumber,
    doctorName = doctorName,
    patientName = patientName,
    createdAt = System.currentTimeMillis(),
    updatedAt = System.currentTimeMillis(),
    syncStatus = com.medipro.manager.core.database.entity.SyncStatus.PENDING,
    syncVersion = syncVersion,
)

fun SaleItem.toEntity(saleId: Long) = SaleItemEntity(
    id = id,
    uuid = uuid.ifBlank { java.util.UUID.randomUUID().toString() },
    saleId = saleId,
    medicineId = medicineId,
    medicineUuid = medicineUuid,
    batchId = batchId,
    batchUuid = batchUuid,
    quantity = quantity,
    unitPrice = unitPrice,
    discount = discount,
    vatPercent = vatPercent,
    totalPrice = totalPrice
)

fun PurchaseEntity.toDomain(items: List<PurchaseItem> = emptyList(), supplierName: String? = null) = Purchase(
    id = id,
    uuid = uuid,
    supplierId = supplierId,
    supplierName = supplierName,
    invoiceNumber = invoiceNumber,
    purchaseDate = purchaseDate,
    subtotal = subtotal,
    discount = discount,
    vatAmount = vatAmount,
    totalAmount = totalAmount,
    paidAmount = paidAmount,
    paymentStatus = paymentStatus,
    paymentMethod = paymentMethod,
    notes = notes,
    items = items,
    syncVersion = syncVersion,
)

fun PurchaseItemEntity.toDomain(medicineName: String = "") = PurchaseItem(
    id = id,
    uuid = uuid,
    purchaseId = purchaseId,
    medicineId = medicineId,
    medicineUuid = medicineUuid,
    medicineName = medicineName,
    batchUuid = batchUuid.orEmpty(),
    batchNumber = batchNumber,
    expiryDate = expiryDate,
    quantity = quantity,
    unitPrice = unitPrice,
    discount = discount,
    vatPercent = vatPercent,
    totalPrice = totalPrice
)

fun Purchase.toEntity() = PurchaseEntity(
    id = id,
    uuid = uuid.ifBlank { java.util.UUID.randomUUID().toString() },
    supplierId = supplierId,
    invoiceNumber = invoiceNumber,
    purchaseDate = purchaseDate,
    subtotal = subtotal,
    discount = discount,
    vatAmount = vatAmount,
    totalAmount = totalAmount,
    paidAmount = paidAmount,
    paymentStatus = paymentStatus,
    paymentMethod = paymentMethod,
    notes = notes,
    createdAt = System.currentTimeMillis(),
    updatedAt = System.currentTimeMillis(),
    syncStatus = com.medipro.manager.core.database.entity.SyncStatus.PENDING,
    syncVersion = syncVersion,
)

fun PurchaseItem.toEntity(purchaseId: Long) = PurchaseItemEntity(
    id = id,
    uuid = uuid.ifBlank { java.util.UUID.randomUUID().toString() },
    purchaseId = purchaseId,
    medicineId = medicineId,
    medicineUuid = medicineUuid,
    batchUuid = batchUuid.ifBlank { null },
    batchNumber = batchNumber,
    expiryDate = expiryDate,
    quantity = quantity,
    unitPrice = unitPrice,
    discount = discount,
    vatPercent = vatPercent,
    totalPrice = totalPrice
)

fun LicenseEntity.toDomain() = License(
    licenseId = licenseId,
    licenseKey = licenseKey,
    mobileNumber = mobileNumber,
    pharmacyName = pharmacyName,
    ownerName = ownerName,
    deviceId = deviceId,
    plan = plan,
    status = status,
    activatedAt = activatedAt,
    expiresAt = expiresAt,
    lastVerifiedAt = lastVerifiedAt,
    isActive = isActive,
)

fun License.toEntity() = LicenseEntity(
    licenseId = licenseId,
    licenseKey = licenseKey,
    mobileNumber = mobileNumber,
    pharmacyName = pharmacyName,
    ownerName = ownerName,
    deviceId = deviceId,
    plan = plan,
    status = status,
    activatedAt = activatedAt,
    expiresAt = expiresAt,
    lastVerifiedAt = lastVerifiedAt,
    isActive = isActive,
)

fun SettingsEntity.toDomain() = PharmacySettings(
    pharmacyName = pharmacyName,
    pharmacyAddress = pharmacyAddress,
    pharmacyPhone = pharmacyPhone,
    pharmacyEmail = pharmacyEmail,
    panNumber = panNumber,
    vatNumber = vatNumber,
    currency = currency,
    language = language,
    theme = theme,
    autoBackupEnabled = autoBackupEnabled,
    appLockEnabled = appLockEnabled,
    biometricEnabled = biometricEnabled,
    lowStockThreshold = lowStockThreshold,
    expiryAlertDays = expiryAlertDays,
    prescriptionModuleEnabled = prescriptionModuleEnabled,
    requirePrescriptionDetails = requirePrescriptionDetails,
    ocrFeedbackOptIn = ocrFeedbackOptIn,
)

fun PharmacySettings.toEntity() = SettingsEntity(
    pharmacyName = pharmacyName,
    pharmacyAddress = pharmacyAddress,
    pharmacyPhone = pharmacyPhone,
    pharmacyEmail = pharmacyEmail,
    panNumber = panNumber,
    vatNumber = vatNumber,
    currency = currency,
    language = language,
    theme = theme,
    autoBackupEnabled = autoBackupEnabled,
    appLockEnabled = appLockEnabled,
    biometricEnabled = biometricEnabled,
    lowStockThreshold = lowStockThreshold,
    expiryAlertDays = expiryAlertDays,
    prescriptionModuleEnabled = prescriptionModuleEnabled,
    requirePrescriptionDetails = requirePrescriptionDetails,
    ocrFeedbackOptIn = ocrFeedbackOptIn,
    updatedAt = System.currentTimeMillis()
)

fun BackupHistoryEntity.toDomain() = BackupRecord(
    id = id,
    fileName = fileName,
    filePath = filePath,
    fileSize = fileSize,
    isEncrypted = isEncrypted,
    backupType = backupType,
    createdAt = createdAt
)

fun BackupRecord.toEntity() = BackupHistoryEntity(
    id = id,
    fileName = fileName,
    filePath = filePath,
    fileSize = fileSize,
    isEncrypted = isEncrypted,
    backupType = backupType,
    createdAt = createdAt
)
