package com.medipro.manager.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

object MediProFabDefaults {
    val EdgePadding = 16.dp
    val StickyBarClearance = 76.dp
}

/**
 * Keeps FAB above inner sticky bars so taps are not blocked in nested Scaffold layouts.
 */
@Composable
fun MediProScreenWithFab(
    stickyBarVisible: Boolean = false,
    floatingActionButton: @Composable BoxScope.() -> Unit,
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        content()
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = MediProFabDefaults.EdgePadding,
                    bottom = MediProFabDefaults.EdgePadding +
                        if (stickyBarVisible) MediProFabDefaults.StickyBarClearance else 0.dp,
                )
                .zIndex(1f),
            content = floatingActionButton,
        )
    }
}
