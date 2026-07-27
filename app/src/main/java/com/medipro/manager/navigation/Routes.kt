package com.medipro.manager.navigation

import com.medipro.manager.core.designsystem.navigation.BottomNavTab

object Routes {
    // ── Root graph (auth flow) ──────────────────────────────────────────────
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val LICENSE = "license"
    /** In-app license activation (subscribe / import). */
    const val LICENSE_ACTIVATION = LICENSE
    const val LICENSE_EXPIRED = "license_expired"
    const val SUBSCRIPTION = "subscription"
    /** App lock / PIN — "Activation" in product navigation docs. */
    const val APP_LOCK = "app_lock"
    const val ACTIVATION = APP_LOCK

    // ── Main graph (authenticated shell) ───────────────────────────────────
    const val MAIN = "main"

    /** Bottom-tab nested graphs — one independent back stack each. */
    const val DASHBOARD_GRAPH = "dashboard_graph"
    const val SALES_GRAPH = "sales_graph"
    const val PURCHASE_GRAPH = "purchase_graph"
    const val INVENTORY_GRAPH = "inventory_graph"
    const val REPORTS_GRAPH = "reports_graph"

    // ── Tab roots ───────────────────────────────────────────────────────────
    const val DASHBOARD = "dashboard"
    const val MEDICINE = "medicine"
    const val SUPPLIER = "supplier"
    const val CUSTOMER = "customer"
    const val PURCHASE = "purchase"
    const val PURCHASE_HISTORY = "purchase_history"
    const val PURCHASE_INVOICE = "purchase_invoice/{invoiceNumber}"
    const val PURCHASE_RETURN = "purchase_return/{purchaseId}"
    const val SALES = "sales"
    const val SALES_HISTORY = "sales_history"
    const val SALES_INVOICE = "sales_invoice/{invoiceNumber}"
    const val SALE_RETURN = "sale_return/{saleId}"
    const val INVENTORY = "inventory"
    const val INVENTORY_ADJUSTMENT = "inventory_adjustment/{batchId}?type={type}"
    const val EXPIRY = "expiry"
    const val REPORTS = "reports"
    const val ACCOUNTING = "accounting"
    const val ACCOUNTING_CUSTOMER_RECEIPTS = "accounting/customer_receipts"
    const val ACCOUNTING_SUPPLIER_PAYMENTS = "accounting/supplier_payments"
    const val ACCOUNTING_CASH_BOOK = "accounting/cash_book"
    const val ACCOUNTING_EXPENSES = "accounting/expenses"
    const val ACCOUNTING_DAY_CLOSING = "accounting/day_closing"
    const val PAYMENTS = "payments"
    const val LEDGER = "ledger"
    const val BACKUP = "backup"
    const val CLOUD_SYNC = "cloud_sync"
    const val THERMAL_PRINTER = "thermal_printer"
    const val LICENSE_INFO = "license_info"
    const val HELP = "help"
    const val ABOUT = "about"
    const val APP_INFO = "app_info"
    const val SETTINGS = "settings"
    const val OCR_LEARNING = "ocr_learning"
    const val PROFILE = "profile"
    const val NOTIFICATION = "notification"
    const val GLOBAL_SEARCH = "global_search"
    const val SCANNER = "scanner"

    val BOTTOM_NAV_ROUTES = setOf(
        DASHBOARD,
        SALES,
        PURCHASE,
        INVENTORY,
        REPORTS,
    )

    val BOTTOM_NAV_GRAPHS = setOf(
        DASHBOARD_GRAPH,
        SALES_GRAPH,
        PURCHASE_GRAPH,
        INVENTORY_GRAPH,
        REPORTS_GRAPH,
    )

    private val DASHBOARD_STACK = setOf(
        DASHBOARD,
        NOTIFICATION,
        CUSTOMER,
        SUPPLIER,
        ACCOUNTING,
        ACCOUNTING_CUSTOMER_RECEIPTS,
        ACCOUNTING_SUPPLIER_PAYMENTS,
        ACCOUNTING_CASH_BOOK,
        ACCOUNTING_EXPENSES,
        ACCOUNTING_DAY_CLOSING,
        PAYMENTS,
        LEDGER,
        BACKUP,
        CLOUD_SYNC,
        THERMAL_PRINTER,
        LICENSE_INFO,
        SETTINGS,
        OCR_LEARNING,
        HELP,
        ABOUT,
        APP_INFO,
        PROFILE,
        SCANNER,
    )

