package com.medipro.manager.data.sync

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.medipro.manager.core.database.dao.BatchDao
import com.medipro.manager.core.database.dao.CustomerDao
import com.medipro.manager.core.database.dao.LedgerDao
import com.medipro.manager.core.database.dao.LicenseDao
import com.medipro.manager.core.database.dao.MedicineDao
import com.medipro.manager.core.database.dao.PaymentDao
import com.medipro.manager.core.database.dao.PendingOperationDao
import com.medipro.manager.core.database.dao.PurchaseDao
import com.medipro.manager.core.database.dao.SaleDao
import com.medipro.manager.core.database.dao.StockAdjustmentDao
import com.medipro.manager.core.database.dao.SettingsDao
import com.medipro.manager.core.database.dao.AuditLogDao
import com.medipro.manager.core.database.dao.SupplierDao
import com.medipro.manager.core.database.entity.SyncStatus
import com.medipro.manager.domain.model.PendingOperation
import com.medipro.manager.domain.model.SyncEntityType
import com.medipro.manager.domain.model.SyncStatusSnapshot
import com.medipro.manager.domain.repository.FirestoreSyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSyncRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val pendingOperationDao: PendingOperationDao,
    private val licenseDao: LicenseDao,
    private val saleDao: SaleDao,
    private val purchaseDao: PurchaseDao,
    private val customerDao: CustomerDao,
    private val supplierDao: SupplierDao,
    private val paymentDao: PaymentDao,
    private val ledgerDao: LedgerDao,
    private val medicineDao: MedicineDao,
    private val batchDao: BatchDao,
    private val stockAdjustmentDao: StockAdjustmentDao,
    private val settingsDao: SettingsDao,
    private val auditLogDao: AuditLogDao,
    private val remoteApplier: FirestoreRemoteApplier,
) : FirestoreSyncRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val listeners = mutableListOf<ListenerRegistration>()
    private var activePharmacyId: String? = null

    override suspend fun pushOperation(pharmacyId: String, operation: PendingOperation): Result<Unit> =
        runCatching {
            val envelope = json.decodeFromString<SyncDocumentEnvelope>(operation.payloadJson)
            val collection = FirestorePaths.collection(pharmacyId, envelope.entityType)
            val document = mapOf(
                "uuid" to envelope.uuid,
                "entityType" to envelope.entityType,
                "licenseId" to pharmacyId,
                "createdAt" to envelope.createdAt,
                "updatedAt" to envelope.updatedAt,
                "syncVersion" to envelope.syncVersion,
                "deletedAt" to envelope.deletedAt,
                "deviceId" to envelope.deviceId,
                "isSynced" to true,
                "payload" to envelope.payloadJson,
            )
            firestore.collection(collection).document(envelope.uuid)
                .set(document, SetOptions.merge())
                .await()
            markLocalEntitySynced(envelope)
        }

    override fun startListening(pharmacyId: String, localDeviceId: String?) {
        if (activePharmacyId == pharmacyId && listeners.isNotEmpty()) return
        stopListening()
        activePharmacyId = pharmacyId

        SYNC_LISTENER_TYPES.forEach { entityType ->
            val registration = firestore.collection(FirestorePaths.collection(pharmacyId, entityType))
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(LISTENER_LIMIT)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Timber.w(error, "Firestore listener error for $entityType")
                        return@addSnapshotListener
                    }
                    snapshot?.documentChanges?.forEach { change ->
                        val data = change.document.data
                        if (data["deviceId"] == localDeviceId) return@forEach
                        remoteApplier.applyRemoteDocument(entityType, data)
                    }
                }
            listeners += registration
        }
        Timber.i("Firestore sync listeners started for pharmacy $pharmacyId (${SYNC_LISTENER_TYPES.size} collections)")
    }

    private suspend fun markLocalEntitySynced(envelope: SyncDocumentEnvelope) {
        when (envelope.entityType) {
            SyncEntityType.INVOICE -> saleDao.getByUuid(envelope.uuid)?.let { sale ->
                saleDao.updateSyncState(sale.id, SyncStatus.SYNCED, envelope.syncVersion, envelope.updatedAt)
            }
            SyncEntityType.PURCHASE -> purchaseDao.getByUuid(envelope.uuid)?.let { purchase ->
                purchaseDao.updateSyncState(purchase.id, SyncStatus.SYNCED, envelope.syncVersion, envelope.updatedAt)
            }
            SyncEntityType.CUSTOMER -> customerDao.getByUuid(envelope.uuid)?.let { customer ->
                customerDao.updateSyncState(customer.id, SyncStatus.SYNCED, envelope.syncVersion, envelope.updatedAt)
            }
            SyncEntityType.SUPPLIER -> supplierDao.getByUuid(envelope.uuid)?.let { supplier ->
                supplierDao.updateSyncState(supplier.id, SyncStatus.SYNCED, envelope.syncVersion, envelope.updatedAt)
            }
            SyncEntityType.PAYMENT -> paymentDao.getByUuid(envelope.uuid)?.let { payment ->
                paymentDao.updateSyncState(payment.id, SyncStatus.SYNCED, envelope.syncVersion, envelope.updatedAt)
            }
            SyncEntityType.LEDGER -> ledgerDao.getByUuid(envelope.uuid)?.let { entry ->
                ledgerDao.updateSyncState(entry.id, SyncStatus.SYNCED, envelope.syncVersion, envelope.updatedAt)
            }
            SyncEntityType.MEDICINE -> medicineDao.getByUuid(envelope.uuid)?.let { medicine ->
                medicineDao.updateSyncState(medicine.id, SyncStatus.SYNCED, envelope.syncVersion, envelope.updatedAt)
            }
            SyncEntityType.STOCK_BATCH -> batchDao.getByUuid(envelope.uuid)?.let { batch ->
                batchDao.updateSyncState(batch.id, SyncStatus.SYNCED, envelope.syncVersion, envelope.updatedAt)
            }
            SyncEntityType.STOCK_ADJUSTMENT -> stockAdjustmentDao.getByUuid(envelope.uuid)?.let { adjustment ->
                stockAdjustmentDao.updateSyncState(
                    adjustment.id, SyncStatus.SYNCED, envelope.syncVersion, envelope.updatedAt
                )
            }
            SyncEntityType.SETTINGS -> settingsDao.getByUuid(envelope.uuid)?.let { settings ->
                settingsDao.updateSyncState(settings.id, SyncStatus.SYNCED, envelope.syncVersion, envelope.updatedAt)
            }
            SyncEntityType.AUDIT_LOG -> auditLogDao.getByUuid(envelope.uuid)?.let { log ->
                auditLogDao.updateSyncState(log.uuid, SyncStatus.SYNCED, envelope.syncVersion, envelope.updatedAt)
            }
        }
    }

    override fun stopListening() {
        listeners.forEach { it.remove() }
        listeners.clear()
        activePharmacyId = null
    }

    override fun observeSyncStatus(): Flow<SyncStatusSnapshot> =
        combine(
            pendingOperationDao.observePendingCount(),
            licenseDao.observe().map { it?.licenseId },
        ) { pendingCount, licenseId ->
            SyncStatusSnapshot(
                pendingCount = pendingCount,
                isCloudEnabled = licenseId != null,
                lastSyncLabel = if (pendingCount == 0) "Offline Ready" else "Pending: $pendingCount",
                cloudStatusLabel = if (licenseId != null) "Connected" else "Not Connected",
            )
        }

    companion object {
        private const val LISTENER_LIMIT = 100L

        /** Phase 1–3 entities with realtime pull. */
        val SYNC_LISTENER_TYPES = listOf(
            SyncEntityType.CUSTOMER,
            SyncEntityType.SUPPLIER,
            SyncEntityType.PAYMENT,
            SyncEntityType.LEDGER,
            SyncEntityType.INVOICE,
            SyncEntityType.PURCHASE,
            SyncEntityType.MEDICINE,
            SyncEntityType.STOCK_BATCH,
            SyncEntityType.STOCK_ADJUSTMENT,
            SyncEntityType.SETTINGS,
            SyncEntityType.AUDIT_LOG,
        )
    }
}
