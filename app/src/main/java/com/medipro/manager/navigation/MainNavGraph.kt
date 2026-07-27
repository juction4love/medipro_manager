package com.medipro.manager.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import com.medipro.manager.BuildConfig
import com.medipro.manager.core.designsystem.component.FeaturePlaceholderScreen
import com.medipro.manager.domain.licensing.PremiumFeature
import com.medipro.manager.feature.license.presentation.PremiumGatedScreen
import com.medipro.manager.feature.license.presentation.SubscriptionScreen
import com.medipro.manager.domain.model.GlobalSearchCategory
import com.medipro.manager.feature.accounting.presentation.AccountingPlaceholderScreen
import com.medipro.manager.feature.accounting.presentation.AccountingScreen
import com.medipro.manager.feature.accounting.presentation.CashBookScreen
import com.medipro.manager.feature.accounting.presentation.CustomerReceiptsScreen
import com.medipro.manager.feature.accounting.presentation.DayClosingScreen
import com.medipro.manager.feature.accounting.presentation.ExpensesScreen
import com.medipro.manager.feature.accounting.presentation.SupplierPaymentsScreen
import com.medipro.manager.feature.backup.presentation.BackupScreen
import com.medipro.manager.feature.customer.presentation.CustomerScreen
import com.medipro.manager.feature.dashboard.presentation.DashboardScreen
import com.medipro.manager.feature.expiry.presentation.ExpiryScreen
import com.medipro.manager.feature.globalsearch.presentation.GlobalSearchScreen
import com.medipro.manager.feature.inventory.presentation.InventoryScreen
import com.medipro.manager.feature.inventory.presentation.StockAdjustmentScreen
import com.medipro.manager.feature.medicine.presentation.MedicineScreen
import com.medipro.manager.feature.notification.presentation.NotificationScreen
import com.medipro.manager.feature.profile.presentation.ProfileScreen
import com.medipro.manager.feature.purchase.presentation.PurchaseHistoryScreen
import com.medipro.manager.feature.purchase.presentation.PurchaseInvoiceScreen
import com.medipro.manager.feature.purchase.presentation.PurchaseReturnScreen
import com.medipro.manager.feature.purchase.presentation.PurchaseScreen
import com.medipro.manager.feature.reports.presentation.ReportsScreen
import com.medipro.manager.feature.sales.presentation.SaleHistoryScreen
import com.medipro.manager.feature.sales.presentation.SaleInvoiceScreen
import com.medipro.manager.feature.sales.presentation.SaleReturnScreen
import com.medipro.manager.feature.sales.presentation.SalesScreen
import com.medipro.manager.feature.scanner.presentation.ScannerScreen
import com.medipro.manager.feature.settings.presentation.AboutScreen
import com.medipro.manager.feature.settings.presentation.AppInformationScreen
import com.medipro.manager.feature.settings.presentation.HelpScreen
import com.medipro.manager.feature.settings.presentation.OcrLearningScreen
import com.medipro.manager.feature.settings.presentation.PrinterSettingsScreen
import com.medipro.manager.feature.settings.presentation.SettingsScreen
import com.medipro.manager.feature.supplier.presentation.SupplierScreen

/** Main graph: five tab nested graphs + global search overlay. */
fun NavGraphBuilder.mainNavGraph(navController: NavHostController) {
    navigation(
        route = Routes.MAIN,
        startDestination = Routes.DASHBOARD_GRAPH,
    ) {
        dashboardGraph(navController)
        salesGraph(navController)
        purchaseGraph(navController)
        inventoryGraph(navController)
        reportsGraph(navController)

        composable(Routes.GLOBAL_SEARCH) {
            GlobalSearchScreen(
                onBack = { navController.popBackStack() },
                onResultClick = { result ->
                    navController.popBackStack()
                    when (result.category) {
                        GlobalSearchCategory.MEDICINE -> {
                            navController.navigateBottomNavTab(Routes.INVENTORY)
                            navController.navigateInApp(Routes.MEDICINE)
                        }
                        GlobalSearchCategory.CUSTOMER -> navController.navigateInApp(Routes.CUSTOMER)
                        GlobalSearchCategory.SUPPLIER -> navController.navigateInApp(Routes.SUPPLIER)
                        GlobalSearchCategory.SALE_INVOICE -> {
                            navController.navigateBottomNavTab(Routes.SALES)
                            navController.navigateInApp(Routes.SALES_HISTORY)
                        }
                        GlobalSearchCategory.PURCHASE_INVOICE -> {
                            navController.navigateBottomNavTab(Routes.PURCHASE)
                            navController.navigateInApp(Routes.PURCHASE_HISTORY)
                        }
                    }
                },
            )
        }
    }
}

