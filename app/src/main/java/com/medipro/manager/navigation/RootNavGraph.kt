package com.medipro.manager.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.medipro.manager.BuildConfig
import com.medipro.manager.feature.license.presentation.LicenseScreen
import com.medipro.manager.feature.license.presentation.LoginScreen
import com.medipro.manager.feature.license.presentation.SubscriptionScreen
import com.medipro.manager.ui.applock.AppLockScreen
import com.medipro.manager.ui.splash.SplashScreen

/** Root graph: Splash → Login → Activation → Main. License check runs silently in background. */
fun NavGraphBuilder.rootNavGraph(navController: NavHostController) {
    composable(Routes.SPLASH) {
        SplashScreen(
            onNavigateToLogin = {
                navController.navigate(Routes.LOGIN) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            },
            onNavigateToAppLock = {
                navController.navigate(Routes.ACTIVATION) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            },
            onNavigateToDashboard = {
                navController.navigate(Routes.MAIN) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            },
        )
    }

    composable(Routes.LOGIN) {
        LoginScreen(
            onLoginSuccess = {
                navController.navigate(Routes.MAIN) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            },
        )
    }

    composable(Routes.LICENSE) {
        LicenseScreen(
            onLicenseVerified = {
                if (!navController.popBackStack()) {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LICENSE) { inclusive = true }
                    }
                }
            },
        )
    }

    composable(Routes.LICENSE_EXPIRED) {
        SubscriptionScreen(
            appVersion = BuildConfig.VERSION_NAME,
            onBack = {
                if (!navController.popBackStack()) {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LICENSE_EXPIRED) { inclusive = true }
                    }
                }
            },
            onSubscribe = { navController.navigateInApp(Routes.LICENSE) },
            onImportLicense = { navController.navigateInApp(Routes.LICENSE) },
        )
    }

    composable(Routes.SUBSCRIPTION) {
        SubscriptionScreen(
            appVersion = BuildConfig.VERSION_NAME,
            onBack = { navController.popBackStack() },
            onSubscribe = { navController.navigateInApp(Routes.LICENSE) },
            onImportLicense = { navController.navigateInApp(Routes.LICENSE) },
        )
    }

    composable(Routes.ACTIVATION) {
        AppLockScreen(
            onUnlocked = {
                navController.navigate(Routes.MAIN) {
                    popUpTo(Routes.ACTIVATION) { inclusive = true }
                }
            },
        )
    }

    mainNavGraph(navController)
}
