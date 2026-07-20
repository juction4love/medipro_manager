package com.medipro.manager.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medipro.manager.BuildConfig
import com.medipro.manager.core.database.dao.PendingOperationDao
import com.medipro.manager.core.designsystem.navigation.DrawerBadges
import com.medipro.manager.core.designsystem.navigation.DrawerHeaderState
import com.medipro.manager.core.designsystem.navigation.DrawerUiState
import com.medipro.manager.domain.usecase.dashboard.ObserveDashboardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class NavigationDrawerViewModel @Inject constructor(
    observeDashboard: ObserveDashboardUseCase,
    pendingOperationDao: PendingOperationDao,
) : ViewModel() {

    val drawerState: StateFlow<DrawerUiState> = combine(
        observeDashboard(),
        pendingOperationDao.observePendingCount(),
    ) { snapshot, pendingSyncCount ->
        val stats = snapshot.stats
        val licenseActive = snapshot.licenseDaysRemaining?.let { it > 0 } ?: true
        val cloudSynced = pendingSyncCount == 0

        DrawerUiState(
            header = DrawerHeaderState(
                pharmacyName = snapshot.pharmacyName,
                licenseStatus = if (licenseActive) "✓ Active" else "Expired",
                cloudStatus = when {
                    pendingSyncCount > 0 -> "● Pending ($pendingSyncCount)"
                    snapshot.syncStatusLabel.contains("sync", ignoreCase = true) -> "✓ Synced"
                    else -> snapshot.syncStatusLabel
                },
                appVersion = "v${BuildConfig.VERSION_NAME}",
            ),
            badges = DrawerBadges(
                cloudSyncPending = pendingSyncCount,
                inventoryAlerts = stats.lowStockCount + stats.nearExpiryCount,
                customersDue = stats.pendingCustomerDueCount,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DrawerUiState(),
    )
}
