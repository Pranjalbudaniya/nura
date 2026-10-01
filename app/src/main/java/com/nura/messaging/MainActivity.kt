package com.nura.messaging

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.nura.messaging.core.common.ui.theme.NuraTheme
import com.nura.messaging.domain.usecases.auth.HandleDeepLinkUseCase
import com.nura.messaging.ui.navigation.NuraNavHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var handleDeepLinkUseCase: HandleDeepLinkUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            NuraTheme {
                NuraNavHost()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uriString = intent?.data?.toString() ?: return
        lifecycleScope.launch {
            handleDeepLinkUseCase(uriString)
        }
    }
}
