package com.medipro.manager

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.medipro.manager.core.designsystem.theme.MediProTheme
import com.medipro.manager.navigation.DeepLinks
import com.medipro.manager.navigation.MediProNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var deepLinkUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deepLinkUri = intent.extractMediProDeepLink()
        enableEdgeToEdge()
        setContent {
            MediProTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MediProNavHost(
                        deepLinkUri = deepLinkUri,
                        onDeepLinkConsumed = { deepLinkUri = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkUri = intent.extractMediProDeepLink()
    }

    private fun Intent?.extractMediProDeepLink(): Uri? =
        this?.data?.takeIf { it.scheme == DeepLinks.SCHEME }
}
