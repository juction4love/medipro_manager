package com.medipro.manager.data.repository

import com.medipro.manager.core.database.dao.MedicineDao
import com.medipro.manager.core.database.dao.OcrMedicineAliasDao
import com.medipro.manager.core.database.entity.OcrMedicineAliasEntity
import com.medipro.manager.data.purchasebill.OcrTextNormalizer
import com.medipro.manager.domain.model.OcrMedicineAlias
import com.medipro.manager.domain.repository.OcrMedicineAliasRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OcrMedicineAliasRepositoryImpl @Inject constructor(
    private val aliasDao: OcrMedicineAliasDao,
    private val medicineDao: MedicineDao,
) : OcrMedicineAliasRepository {

    override suspend fun findMedicineId(ocrDescription: String): Long? {
        val normalized = OcrTextNormalizer.normalize(ocrDescription)
        if (normalized.isBlank()) return null
        return aliasDao.findEnabledByNormalized(normalized)?.medicineId
    }

    override suspend fun saveMapping(
        ocrDescription: String,
        medicineId: Long,
        medicineName: String,
        medicineUuid: String?,
    ) {
        val normalized = OcrTextNormalizer.normalize(ocrDescription)
        if (normalized.isBlank()) return
        val existing = aliasDao.findByNormalized(normalized)
        val now = System.currentTimeMillis()
        aliasDao.upsert(
            OcrMedicineAliasEntity(
                id = existing?.id ?: 0,
                normalizedText = normalized,
                ocrText = ocrDescription.trim(),
                medicineId = medicineId,
                medicineUuid = medicineUuid,
                medicineName = medicineName.trim(),
                hitCount = existing?.hitCount ?: 0,
                isEnabled = true,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            ),
        )
    }

    override suspend fun recordHit(ocrDescription: String) {
        val normalized = OcrTextNormalizer.normalize(ocrDescription)
        aliasDao.findEnabledByNormalized(normalized)?.let { aliasDao.incrementHit(it.id) }
    }

    override fun observeAll(): Flow<List<OcrMedicineAlias>> =
        aliasDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun delete(id: Long) = aliasDao.deleteById(id)

    override suspend fun deleteAll() = aliasDao.deleteAll()

    override suspend fun setEnabled(id: Long, enabled: Boolean) = aliasDao.setEnabled(id, enabled)

    override suspend fun updateMapping(id: Long, ocrText: String, medicineId: Long, medicineName: String) {
        val existing = aliasDao.findById(id) ?: return
        val normalized = OcrTextNormalizer.normalize(ocrText)
        if (normalized.isBlank()) return
        val medicine = medicineDao.getById(medicineId)
        aliasDao.upsert(
            existing.copy(
                normalizedText = normalized,
                ocrText = ocrText.trim(),
                medicineId = medicineId,
                medicineUuid = medicine?.uuid,
                medicineName = medicineName.trim(),
                isEnabled = true,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    private fun OcrMedicineAliasEntity.toDomain() = OcrMedicineAlias(
        id = id,
        ocrText = ocrText,
        medicineId = medicineId,
        medicineName = medicineName,
        hitCount = hitCount,
        isEnabled = isEnabled,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
