package com.medipro.manager.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Composable
fun MediProNavHost(
    deepLinkUri: Uri? = null,
    onDeepLinkConsumed: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route?.substringBefore("/")

    LaunchedEffect(deepLinkUri) {
        deepLinkUri?.let { DeepLinkHolder.store(it) }
    }

    LaunchedEffect(currentRoute) {
        if (currentRoute == null || currentRoute in NavigationGraph.AUTH_ROUTES) return@LaunchedEffect
        if (navController.consumePendingDeepLink()) {
            onDeepLinkConsumed()
        }
    }

    MediProMainScaffold(
        navController = navController,
        content = { modifier ->
            NavHost(
                navController = navController,
                startDestination = Routes.SPLASH,
                modifier = modifier,
            ) {
                rootNavGraph(navController)
            }
        },
    )
}