private fun NavGraphBuilder.dashboardGraph(navController: NavHostController) {
    navigation(
        route = Routes.DASHBOARD_GRAPH,
        startDestination = Routes.DASHBOARD,
    ) {
        composable(
            route = Routes.DASHBOARD,
            deepLinks = listOf(navDeepLink { uriPattern = DeepLinks.DASHBOARD_URI }),
        ) {
            DashboardScreen(
                onNavigate = { route ->
                    if (route in Routes.BOTTOM_NAV_ROUTES) {
                        navController.navigateBottomNavTab(route)
                    } else {
                        navController.navigateInApp(route)
                    }
                },
                onOpenDrawer = LocalDrawerOpener.current,
                onOpenGlobalSearch = LocalGlobalSearchOpener.current,
                onOpenSubscription = { navController.navigateInApp(Routes.SUBSCRIPTION) },
            )
        }
        composable(Routes.NOTIFICATION) {
            NotificationScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.CUSTOMER) {
            CustomerScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SUPPLIER) {
            SupplierScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ACCOUNTING) {
            AccountingScreen(
                onBack = { navController.popBackStack() },
                onOpenDrawer = LocalDrawerOpener.current,
                onNavigate = { subRoute ->
                    val target = when (subRoute) {
                        "customer_receipts" -> Routes.ACCOUNTING_CUSTOMER_RECEIPTS
                        "supplier_payments" -> Routes.ACCOUNTING_SUPPLIER_PAYMENTS
                        "cash_book" -> Routes.ACCOUNTING_CASH_BOOK
                        "expenses" -> Routes.ACCOUNTING_EXPENSES
                        "ledger" -> Routes.LEDGER
                        "day_closing" -> Routes.ACCOUNTING_DAY_CLOSING
                        else -> return@AccountingScreen
                    }
                    navController.navigateInApp(target)
                },
            )
        }
        composable(Routes.ACCOUNTING_CUSTOMER_RECEIPTS) {
            CustomerReceiptsScreen(
                onBack = { navController.popBackStack() },
                onOpenDrawer = LocalDrawerOpener.current,
            )
        }
        composable(Routes.ACCOUNTING_SUPPLIER_PAYMENTS) {
            SupplierPaymentsScreen(
                onBack = { navController.popBackStack() },
                onOpenDrawer = LocalDrawerOpener.current,
            )
        }
        composable(Routes.ACCOUNTING_CASH_BOOK) {
            CashBookScreen(
                onBack = { navController.popBackStack() },
                onOpenDrawer = LocalDrawerOpener.current,
            )
        }
        composable(Routes.ACCOUNTING_EXPENSES) {
            ExpensesScreen(
                onBack = { navController.popBackStack() },
                onOpenDrawer = LocalDrawerOpener.current,
            )
        }
        composable(Routes.ACCOUNTING_DAY_CLOSING) {
            DayClosingScreen(
                onBack = { navController.popBackStack() },
                onOpenDrawer = LocalDrawerOpener.current,
            )
        }
        composable(Routes.PAYMENTS) {
            AccountingScreen(
                onBack = { navController.popBackStack() },
                onOpenDrawer = LocalDrawerOpener.current,
                onNavigate = { subRoute ->
                    val target = when (subRoute) {
                        "customer_receipts" -> Routes.ACCOUNTING_CUSTOMER_RECEIPTS
                        "supplier_payments" -> Routes.ACCOUNTING_SUPPLIER_PAYMENTS
                        "cash_book" -> Routes.ACCOUNTING_CASH_BOOK
                        "expenses" -> Routes.ACCOUNTING_EXPENSES
                        "ledger" -> Routes.LEDGER
                        "day_closing" -> Routes.ACCOUNTING_DAY_CLOSING
                        else -> return@AccountingScreen
                    }
                    navController.navigateInApp(target)
                },
            )
        }
        composable(Routes.LEDGER) {
            FeaturePlaceholderScreen(
                title = "Ledger",
                description = "General ledger, cash book, and account balances.",
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.BACKUP) {
            PremiumGatedScreen(
                feature = PremiumFeature.BACKUP_RESTORE,
                onRequireSubscription = { navController.navigateInApp(Routes.SUBSCRIPTION) },
            ) {
                BackupScreen(onBack = { navController.popBackStack() })
            }
        }
        composable(Routes.CLOUD_SYNC) {
            PremiumGatedScreen(
                feature = PremiumFeature.CLOUD_SYNC,
                onRequireSubscription = { navController.navigateInApp(Routes.SUBSCRIPTION) },
            ) {
                FeaturePlaceholderScreen(
                    title = "Cloud Sync",
                    description = "Sync pharmacy data with Firestore when online.",
                    onBack = { navController.popBackStack() },
                )
            }
        }
        composable(Routes.THERMAL_PRINTER) {
            PrinterSettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenDrawer = LocalDrawerOpener.current,
            )
        }
        composable(Routes.LICENSE_INFO) {
            SubscriptionScreen(
                appVersion = BuildConfig.VERSION_NAME,
                onBack = { navController.popBackStack() },
                onSubscribe = { navController.navigateInApp(Routes.LICENSE) },
                onImportLicense = { navController.navigateInApp(Routes.LICENSE) },
            )
        }
        composable(Routes.HELP) {
            HelpScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.APP_INFO) {
            AppInformationScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenBackup = { navController.navigateInApp(Routes.BACKUP) },
                onOpenPrinter = { navController.navigateInApp(Routes.THERMAL_PRINTER) },
                onOpenOcrLearning = { navController.navigateInApp(Routes.OCR_LEARNING) },
                onOpenLicense = { navController.navigateInApp(Routes.LICENSE_INFO) },
                onOpenAbout = { navController.navigateInApp(Routes.ABOUT) },
                onOpenAppInfo = { navController.navigateInApp(Routes.APP_INFO) },
                onOpenHelp = { navController.navigateInApp(Routes.HELP) },
            )
        }
        composable(Routes.OCR_LEARNING) {
            PremiumGatedScreen(
                feature = PremiumFeature.OCR_LEARNING,
                onRequireSubscription = { navController.navigateInApp(Routes.SUBSCRIPTION) },
            ) {
                OcrLearningScreen(onBack = { navController.popBackStack() })
            }
        }
        composable(Routes.PROFILE) {
            ProfileScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SCANNER) {
            ScannerScreen(onBack = { navController.popBackStack() })
        }
    }
}

