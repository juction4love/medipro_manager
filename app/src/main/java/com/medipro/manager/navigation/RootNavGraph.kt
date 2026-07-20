package com.medipro.manager.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.medipro.manager.feature.license.presentation.LicenseScreen
import com.medipro.manager.ui.applock.AppLockScreen
import com.medipro.manager.ui.splash.SplashScreen

/** Root graph: Splash → License → Activation → Main. */
fun NavGraphBuilder.rootNavGraph(navController: NavHostController) {
    composable(Routes.SPLASH) {
        SplashScreen(
            onNavigateToLicense = {
                navController.navigate(Routes.LICENSE) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            },
            onNavigateToExpiredLicense = {
                navController.navigate(Routes.LICENSE_EXPIRED) {
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

    composable(Routes.LICENSE) {
        LicenseScreen(
            onLicenseVerified = {
                navController.navigate(Routes.ACTIVATION) {
                    popUpTo(Routes.LICENSE) { inclusive = true }
                }
            },
        )
    }

    composable(Routes.LICENSE_EXPIRED) {
        LicenseScreen(
            expiredMode = true,
            onLicenseVerified = {
                navController.navigate(Routes.ACTIVATION) {
                    popUpTo(Routes.LICENSE_EXPIRED) { inclusive = true }
                }
            },
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