    private val SALES_STACK = setOf(
        SALES,
        SALES_HISTORY,
    )

    private val SALES_INVOICE_STACK_PREFIX = "sales_invoice"

    private val PURCHASE_STACK = setOf(
        PURCHASE,
        PURCHASE_HISTORY,
    )

    private val PURCHASE_INVOICE_STACK_PREFIX = "purchase_invoice"

    private val INVENTORY_STACK = setOf(
        INVENTORY,
        MEDICINE,
        EXPIRY,
    )

    private val REPORTS_STACK = setOf(
        REPORTS,
    )

    val DRAWER_ROUTES = setOf(
        CUSTOMER,
        SUPPLIER,
        ACCOUNTING,
        ACCOUNTING_CUSTOMER_RECEIPTS,
        ACCOUNTING_SUPPLIER_PAYMENTS,
        ACCOUNTING_CASH_BOOK,
        ACCOUNTING_EXPENSES,
        ACCOUNTING_DAY_CLOSING,
        PAYMENTS,
        LEDGER,
        MEDICINE,
        CLOUD_SYNC,
        BACKUP,
        THERMAL_PRINTER,
        LICENSE_INFO,
        SETTINGS,
        HELP,
        ABOUT,
        APP_INFO,
        ACCOUNTING,
        EXPIRY,
        PROFILE,
        NOTIFICATION,
    )

    val AUTHENTICATED_ROUTES = BOTTOM_NAV_ROUTES + DRAWER_ROUTES + setOf(
        GLOBAL_SEARCH,
        PURCHASE_HISTORY,
        SALES_HISTORY,
        SCANNER,
    )

    fun tabGraphRoute(tabRoute: String): String = when (tabRoute) {
        DASHBOARD -> DASHBOARD_GRAPH
        SALES -> SALES_GRAPH
        PURCHASE -> PURCHASE_GRAPH
        INVENTORY -> INVENTORY_GRAPH
        REPORTS -> REPORTS_GRAPH
        else -> tabRoute
    }

    fun bottomNavTabForRoute(route: String?): BottomNavTab? {
        val base = route?.substringBefore("/") ?: return null
        return when {
            base in DASHBOARD_STACK || base == DASHBOARD_GRAPH -> BottomNavTab.DASHBOARD
            base in SALES_STACK || base.startsWith("sale_return") ||
                base.startsWith(SALES_INVOICE_STACK_PREFIX) || base == SALES_GRAPH -> BottomNavTab.SALES
            base in PURCHASE_STACK || base.startsWith("purchase_return") ||
                base.startsWith(PURCHASE_INVOICE_STACK_PREFIX) || base == PURCHASE_GRAPH -> BottomNavTab.PURCHASE
            base in INVENTORY_STACK || base.startsWith("inventory_adjustment") || base == INVENTORY_GRAPH -> {
                BottomNavTab.INVENTORY
            }
            base in REPORTS_STACK || base == REPORTS_GRAPH -> BottomNavTab.REPORTS
            else -> null
        }
    }

    fun isBottomTabRoot(route: String?): Boolean {
        val base = route?.substringBefore("/") ?: return false
        return base in BOTTOM_NAV_ROUTES
    }

    fun isMainAppRoute(route: String?): Boolean =
        route == GLOBAL_SEARCH || showsMainChrome(route)

    fun showsBottomBar(route: String?): Boolean = bottomNavTabForRoute(route) != null

    fun showsMainChrome(route: String?): Boolean {
        val base = route?.substringBefore("/") ?: return false
        if (base == GLOBAL_SEARCH) return false
        return bottomNavTabForRoute(route) != null ||
            base.startsWith("purchase_return") ||
            base.startsWith("sale_return") ||
            base.startsWith("inventory_adjustment") ||
            base.startsWith(SALES_INVOICE_STACK_PREFIX) ||
            base.startsWith(PURCHASE_INVOICE_STACK_PREFIX)
    }
}
