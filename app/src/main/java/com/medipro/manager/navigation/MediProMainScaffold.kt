package com.medipro.manager.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.medipro.manager.core.designsystem.navigation.BottomNavTab
import com.medipro.manager.core.designsystem.navigation.MediProBottomBar
import com.medipro.manager.core.designsystem.navigation.MediProNavigationDrawerContent
import kotlinx.coroutines.launch

val LocalDrawerOpener = compositionLocalOf<(() -> Unit)?> { null }
val LocalGlobalSearchOpener = compositionLocalOf<(() -> Unit)?> { null }

@Composable
fun MediProMainScaffold(
    navController: NavHostController,
    content: @Composable (Modifier) -> Unit,
    drawerViewModel: NavigationDrawerViewModel = hiltViewModel(),
) {
    val drawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = Routes.showsBottomBar(currentRoute)
    val showDrawer = Routes.showsMainChrome(currentRoute)
    val drawerUiState by drawerViewModel.drawerState.collectAsStateWithLifecycle()

    val openDrawer: () -> Unit = {
        scope.launch { drawerState.open() }
    }

    val openGlobalSearch: () -> Unit = {
        navController.navigate(Routes.GLOBAL_SEARCH) {
            launchSingleTop = true
        }
    }

    MainAppBackHandler(
        navController = navController,
        currentRoute = currentRoute,
        drawerState = if (showDrawer) drawerState else null,
        drawerScope = if (showDrawer) scope else null,
        enabled = Routes.isMainAppRoute(currentRoute),
    )

    if (!showDrawer) {
        content(Modifier)
        return
    }

    CompositionLocalProvider(
        LocalDrawerOpener provides openDrawer,
        LocalGlobalSearchOpener provides openGlobalSearch,
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                MediProNavigationDrawerContent(
                    currentRoute = currentRoute,
                    drawerState = drawerUiState,
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }
                        if (route in Routes.BOTTOM_NAV_ROUTES) {
                            navController.navigateBottomNavTab(route)
                        } else {
                            navController.navigateInApp(route)
                        }
                    },
                    onLogout = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            },
        ) {
            Scaffold(
                bottomBar = {
                    if (showBottomBar) {
                        MediProBottomBar(
                            selected = Routes.bottomNavTabForRoute(currentRoute) ?: BottomNavTab.DASHBOARD,
                            badges = drawerUiState.badges,
                            onTabSelected = { tab ->
                                navController.navigateBottomNavTab(tab.route)
                            },
                        )
                    }
                },
            ) { padding ->
                content(Modifier.padding(padding))
            }
        }
    }
}
