package com.medipro.manager.data.repository

import com.medipro.manager.core.database.dao.MedicineDao
import com.medipro.manager.core.database.dao.StockDao
import com.medipro.manager.core.database.entity.MedicineEntity
import com.medipro.manager.core.database.entity.AuditEventType
import com.medipro.manager.core.database.entity.SyncStatus
import com.medipro.manager.core.search.SearchQueryEnhancer
import com.medipro.manager.data.mapper.toDomain
import com.medipro.manager.data.mapper.toEntity
import com.medipro.manager.data.sync.SyncEnqueueHelper
import com.medipro.manager.domain.model.AuditLog
import com.medipro.manager.domain.model.Medicine
import com.medipro.manager.domain.repository.AuditRepository
import com.medipro.manager.domain.repository.MedicineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicineRepositoryImpl @Inject constructor(
    private val medicineDao: MedicineDao,
    private val stockDao: StockDao,
    private val auditRepository: AuditRepository,
    private val syncEnqueueHelper: SyncEnqueueHelper,
) : MedicineRepository {

    override fun observeMedicines(): Flow<List<Medicine>> =
        medicineDao.observeAll().map { entities ->
            entities.map { entity ->
                val qty = stockDao.getTotalQuantity(entity.id) ?: 0
                entity.toDomain(qty)
            }
        }

    override fun searchMedicines(query: String): Flow<List<Medicine>> {
        val trimmed = SearchQueryEnhancer.normalize(query)
        if (trimmed.isBlank()) return flowOf(emptyList())

        val ftsQueries = SearchQueryEnhancer.buildFtsQueries(trimmed)
        if (ftsQueries.isEmpty()) {
            return medicineDao.search(trimmed).map { entities ->
                entities.map { entity ->
                    val qty = stockDao.getTotalQuantity(entity.id) ?: 0
                    entity.toDomain(qty)
                }
            }
        }

        return flow {
            val ranked = linkedMapOf<Long, MedicineEntity>()
            ftsQueries.forEach { (ftsQuery, _) ->
                medicineDao.searchFts(ftsQuery).first().forEach { entity ->
                    ranked.putIfAbsent(entity.id, entity)
                }
            }
            if (ranked.size < 20) {
                medicineDao.search(trimmed).first().forEach { entity ->
                    ranked.putIfAbsent(entity.id, entity)
                }
            }
            emit(
                ranked.values.map { entity ->
                    val qty = stockDao.getTotalQuantity(entity.id) ?: 0
                    entity.toDomain(qty)
                },
            )
        }
    }

    override suspend fun getMedicineById(id: Long): Medicine? {
        val entity = medicineDao.getById(id) ?: return null
        val qty = stockDao.getTotalQuantity(id) ?: 0
        return entity.toDomain(qty)
    }

    override suspend fun getMedicineByBarcode(barcode: String): Medicine? {
        val entity = medicineDao.getByBarcode(barcode) ?: return null
        val qty = stockDao.getTotalQuantity(entity.id) ?: 0
        return entity.toDomain(qty)
    }

    override suspend fun addMedicine(medicine: Medicine): Long {
        val id = medicineDao.insert(medicine.toEntity())
        val saved = medicineDao.getById(id)
        auditRepository.log(
            AuditLog(
                uuid = UUID.randomUUID().toString(),
                eventType = AuditEventType.MEDICINE_CREATED,
                entityType = "MEDICINE",
                entityUuid = saved?.uuid,
                description = "Medicine created: ${medicine.brandName}"
            )
        )
        saved?.let { syncEnqueueHelper.enqueueMedicine(it) }
        return id
    }

    override suspend fun updateMedicine(medicine: Medicine) {
        val existing = medicineDao.getById(medicine.id)
        medicineDao.update(
            medicine.toEntity().copy(
                updatedAt = System.currentTimeMillis(),
                syncStatus = "PENDING",
                syncVersion = (existing?.syncVersion ?: 0) + 1
            )
        )
        if (existing != null && existing.sellingPricePaisa != medicine.toEntity().sellingPricePaisa) {
            auditRepository.log(
                AuditLog(
                    uuid = UUID.randomUUID().toString(),
                    eventType = AuditEventType.PRICE_CHANGED,
                    entityType = "MEDICINE",
                    entityUuid = existing.uuid,
                    description = "Price changed: ${medicine.brandName}",
                    oldValue = (existing.sellingPricePaisa / 100.0).toString(),
                    newValue = medicine.sellingPrice.toString()
                )
            )
        }
        auditRepository.log(
            AuditLog(
                uuid = UUID.randomUUID().toString(),
                eventType = AuditEventType.MEDICINE_UPDATED,
                entityType = "MEDICINE",
                entityUuid = existing?.uuid,
                description = "Medicine updated: ${medicine.brandName}"
            )
        )
        medicineDao.getById(medicine.id)?.let { syncEnqueueHelper.enqueueMedicine(it) }
    }

    override suspend fun deleteMedicine(id: Long) {
        val existing = medicineDao.getById(id)
        val deletedAt = System.currentTimeMillis()
        medicineDao.softDelete(id, deletedAt)
        auditRepository.log(
            AuditLog(
                uuid = UUID.randomUUID().toString(),
                eventType = AuditEventType.MEDICINE_DELETED,
                entityType = "MEDICINE",
                entityUuid = existing?.uuid,
                description = "Medicine deleted: ${existing?.brandName.orEmpty()}"
            )
        )
        existing?.let { entity ->
            syncEnqueueHelper.enqueueMedicine(
                entity.copy(
                    isActive = false,
                    deletedAt = deletedAt,
                    updatedAt = deletedAt,
                    syncStatus = SyncStatus.DELETED,
                    syncVersion = entity.syncVersion + 1,
                )
            )
        }
    }
}
