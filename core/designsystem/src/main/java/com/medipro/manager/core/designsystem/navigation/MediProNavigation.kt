package com.medipro.manager.core.designsystem.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class BottomNavTab(val route: String, val label: String) {
    DASHBOARD("dashboard", "Dashboard"),
    SALES("sales", "Sales"),
    PURCHASE("purchase", "Purchase"),
    INVENTORY("inventory", "Inventory"),
    REPORTS("reports", "Reports"),
    ;

    companion object {
        fun fromRoute(route: String?): BottomNavTab? {
            val base = route?.substringBefore("/") ?: return null
            return entries.find { it.route == base }
        }
    }
}

private fun bottomNavIcon(tab: BottomNavTab, selected: Boolean): ImageVector = when (tab) {
    BottomNavTab.DASHBOARD -> if (selected) Icons.Filled.Dashboard else Icons.Outlined.Dashboard
    BottomNavTab.SALES -> if (selected) Icons.Filled.PointOfSale else Icons.Outlined.PointOfSale
    BottomNavTab.PURCHASE -> if (selected) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart
    BottomNavTab.INVENTORY -> if (selected) Icons.Filled.Inventory2 else Icons.Outlined.Inventory2
    BottomNavTab.REPORTS -> if (selected) Icons.Filled.Assessment else Icons.Outlined.Assessment
}

val MediProDrawerSections = listOf(
        DrawerSection(
        title = "Business",
        items = listOf(
            DrawerNavItem("customer", "Customers", Icons.Default.People, DrawerBadgeKey.CUSTOMERS),
            DrawerNavItem("supplier", "Suppliers", Icons.Default.LocalShipping),
            DrawerNavItem("accounting", "Accounting", Icons.Default.AccountBalance),
            DrawerNavItem("ledger", "Ledger", Icons.Default.ReceiptLong),
        ),
    ),
    DrawerSection(
        title = "Inventory",
        items = listOf(
            DrawerNavItem("medicine", "Master Catalog", Icons.Default.Inventory2, DrawerBadgeKey.INVENTORY),
            DrawerNavItem("cloud_sync", "Cloud Sync", Icons.Default.CloudSync, DrawerBadgeKey.CLOUD_SYNC),
            DrawerNavItem("backup", "Backup & Restore", Icons.Default.Backup),
        ),
    ),
    DrawerSection(
        title = "System",
        items = listOf(
            DrawerNavItem("thermal_printer", "Thermal Printer", Icons.Default.Print),
            DrawerNavItem("license_info", "License", Icons.Default.VerifiedUser),
            DrawerNavItem("settings", "Settings", Icons.Default.Settings),
        ),
    ),
    DrawerSection(
        title = "Help",
        items = listOf(
            DrawerNavItem("help", "Help", Icons.Default.HelpOutline),
            DrawerNavItem("about", "About", Icons.Default.Info),
        ),
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediProBottomBar(
    selected: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    badges: DrawerBadges = DrawerBadges(),
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        BottomNavTab.entries.forEach { tab ->
            val isSelected = selected == tab
            val inventoryBadge = if (tab == BottomNavTab.INVENTORY) {
                badges.inventoryAlerts.takeIf { it > 0 }
            } else {
                null
            }
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text(tab.label) } },
                        state = rememberTooltipState(),
                    ) {
                        if (inventoryBadge != null) {
                            BadgedBox(
                                badge = {
                                    Badge {
                                        Text(
                                            if (inventoryBadge > 99) "99+" else inventoryBadge.toString(),
                                        )
                                    }
                                },
                            ) {
                                Icon(
                                    bottomNavIcon(tab, isSelected),
                                    contentDescription = tab.label,
                                )
                            }
                        } else {
                            Icon(
                                bottomNavIcon(tab, isSelected),
                                contentDescription = tab.label,
                            )
                        }
                    }
                },
                label = {},
                alwaysShowLabel = false,
            )
        }
    }
}

@Composable
private fun MediProDrawerHeader(header: DrawerHeaderState) {
    Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp)) {
        Text(
            text = "MediPro ERP",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = header.pharmacyName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "License  ${header.licenseStatus}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Cloud  ${header.cloudStatus}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = header.appVersion,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun DrawerItemIcon(
    item: DrawerNavItem,
    badges: DrawerBadges,
) {
    val count = badgeCountFor(item.badgeKey, badges)
    if (count != null) {
        BadgedBox(
            badge = {
                Badge {
                    Text(if (count > 99) "99+" else count.toString())
                }
            },
        ) {
            Icon(item.icon, contentDescription = item.label)
        }
    } else {
        Icon(item.icon, contentDescription = item.label)
    }
}

@Composable
private fun DrawerSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
    )
}

@Composable
fun MediProNavigationDrawerContent(
    currentRoute: String?,
    drawerState: DrawerUiState,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalDrawerSheet(modifier = modifier) {
        MediProDrawerHeader(header = drawerState.header)
        HorizontalDivider()
        Spacer(modifier = Modifier.height(4.dp))

        MediProDrawerSections.forEachIndexed { index, section ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(4.dp))
            }
            DrawerSectionHeader(section.title)
            section.items.forEach { item ->
                NavigationDrawerItem(
                    label = { Text(item.label) },
                    selected = currentRoute?.substringBefore("/") == item.route,
                    onClick = { onNavigate(item.route) },
                    icon = { DrawerItemIcon(item, drawerState.badges) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        NavigationDrawerItem(
            label = { Text("Logout") },
            selected = false,
            onClick = onLogout,
            icon = { Icon(Icons.Default.Logout, contentDescription = "Logout") },
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun MediProGlobalSearchIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(Icons.Default.Search, contentDescription = "Global search")
    }
}

@Composable
fun MediProTopBarNavigationIcon(
    onBack: (() -> Unit)?,
    onOpenDrawer: (() -> Unit)?,
) {
    when {
        onOpenDrawer != null -> {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Default.Menu, contentDescription = "Open menu")
            }
        }
        onBack != null -> {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }
    }
}
