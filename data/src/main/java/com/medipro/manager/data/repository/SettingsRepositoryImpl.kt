package com.medipro.manager.data.repository

import com.medipro.manager.core.database.dao.LicenseDao
import com.medipro.manager.core.database.dao.SettingsDao
import com.medipro.manager.core.database.entity.SettingsEntity
import com.medipro.manager.core.database.entity.SyncStatus
import com.medipro.manager.data.mapper.toDomain
import com.medipro.manager.data.mapper.toEntity
import com.medipro.manager.data.sync.SyncEnqueueHelper
import com.medipro.manager.data.sync.pharmacyUuid
import com.medipro.manager.domain.model.PharmacySettings
import com.medipro.manager.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao,
    private val licenseDao: LicenseDao,
    private val syncEnqueueHelper: SyncEnqueueHelper,
) : SettingsRepository {

    override fun observeSettings(): Flow<PharmacySettings> =
        settingsDao.observe().map { it?.toDomain() ?: PharmacySettings() }

    override suspend fun getSettings(): PharmacySettings =
        settingsDao.get()?.toDomain() ?: PharmacySettings()

    override suspend fun updateSettings(settings: PharmacySettings) {
        val existing = settingsDao.get()
        val now = System.currentTimeMillis()
        val license = licenseDao.get()
        val entity = settings.toEntity().copy(
            id = 1,
            uuid = existing?.uuid ?: SettingsEntity.SETTINGS_UUID,
            pharmacyUuid = licenseDao.pharmacyUuid(),
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING,
            syncVersion = (existing?.syncVersion ?: 0) + 1,
            deviceId = license?.deviceId,
        )
        if (existing == null) {
            settingsDao.insert(entity)
        } else {
            settingsDao.update(entity)
        }
        syncEnqueueHelper.enqueueSettings(entity)
    }
}
