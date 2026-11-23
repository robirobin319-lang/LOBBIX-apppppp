package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.GameMode
import com.example.model.PlayerProfile
import com.example.model.RoomCredentials
import com.example.model.Tournament
import com.example.model.TournamentStatus
import com.example.model.UiState
import com.example.notifications.MatchNotificationHelper
import com.example.notifications.MatchScheduleParser
import com.example.ui.components.CenteredResponsiveColumn
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
import com.example.ui.theme.GunmetalCard
import com.example.ui.theme.GunmetalElevated
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.NeonScarlet
import com.example.ui.theme.SteelBorder

@Composable
fun TournamentDetailScreen(
    tournamentState: UiState<Tournament?>,
    profile: PlayerProfile?,
    isJoined: Boolean,
    roomCredentialsState: UiState<RoomCredentials?>,
    isBusy: Boolean,
    onBack: () -> Unit,
    onJoinClick: (
        tournament: Tournament,
        overrideIgn: String,
        overrideFfUid: String,
        teamTag: String
    ) -> Unit,
    onSendRoomAlertNow: (Tournament, RoomCredentials?) -> Unit,
    onOpenSupport: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var showJoinDialog by remember { mutableStateOf(false) }
    var ignInput by remember(profile?.inGameName) { mutableStateOf(profile?.inGameName.orEmpty()) }
    var ffUidInput by remember(profile?.freeFireId) { mutableStateOf(profile?.freeFireId.orEmpty()) }
    var teamTagInput by remember { mutableStateOf("") }
    var joinDialogError by remember { mutableStateOf<String?>(null) }

    var notifPermissionGranted by remember {
        mutableStateOf(MatchNotificationHelper.hasNotificationPermission(context))
    }
    var pendingAlertAfterGrant by remember { mutableStateOf<Pair<Tournament, RoomCredentials?>?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifPermissionGranted = granted || MatchNotificationHelper.hasNotificationPermission(context)
        val pending = pendingAlertAfterGrant
        pendingAlertAfterGrant = null
        if (notifPermissionGranted && pending != null) {
            onSendRoomAlertNow(pending.first, pending.second)
        }
    }

    fun requestNotifPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !MatchNotificationHelper.hasNotificationPermission(context)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    CenteredResponsiveColumn {
        when (tournamentState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CrimsonRed)
                }
            }

            is UiState.Error -> {
                Column(modifier = Modifier.statusBarsPadding().padding(top = 20.dp, start = 16.dp, end = 16.dp)) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = CrispWhite)
                    }
                    TacticalCard {
                        Text(tournamentState.message, color = NeonScarlet)
                    }
                }
            }

            is UiState.Success -> {
                val tournament = tournamentState.data
                if (tournament == null) {
                    Column(modifier = Modifier.statusBarsPadding().padding(top = 20.dp, start = 16.dp, end = 16.dp)) {
                        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = CrispWhite)
                        }
                        TacticalCard {
                            Text("Tournament not found.", color = CoolSlate)
                        }
                    }
                } else {
                    val bannerRes = if (tournament.bannerKey == "clash") R.drawable.img_hero_clash else R.drawable.img_hero_bermuda
                    val slotsFull = tournament.joinedCount >= tournament.maxPlayers
                    val isCompleted = tournament.statusEnum == TournamentStatus.COMPLETED
                    val progress = if (tournament.maxPlayers > 0) {
                        (tournament.joinedCount.toFloat() / tournament.maxPlayers.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = bannerRes),
                                    contentDescription = tournament.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    MatteBlack.copy(alpha = 0.45f),
                                                    MatteBlack.copy(alpha = 0.96f)
                                                )
                                            )
                                        )
                                )
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier
                                        .statusBarsPadding()
                                        .padding(top = 20.dp, start = 12.dp)
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MatteBlack.copy(alpha = 0.65f))
                                        .testTag("tournament_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = CrispWhite
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(16.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        StatusPill(
                                            text = "FREE FIRE • ${tournament.modeEnum.label}",
                                            containerColor = CrimsonRed
                                        )
                                        if (isJoined) {
                                            StatusPill(
                                                text = "JOINED",
                                                containerColor = EmeraldLive.copy(alpha = 0.24f),
                                                contentColor = EmeraldLive,
                                                icon = Icons.Filled.CheckCircle
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = tournament.title,
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = CrispWhite
                                    )
                                }
                            }
                        }

                        // Tournament Core Specs
                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                TacticalCard {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        DetailSpecTile(
                                            icon = Icons.Filled.Schedule,
                                            label = "DATE & TIME",
                                            value = tournament.scheduleLabel,
                                            modifier = Modifier.weight(1f)
                                        )
                                        DetailSpecTile(
                                            icon = Icons.Filled.Map,
                                            label = "ARENA MAP",
                                            value = tournament.mapName,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(CarbonSurface)
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("PRIZE POOL", style = MaterialTheme.typography.labelSmall, color = CoolSlate)
                                            Text("${tournament.prizePool}", style = MaterialTheme.typography.titleLarge, color = GoldTrophy)
                                        }
                                        Column {
                                            Text("PER KILL", style = MaterialTheme.typography.labelSmall, color = CoolSlate)
                                            Text("${tournament.perKillReward}", style = MaterialTheme.typography.titleLarge, color = CrispWhite)
                                        }
                                        Column {
                                            Text("ENTRY", style = MaterialTheme.typography.labelSmall, color = CoolSlate)
                                            Text(
                                                text = if (tournament.entryFee == 0L) "FREE" else "${tournament.entryFee}",
                                                style = MaterialTheme.typography.titleLarge,
                                                color = if (tournament.entryFee == 0L) EmeraldLive else NeonScarlet
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Groups, contentDescription = null, tint = CoolSlate, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Total Joined Players: ${tournament.joinedCount} / ${tournament.maxPlayers}",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = CrispWhite
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp)
                                            .clip(CircleShape),
                                        color = CrimsonRed,
                                        trackColor = GunmetalElevated
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text("TOURNAMENT DETAILS", style = MaterialTheme.typography.labelMedium, color = CrimsonRed)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = tournament.rulesSummary.ifBlank { "Standard Free Fire competitive fair-play rules apply." },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = CoolSlate
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    if (isJoined) {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("joined_status_banner"),
                                            shape = RoundedCornerShape(14.dp),
                                            color = EmeraldLive.copy(alpha = 0.14f),
                                            border = BorderStroke(1.dp, EmeraldLive.copy(alpha = 0.55f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = EmeraldLive)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "JOINED • SLOT CONFIRMED",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = EmeraldLive
                                                )
                                            }
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                val needsPlayerIdentity = profile?.freeFireId.isNullOrBlank() ||
                                                    profile?.inGameName.isNullOrBlank()
                                                val needsTeamTag = tournament.modeEnum == GameMode.DUO ||
                                                    tournament.modeEnum == GameMode.SQUAD
                                                if (needsPlayerIdentity || needsTeamTag) {
                                                    joinDialogError = null
                                                    showJoinDialog = true
                                                } else {
                                                    requestNotifPermissionIfNeeded()
                                                    onJoinClick(tournament, "", "", "")
                                                }
                                            },
                                            enabled = !isBusy && !slotsFull && !isCompleted,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(52.dp)
                                                .testTag("join_tournament_button"),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
                                        ) {
                                            if (isBusy) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(22.dp),
                                                    color = CrispWhite,
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Icon(Icons.Filled.EmojiEvents, contentDescription = null)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = when {
                                                        isCompleted -> "MATCH COMPLETED"
                                                        slotsFull -> "SLOTS FULL"
                                                        else -> "JOIN TOURNAMENT"
                                                    },
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ROOM DETAILS VAULT (Strictly gated to Joined Players!)
                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                RoomDetailsVaultCard(
                                    isJoined = isJoined,
                                    roomCredentialsState = roomCredentialsState,
                                    onCopy = { label, value ->
                                        copyToClipboard(context, label, value)
                                    }
                                )
                            }
                        }

                        // 10-Min Pre-Match Room ID & Password Alert Card (for registered players)
                        if (isJoined) {
                            item {
                                val creds = (roomCredentialsState as? UiState.Success)?.data
                                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    PreMatchReminderCard(
                                        tournament = tournament,
                                        roomCredentials = creds,
                                        permissionGranted = notifPermissionGranted,
                                        onEnableNotifications = { requestNotifPermissionIfNeeded() },
                                        onSendAlertNow = {
                                            if (!notifPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                pendingAlertAfterGrant = tournament to creds
                                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            } else {
                                                onSendRoomAlertNow(tournament, creds)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Match Support Shortcut
                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                OutlinedButton(
                                    onClick = onOpenSupport,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("tournament_support_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, SteelBorder)
                                ) {
                                    Icon(Icons.Filled.SupportAgent, contentDescription = null, tint = CrispWhite)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("NEED MATCH SUPPORT?", color = CrispWhite, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }

                    if (showJoinDialog) {
                        val needsTeamTag = tournament.modeEnum == GameMode.DUO ||
                            tournament.modeEnum == GameMode.SQUAD
                        AlertDialog(
                            onDismissRequest = { showJoinDialog = false },
                            containerColor = GunmetalCard,
                            titleContentColor = CrispWhite,
                            textContentColor = CoolSlate,
                            title = { Text("Confirm Free Fire Entry") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "Verify your Free Fire In-Game Name and UID for ${tournament.modeEnum.label} room verification.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    OutlinedTextField(
                                        value = ignInput,
                                        onValueChange = { ignInput = it; joinDialogError = null },
                                        label = { Text("In-Game Name (IGN)") },
                                        singleLine = true,
                                        colors = lobbixFieldColors()
                                    )
                                    OutlinedTextField(
                                        value = ffUidInput,
                                        onValueChange = { ffUidInput = it.filter { ch -> ch.isDigit() || ch.isLetter() || ch == '-' }; joinDialogError = null },
                                        label = { Text("Free Fire UID") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        colors = lobbixFieldColors()
                                    )
                                    if (needsTeamTag) {
                                        OutlinedTextField(
                                            value = teamTagInput,
                                            onValueChange = { teamTagInput = it },
                                            label = { Text("Team / Clan Name (Optional)") },
                                            singleLine = true,
                                            colors = lobbixFieldColors()
                                        )
                                    }
                                    if (joinDialogError != null) {
                                        Text(joinDialogError!!, color = NeonScarlet, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        when {
                                            ignInput.trim().isEmpty() -> joinDialogError = "Enter your In-Game Name (IGN)."
                                            ffUidInput.trim().length < 5 -> joinDialogError = "Enter a valid Free Fire UID."
                                            else -> {
                                                showJoinDialog = false
                                                requestNotifPermissionIfNeeded()
                                                onJoinClick(
                                                    tournament,
                                                    ignInput.trim(),
                                                    ffUidInput.trim(),
                                                    teamTagInput.trim()
                                                )
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
                                ) {
                                    Text("Confirm Join")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showJoinDialog = false }) {
                                    Text("Cancel", color = CoolSlate)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomDetailsVaultCard(
    isJoined: Boolean,
    roomCredentialsState: UiState<RoomCredentials?>,
    onCopy: (String, String) -> Unit
) {
    TacticalCard(
        modifier = Modifier.testTag("room_details_card"),
        accentBorder = isJoined
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isJoined) Icons.Filled.VpnKey else Icons.Filled.Lock,
                    contentDescription = null,
                    tint = if (isJoined) EmeraldLive else CrimsonRed
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ROOM DETAILS",
                    style = MaterialTheme.typography.titleLarge,
                    color = CrispWhite
                )
            }
            StatusPill(
                text = if (isJoined) "ACCESS UNLOCKED" else "LOCKED",
                containerColor = if (isJoined) EmeraldLive.copy(alpha = 0.18f) else CrimsonTint,
                contentColor = if (isJoined) EmeraldLive else NeonScarlet
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!isJoined) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CarbonSurface,
                border = BorderStroke(1.dp, SteelBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Hidden Room Credentials",
                        tint = CoolSlate,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(id = R.string.room_locked_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = CoolSlate
                    )
                }
            }
        } else {
            when (roomCredentialsState) {
                is UiState.Loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = CrimsonRed, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Syncing live Room ID & Password...", style = MaterialTheme.typography.bodyMedium, color = CoolSlate)
                    }
                }

                is UiState.Error -> {
                    Text(
                        text = roomCredentialsState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeonScarlet
                    )
                }

                is UiState.Success -> {
                    val creds = roomCredentialsState.data
                    if (creds == null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CarbonSurface,
                            border = BorderStroke(1.dp, SteelBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(id = R.string.room_waiting_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = CoolSlate,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else {
                        CredentialCopyRow(
                            label = "ROOM ID",
                            value = creds.roomId,
                            onCopy = { onCopy("Room ID", creds.roomId) },
                            tag = "room_id_value"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        CredentialCopyRow(
                            label = "ROOM PASSWORD",
                            value = creds.roomPassword,
                            onCopy = { onCopy("Room Password", creds.roomPassword) },
                            tag = "room_password_value"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Server: ${creds.serverRegion}",
                            style = MaterialTheme.typography.labelMedium,
                            color = GoldTrophy
                        )
                        if (creds.matchNote.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = creds.matchNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = CoolSlate
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CredentialCopyRow(
    label: String,
    value: String,
    onCopy: () -> Unit,
    tag: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CarbonSurface,
        border = BorderStroke(1.dp, EmeraldLive.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = CoolSlate)
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = CrispWhite,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag(tag)
                )
            }
            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy $label",
                    tint = EmeraldLive
                )
            }
        }
    }
}

@Composable
private fun PreMatchReminderCard(
    tournament: Tournament,
    roomCredentials: RoomCredentials?,
    permissionGranted: Boolean,
    onEnableNotifications: () -> Unit,
    onSendAlertNow: () -> Unit
) {
    val startMillis = remember(tournament.scheduleLabel, tournament.startTimeMillis) {
        MatchScheduleParser.resolveStartTimeMillis(
            scheduleLabel = tournament.scheduleLabel,
            explicitStartMillis = tournament.startTimeMillis
        )
    }
    val triggerAtMillis = startMillis?.let { MatchScheduleParser.reminderTriggerMillis(it) }
    val inPreStartWindow = startMillis?.let { MatchScheduleParser.isInTenMinutePreStartWindow(it) } == true
    val isCompleted = tournament.statusEnum == TournamentStatus.COMPLETED
    val hasCredentials = roomCredentials != null &&
        roomCredentials.roomId.isNotBlank() &&
        roomCredentials.roomPassword.isNotBlank()

    val statusLabel = when {
        isCompleted -> "COMPLETED"
        !permissionGranted -> "PERMISSION NEEDED"
        inPreStartWindow -> "ACTIVE WINDOW"
        triggerAtMillis != null -> "ARMED"
        else -> "SYNCED"
    }
    val statusBg = when {
        isCompleted -> SteelBorder.copy(alpha = 0.35f)
        !permissionGranted -> AmberPending.copy(alpha = 0.2f)
        else -> EmeraldLive.copy(alpha = 0.18f)
    }
    val statusFg = when {
        isCompleted -> CoolSlate
        !permissionGranted -> AmberPending
        else -> EmeraldLive
    }

    TacticalCard(
        modifier = Modifier.testTag("pre_match_reminder_card"),
        accentBorder = !isCompleted
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    tint = if (permissionGranted) CrimsonRed else AmberPending
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "10-MIN MATCH ALERT",
                    style = MaterialTheme.typography.titleMedium,
                    color = CrispWhite
                )
            }
            StatusPill(
                text = statusLabel,
                containerColor = statusBg,
                contentColor = statusFg
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        val scheduleSummary = when {
            isCompleted -> "This tournament has already finished."
            inPreStartWindow -> "Match starts within 10 minutes (${tournament.scheduleLabel}). Your Room ID & Password alert is active."
            triggerAtMillis != null -> "Scheduled automatic alert at ${MatchScheduleParser.formatLocalTime(triggerAtMillis)} (10 mins before ${tournament.scheduleLabel})."
            else -> "Automatic alert armed 10 minutes before match start (${tournament.scheduleLabel})."
        }

        Text(
            text = scheduleSummary,
            style = MaterialTheme.typography.bodySmall,
            color = CoolSlate
        )

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CarbonSurface,
            border = BorderStroke(
                1.dp,
                if (hasCredentials) EmeraldLive.copy(alpha = 0.35f) else SteelBorder
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "NOTIFICATION PAYLOAD PREVIEW",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldTrophy
                )
                if (hasCredentials && roomCredentials != null) {
                    Text(
                        text = "Room ID: ${roomCredentials.roomId}  |  Pass: ${roomCredentials.roomPassword}",
                        style = MaterialTheme.typography.titleSmall,
                        color = CrispWhite,
                        fontWeight = FontWeight.Bold
                    )
                    if (roomCredentials.serverRegion.isNotBlank()) {
                        Text(
                            text = "Server: ${roomCredentials.serverRegion}",
                            style = MaterialTheme.typography.bodySmall,
                            color = CoolSlate
                        )
                    }
                } else {
                    Text(
                        text = "Room ID & Password will be included directly in the notification as soon as Admin publishes them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CoolSlate
                    )
                }
            }
        }

        if (!permissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onEnableNotifications,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("enable_match_alert_permission_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AmberPending)
            ) {
                Icon(
                    imageVector = Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    tint = AmberPending
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ENABLE NOTIFICATION PERMISSION",
                    color = AmberPending,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        if (!isCompleted) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onSendAlertNow,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("send_room_alert_now_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
            ) {
                Icon(
                    imageVector = Icons.Filled.NotificationsActive,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SEND ROOM ALERT NOW",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun DetailSpecTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = CoolSlate)
            Text(value, style = MaterialTheme.typography.titleSmall, color = CrispWhite)
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
}
