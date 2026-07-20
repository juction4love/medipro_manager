package com.medipro.manager.data.sync

import com.google.firebase.firestore.FirebaseFirestore
import com.medipro.manager.core.database.dao.LicenseDao
import com.medipro.manager.domain.repository.FirestoreSyncRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncCoordinator @Inject constructor(
    private val licenseDao: LicenseDao,
    private val firestoreSync: FirestoreSyncRepository,
) {
    suspend fun startIfLicensed() {
        val license = licenseDao.get() ?: return
        firestoreSync.startListening(license.licenseId, license.deviceId)
    }

    fun stop() {
        firestoreSync.stopListening()
    }
}

@Singleton
class FirestoreProvider @Inject constructor() {
    fun firestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}
