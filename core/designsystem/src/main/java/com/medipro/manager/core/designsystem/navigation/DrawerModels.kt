package com.medipro.manager.core.designsystem.navigation

import androidx.compose.ui.graphics.vector.ImageVector

data class DrawerHeaderState(
    val pharmacyName: String = "My Pharmacy",
    val licenseStatus: String = "✓ Active",
    val cloudStatus: String = "✓ Synced",
    val appVersion: String = "v1.0",
)

data class DrawerBadges(
    val cloudSyncPending: Int = 0,
    val inventoryAlerts: Int = 0,
    val customersDue: Int = 0,
)

data class DrawerNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val badgeKey: DrawerBadgeKey? = null,
)

enum class DrawerBadgeKey {
    CLOUD_SYNC,
    INVENTORY,
    CUSTOMERS,
}

data class DrawerSection(
    val title: String,
    val items: List<DrawerNavItem>,
)

data class DrawerUiState(
    val header: DrawerHeaderState = DrawerHeaderState(),
    val badges: DrawerBadges = DrawerBadges(),
)

fun badgeCountFor(key: DrawerBadgeKey?, badges: DrawerBadges): Int? = when (key) {
    DrawerBadgeKey.CLOUD_SYNC -> badges.cloudSyncPending.takeIf { it > 0 }
    DrawerBadgeKey.INVENTORY -> badges.inventoryAlerts.takeIf { it > 0 }
    DrawerBadgeKey.CUSTOMERS -> badges.customersDue.takeIf { it > 0 }
    null -> null
}
