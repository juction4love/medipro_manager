package com.medipro.manager

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.medipro.manager.core.worker.AutoBackupScheduler
import com.medipro.manager.data.seed.SeedDataImporter
import com.medipro.manager.data.sync.SyncCoordinator
import com.medipro.manager.domain.repository.SettingsRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class MediProApplication : Application(), Configuration.Provider {

    @Inject lateinit var seedDataImporter: SeedDataImporter
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var syncCoordinator: SyncCoordinator
    @Inject lateinit var autoBackupScheduler: AutoBackupScheduler
    @Inject lateinit var settingsRepository: SettingsRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        }
        appScope.launch {
            seedDataImporter.importIfNeeded()
            syncCoordinator.startIfLicensed()
            val settings = settingsRepository.getSettings()
            if (settings.autoBackupEnabled) {
                autoBackupScheduler.scheduleWeekly()
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