private fun NavGraphBuilder.salesGraph(navController: NavHostController) {
    navigation(
        route = Routes.SALES_GRAPH,
        startDestination = Routes.SALES,
    ) {
        composable(
            route = Routes.SALES,
            deepLinks = listOf(navDeepLink { uriPattern = DeepLinks.SALES_URI }),
        ) {
            SalesScreen(
                onBack = { navController.popBackStack() },
                onOpenDrawer = LocalDrawerOpener.current,
                onOpenGlobalSearch = LocalGlobalSearchOpener.current,
                onOpenHistory = { navController.navigateInApp(Routes.SALES_HISTORY) },
                onOpenReturn = { saleId -> navController.navigateInApp("sale_return/$saleId") },
            )
        }
        composable(
            route = Routes.SALES_INVOICE,
            deepLinks = listOf(navDeepLink { uriPattern = DeepLinks.INVOICE_PATTERN }),
            arguments = listOf(navArgument("invoiceNumber") { type = NavType.StringType }),
        ) { entry ->
            val invoiceNumber = entry.arguments?.getString("invoiceNumber").orEmpty()
            SaleInvoiceScreen(
                invoiceNumber = invoiceNumber,
                onBack = { navController.popBackStack() },
                onReturnSale = { saleId -> navController.navigateInApp("sale_return/$saleId") },
            )
        }
        composable(Routes.SALES_HISTORY) {
            SaleHistoryScreen(
                onBack = { navController.popBackStack() },
                onReturnSale = { saleId ->
                    navController.navigateInApp("sale_return/$saleId")
                },
                onOpenInvoice = { invoiceNumber ->
                    navController.navigateInApp("sales_invoice/$invoiceNumber")
                },
            )
        }
        composable(
            route = Routes.SALE_RETURN,
            arguments = listOf(navArgument("saleId") { type = NavType.LongType }),
        ) {
            SaleReturnScreen(
                onBack = { navController.popBackStack() },
                onReturnCompleted = {
                    navController.popBackStack(Routes.SALES, inclusive = false)
                },
            )
        }
    }
}

