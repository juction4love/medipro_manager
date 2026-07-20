package com.medipro.manager.navigation

/**
 * MediPro navigation hierarchy (tablet / foldable / multi-window ready).
 *
 * ```
 * Root Graph
 * ├── Splash
 * ├── License (+ LicenseExpired)
 * ├── Activation (App Lock)
 * └── Main Graph
 *     ├── DashboardGraph  → Dashboard, Settings, Customer, …
 *     ├── SalesGraph      → Sales, History, Invoice, Return
 *     ├── PurchaseGraph   → Purchase, History, Invoice, Return
 *     ├── InventoryGraph  → Inventory, Medicine, Adjustment, Expiry
 *     ├── ReportsGraph    → Reports
 *     └── GlobalSearch    (overlay)
 * ```
 *
 * Each bottom-tab nested graph owns its back stack ([navigateBottomNavTab]).
 * Future: map each `*_GRAPH` to an independent pane on tablet/desktop.
 */
object NavigationGraph {
    const val ROOT = "root"

    val AUTH_ROUTES = setOf(
        Routes.SPLASH,
        Routes.LICENSE,
        Routes.LICENSE_EXPIRED,
        Routes.ACTIVATION,
    )

    val MAIN_TAB_GRAPHS = Routes.BOTTOM_NAV_GRAPHS
}
