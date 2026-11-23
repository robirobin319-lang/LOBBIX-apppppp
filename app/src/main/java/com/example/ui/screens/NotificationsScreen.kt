package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AppNotification
import com.example.model.NotificationCategory
import com.example.model.UiState
import com.example.notifications.MatchNotificationHelper
import com.example.ui.components.CenteredResponsiveColumn
import com.example.ui.components.EmptyArenaCard
import com.example.ui.components.EmptyStateVariant
import com.example.ui.components.StatusPill
import com.example.ui.components.TacticalCard
import com.example.ui.theme.AmberPending
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.CrimsonTint
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.GoldTrophy
import com.example.ui.theme.NeonScarlet

@Composable
fun NotificationsScreen(
    notificationsState: UiState<List<AppNotification>>,
    onMarkRead: (String) -> Unit,
    onOpenTournament: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var filterWire by rememberSaveable { mutableStateOf<String?>(null) }
    var hasNotifPerm by remember {
        mutableStateOf(MatchNotificationHelper.hasNotificationPermission(context))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPerm = granted || MatchNotificationHelper.hasNotificationPermission(context)
    }

    CenteredResponsiveColumn {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 20.dp, start = 12.dp, end = 12.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("notifications_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CrispWhite)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("NOTIFICATIONS", style = MaterialTheme.typography.headlineLarge, color = CrispWhite)
            }

            if (!hasNotifPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CrimsonTint,
                    border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable 10-Min Room Alerts",
                                style = MaterialTheme.typography.titleSmall,
                                color = CrispWhite
                            )
                            Text(
                                text = "Allow notifications to receive Room ID & Password 10 min before your match.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CoolSlate
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("enable_notifications_banner_button")
                        ) {
                            Text("Enable", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = filterWire == null,
                        onClick = { filterWire = null },
                        label = { Text("All") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CrimsonRed,
                            selectedLabelColor = CrispWhite,
                            containerColor = CarbonSurface,
                            labelColor = CoolSlate
                        )
                    )
                }
                items(NotificationCategory.entries) { cat ->
                    FilterChip(
                        selected = filterWire == cat.wireValue,
                        onClick = { filterWire = cat.wireValue },
                        label = { Text(cat.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CrimsonRed,
                            selectedLabelColor = CrispWhite,
                            containerColor = CarbonSurface,
                            labelColor = CoolSlate
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (notificationsState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CrimsonRed)
                    }
                }

                is UiState.Error -> {
                    Box(modifier = Modifier.padding(16.dp)) {
                        TacticalCard { Text(notificationsState.message, color = NeonScarlet) }
                    }
                }

                is UiState.Success -> {
                    val filtered = if (filterWire == null) {
                        notificationsState.data
                    } else {
                        notificationsState.data.filter { it.category == filterWire }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (filtered.isEmpty()) {
                            item {
                                EmptyArenaCard(
                                    title = "No Alerts in This Feed",
                                    subtitle = "Tournament updates, published Room IDs, match reminders, and results show up here.",
                                    variant = EmptyStateVariant.NOTIFICATIONS
                                )
                            }
                        } else {
                            items(filtered, key = { it.id }) { notif ->
                                NotificationCard(
                                    notif = notif,
                                    onClick = {
                                        if (!notif.readStatus) {
                                            onMarkRead(notif.id)
                                        }
                                        if (notif.tournamentId.isNotBlank()) {
                                            onOpenTournament(notif.tournamentId)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notif: AppNotification,
    onClick: () -> Unit
) {
    val (icon, tint) = when (notif.categoryEnum) {
        NotificationCategory.ROOM_PUBLISHED -> Icons.Filled.VpnKey to EmeraldLive
        NotificationCategory.MATCH_REMINDER -> Icons.Filled.NotificationsActive to AmberPending
        NotificationCategory.RESULT_ANNOUNCED -> Icons.Filled.EmojiEvents to GoldTrophy
        NotificationCategory.TOURNAMENT_UPDATE -> Icons.Filled.Campaign to CrimsonRed
    }

    TacticalCard(
        accentBorder = !notif.readStatus,
        onClick = onClick,
        modifier = Modifier.testTag("notification_card_${notif.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusPill(
                text = notif.categoryEnum.label,
                containerColor = tint.copy(alpha = 0.16f),
                contentColor = tint,
                icon = icon
            )
            if (!notif.readStatus) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(CrimsonRed)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(notif.title, style = MaterialTheme.typography.titleMedium, color = CrispWhite)
        Spacer(modifier = Modifier.height(4.dp))
        Text(notif.message, style = MaterialTheme.typography.bodyMedium, color = CoolSlate)
        if (notif.tournamentId.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap to open Tournament & Room Details →",
                style = MaterialTheme.typography.labelMedium,
                color = NeonScarlet
            )
        }
    }
}