private fun NavGraphBuilder.purchaseGraph(navController: NavHostController) {
    navigation(
        route = Routes.PURCHASE_GRAPH,
        startDestination = Routes.PURCHASE,
    ) {
        composable(
            route = Routes.PURCHASE,
            deepLinks = listOf(navDeepLink { uriPattern = "medipro://purchase" }),
        ) {
            PurchaseScreen(
                onBack = { navController.popBackStack() },
                onOpenDrawer = LocalDrawerOpener.current,
                onOpenGlobalSearch = LocalGlobalSearchOpener.current,
                onOpenHistory = { navController.navigateInApp(Routes.PURCHASE_HISTORY) },
                onOpenPurchaseInvoice = { invoiceNumber ->
                    navController.navigateInApp("purchase_invoice/$invoiceNumber")
                },
                onRequireSubscription = { navController.navigateInApp(Routes.SUBSCRIPTION) },
            )
        }
        composable(
            route = Routes.PURCHASE_INVOICE,
            deepLinks = listOf(navDeepLink { uriPattern = DeepLinks.PURCHASE_INVOICE_PATTERN }),
            arguments = listOf(navArgument("invoiceNumber") { type = NavType.StringType }),
        ) { entry ->
            val invoiceNumber = entry.arguments?.getString("invoiceNumber").orEmpty()
            PurchaseInvoiceScreen(
                invoiceNumber = invoiceNumber,
                onBack = { navController.popBackStack() },
                onReturnPurchase = { purchaseId ->
                    navController.navigateInApp("purchase_return/$purchaseId")
                },
            )
        }
        composable(Routes.PURCHASE_HISTORY) {
            PurchaseHistoryScreen(
                onBack = { navController.popBackStack() },
                onReturnPurchase = { purchaseId ->
                    navController.navigateInApp("purchase_return/$purchaseId")
                },
            )
        }
        composable(
            route = Routes.PURCHASE_RETURN,
            arguments = listOf(navArgument("purchaseId") { type = NavType.LongType }),
        ) {
            PurchaseReturnScreen(
                onBack = { navController.popBackStack() },
                onReturnCompleted = {
                    navController.popBackStack(Routes.PURCHASE_HISTORY, inclusive = false)
                },
            )
        }
    }
}

private fun NavGraphBuilder.inventoryGraph(navController: NavHostController) {
    navigation(
        route = Routes.INVENTORY_GRAPH,
        startDestination = Routes.INVENTORY,
    ) {
        composable(
            route = Routes.INVENTORY,
            deepLinks = listOf(navDeepLink { uriPattern = DeepLinks.INVENTORY_URI }),
        ) {
            InventoryScreen(
                onBack = { navController.popBackStack() },
                onOpenDrawer = LocalDrawerOpener.current,
                onOpenGlobalSearch = LocalGlobalSearchOpener.current,
                onOpenAdjustment = { batchId, type ->
                    val typeArg = type ?: ""
                    navController.navigateInApp("inventory_adjustment/$batchId?type=$typeArg")
                },
            )
        }
        composable(
            route = Routes.INVENTORY_ADJUSTMENT,
            arguments = listOf(
                navArgument("batchId") { type = NavType.LongType },
                navArgument("type") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) {
            StockAdjustmentScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack(Routes.INVENTORY, inclusive = false) },
            )
        }
        composable(
            route = Routes.MEDICINE,
            deepLinks = listOf(navDeepLink { uriPattern = DeepLinks.MEDICINE_PATTERN }),
        ) {
            MedicineScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.EXPIRY) {
            ExpiryScreen(onBack = { navController.popBackStack() })
        }
    }
}

private fun NavGraphBuilder.reportsGraph(navController: NavHostController) {
    navigation(
        route = Routes.REPORTS_GRAPH,
        startDestination = Routes.REPORTS,
    ) {
        composable(
            route = Routes.REPORTS,
            deepLinks = listOf(navDeepLink { uriPattern = DeepLinks.REPORTS_URI }),
        ) {
            ReportsScreen(
                onBack = { navController.popBackStack() },
                onOpenDrawer = LocalDrawerOpener.current,
                onOpenGlobalSearch = LocalGlobalSearchOpener.current,
                onRequireSubscription = { navController.navigateInApp(Routes.SUBSCRIPTION) },
            )
        }
    }
}
