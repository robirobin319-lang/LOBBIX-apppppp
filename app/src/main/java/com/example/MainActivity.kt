package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notifications.MatchNotificationHelper
import com.example.ui.navigation.LobbixRoot
import com.example.ui.theme.LobbixTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val deepLinkTournamentId = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MatchNotificationHelper.ensureChannel(applicationContext)
        extractTournamentDeepLink(intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            val targetTournamentId by deepLinkTournamentId.collectAsStateWithLifecycle()
            LobbixTheme {
                LobbixRoot(
                    deepLinkTournamentId = targetTournamentId,
                    onConsumeDeepLink = { deepLinkTournamentId.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractTournamentDeepLink(intent)
    }

    private fun extractTournamentDeepLink(intent: Intent?) {
        val tid = intent?.getStringExtra(MatchNotificationHelper.EXTRA_TOURNAMENT_ID)
        if (!tid.isNullOrBlank()) {
            deepLinkTournamentId.value = tid
        }
    }
}
