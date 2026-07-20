package com.medipro.manager.navigation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Bottom-tab root → exit app (do not restore previous tab back stack).
 * Child screen in tab stack → pop within the same tab.
 */
fun NavHostController.handleMainBack(onExitApp: () -> Unit): Boolean {
    val route = currentBackStackEntry?.destination?.route
    return when {
        route == Routes.GLOBAL_SEARCH -> popBackStack()
        Routes.isBottomTabRoot(route) -> {
            onExitApp()
            true
        }
        else -> popBackStack()
    }
}

@Composable
fun MainAppBackHandler(
    navController: NavHostController,
    currentRoute: String?,
    drawerState: DrawerState?,
    drawerScope: CoroutineScope?,
    enabled: Boolean,
) {
    val context = LocalContext.current
    val exitApp: () -> Unit = {
        (context as? Activity)?.finish()
    }

    BackHandler(enabled = enabled) {
        if (drawerState?.isOpen == true && drawerScope != null) {
            drawerScope.launch { drawerState.close() }
            return@BackHandler
        }
        if (!navController.handleMainBack(onExitApp = exitApp)) {
            exitApp()
        }
    }
}

/**
 * Resolves a medipro:// URI into in-app navigation (multiple back stacks aware).
 * Returns false when the URI is not a supported MediPro deep link.
 */
fun NavHostController.navigateDeepLink(uri: Uri): Boolean {
    val target = uri.toDeepLinkTarget() ?: return false
    when (target) {
        is DeepLinkTarget.SaleInvoice -> {
            navigateBottomNavTab(Routes.SALES)
            navigateInApp("sales_invoice/${target.invoiceRef}")
        }
        is DeepLinkTarget.PurchaseInvoice -> {
            navigateBottomNavTab(Routes.PURCHASE)
            navigateInApp("purchase_invoice/${target.invoiceNumber}")
        }
        is DeepLinkTarget.Medicine -> {
            navigateBottomNavTab(Routes.INVENTORY)
            navigateInApp(Routes.MEDICINE)
        }
        is DeepLinkTarget.Customer -> {
            navigateBottomNavTab(Routes.DASHBOARD)
            navigateInApp(Routes.CUSTOMER)
        }
        is DeepLinkTarget.Tab -> navigateBottomNavTab(target.route)
    }
    return true
}

fun NavHostController.consumePendingDeepLink(): Boolean {
    val uri = DeepLinkHolder.consume() ?: return false
    return navigateDeepLink(uri)
}

/** Navigate to a screen within the current or target tab stack. */
fun NavHostController.navigateInApp(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}

/**
 * Switches bottom-nav tabs using multiple back stacks (NiA / Gmail pattern).
 * Each tab graph keeps its own child back stack (e.g. Sales → History → Return).
 */
fun NavHostController.navigateBottomNavTab(tabRoute: String) {
    val graphRoute = Routes.tabGraphRoute(tabRoute)
    val mainGraph = graph.findNode(Routes.MAIN) as? NavGraph
    val startDestinationId = mainGraph?.startDestinationId ?: graph.startDestinationId
    navigate(graphRoute) {
        popUpTo(startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
