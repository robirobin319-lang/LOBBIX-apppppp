package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.JoinedTournamentRecord
import com.example.model.PlayerProfile
import com.example.model.UiState
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
import com.example.ui.theme.GunmetalCard
import com.example.ui.theme.NeonScarlet
import com.example.ui.theme.SteelBorder

@Composable
fun ProfileScreen(
    profileState: UiState<PlayerProfile?>,
    joinedState: UiState<List<JoinedTournamentRecord>>,
    isBusy: Boolean,
    onUpdateProfile: (
        fullName: String,
        inGameName: String,
        phone: String,
        freeFireId: String,
        freeFireLevel: Long,
        avatarPreset: String
    ) -> Unit,
    onOpenTournament: (String) -> Unit,
    onOpenSupport: () -> Unit,
    onOpenNotifications: () -> Unit,
    onSignOut: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    CenteredResponsiveColumn {
        when (profileState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CrimsonRed)
                }
            }

            is UiState.Error -> {
                Box(modifier = Modifier.padding(16.dp)) {
                    TacticalCard { Text(profileState.message, color = NeonScarlet) }
                }
            }

            is UiState.Success -> {
                val profile = profileState.data
                val history = (joinedState as? UiState.Success)?.data.orEmpty()
                val hasFfUid = !profile?.freeFireId.isNullOrBlank()
                val hasIgn = !profile?.inGameName.isNullOrBlank()

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        TacticalCard(accentBorder = true) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_avatar_player),
                                    contentDescription = "Player Profile Photo",
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, CrimsonRed, CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = profile?.fullName?.ifBlank { "Free Fire Player" } ?: "Free Fire Player",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = CrispWhite
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        StatusPill(
                                            text = if (hasFfUid) "UID: ${profile?.freeFireId}" else "UID: NOT SET",
                                            containerColor = if (hasFfUid) CrimsonTint else AmberPending.copy(alpha = 0.16f),
                                            contentColor = if (hasFfUid) NeonScarlet else AmberPending
                                        )
                                        StatusPill(
                                            text = "LVL ${profile?.freeFireLevel ?: 1}",
                                            containerColor = GoldTrophy.copy(alpha = 0.16f),
                                            contentColor = GoldTrophy
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.SportsEsports,
                                            contentDescription = null,
                                            tint = if (hasIgn) CrimsonRed else CoolSlate,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (hasIgn) "IGN: ${profile?.inGameName}" else "IGN: Tap Edit to add In-Game Name",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (hasIgn) CrispWhite else AmberPending
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Mail, contentDescription = null, tint = CoolSlate, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = profile?.email?.ifBlank { "Google Verified" } ?: "Google Verified",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = CoolSlate
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CareerStatBox(
                                    label = "MATCHES",
                                    value = (profile?.totalMatches ?: history.size.toLong()).toString(),
                                    modifier = Modifier.weight(1f)
                                )
                                CareerStatBox(
                                    label = "WINS",
                                    value = (profile?.totalWins ?: 0L).toString(),
                                    modifier = Modifier.weight(1f)
                                )
                                CareerStatBox(
                                    label = "KILLS",
                                    value = (profile?.totalKills ?: 0L).toString(),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { showEditDialog = true },
                                enabled = !isBusy,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("edit_profile_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("EDIT FREE FIRE UID & IGN", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }

                    // Support & Alerts Quick Navigation (Wallet lives in top header badge)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onOpenSupport,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("profile_support_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, SteelBorder)
                            ) {
                                Icon(Icons.Filled.SupportAgent, contentDescription = null, tint = CrispWhite, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Support", color = CrispWhite)
                            }
                            OutlinedButton(
                                onClick = onOpenNotifications,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("profile_notifications_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, SteelBorder)
                            ) {
                                Icon(Icons.Filled.Notifications, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Alerts", color = CrispWhite)
                            }
                        }
                    }

                    item {
                        Text(
                            text = "TOURNAMENT HISTORY",
                            style = MaterialTheme.typography.titleLarge,
                            color = CrispWhite
                        )
                    }

                    if (history.isEmpty()) {
                        item {
                            EmptyArenaCard(
                                title = "No Tournament History",
                                subtitle = "Matches you join will be tracked here with placements, kills, and Room Details links.",
                                variant = EmptyStateVariant.MATCHES
                            )
                        }
                    } else {
                        items(history, key = { it.tournamentId }) { record ->
                            TacticalCard(
                                onClick = { onOpenTournament(record.tournamentId) },
                                modifier = Modifier.testTag("history_item_${record.tournamentId}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StatusPill(
                                        text = record.modeEnum.label,
                                        containerColor = CrimsonTint,
                                        contentColor = NeonScarlet
                                    )
                                    StatusPill(
                                        text = record.status,
                                        containerColor = EmeraldLive.copy(alpha = 0.16f),
                                        contentColor = EmeraldLive
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(record.tournamentTitle, style = MaterialTheme.typography.titleMedium, color = CrispWhite)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${record.mapName} • ${record.scheduleLabel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CoolSlate
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = GoldTrophy, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "${record.placementLabel} • ${record.killsLogged} Kills",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = GoldTrophy
                                    )
                                }
                            }
                        }
                    }

                    item {
                        OutlinedButton(
                            onClick = onSignOut,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("sign_out_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.7f))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = NeonScarlet)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SIGN OUT", color = NeonScarlet, style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }

                if (showEditDialog && profile != null) {
                    EditProfileDialog(
                        profile = profile,
                        onDismiss = { showEditDialog = false },
                        onSave = { name, ign, phone, ffId, ffLevel ->
                            showEditDialog = false
                            onUpdateProfile(name, ign, phone, ffId, ffLevel, profile.avatarPreset)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CareerStatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = CarbonSurface,
        border = BorderStroke(1.dp, SteelBorder)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = CrispWhite, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = CoolSlate)
        }
    }
}

@Composable
private fun EditProfileDialog(
    profile: PlayerProfile,
    onDismiss: () -> Unit,
    onSave: (fullName: String, inGameName: String, phone: String, freeFireId: String, level: Long) -> Unit
) {
    var name by remember { mutableStateOf(profile.fullName) }
    var inGameName by remember { mutableStateOf(profile.inGameName) }
    var ffId by remember { mutableStateOf(profile.freeFireId) }
    var ffLevelText by remember { mutableStateOf(profile.freeFireLevel.toString()) }
    var phone by remember { mutableStateOf(profile.phone) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GunmetalCard,
        titleContentColor = CrispWhite,
        textContentColor = CoolSlate,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = CrimsonRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Free Fire Profile")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    label = { Text("Full Name") },
                    singleLine = true,
                    colors = lobbixFieldColors(),
                    modifier = Modifier.testTag("edit_fullname_input")
                )
                OutlinedTextField(
                    value = inGameName,
                    onValueChange = { inGameName = it; errorMsg = null },
                    label = { Text("Free Fire In-Game Name (IGN)") },
                    placeholder = { Text("e.g., ViperX_FF") },
                    singleLine = true,
                    colors = lobbixFieldColors(),
                    modifier = Modifier.testTag("edit_ign_input")
                )
                OutlinedTextField(
                    value = ffId,
                    onValueChange = { ffId = it.filter { ch -> ch.isDigit() || ch.isLetter() || ch == '-' }; errorMsg = null },
                    label = { Text("Free Fire UID") },
                    placeholder = { Text("Enter numeric Free Fire UID") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = lobbixFieldColors(),
                    modifier = Modifier.testTag("edit_ffuid_input")
                )
                OutlinedTextField(
                    value = ffLevelText,
                    onValueChange = { ffLevelText = it.filter { ch -> ch.isDigit() }; errorMsg = null },
                    label = { Text("Free Fire Level (1-120)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = lobbixFieldColors(),
                    modifier = Modifier.testTag("edit_fflevel_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; errorMsg = null },
                    label = { Text("Mobile Number (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    colors = lobbixFieldColors(),
                    modifier = Modifier.testTag("edit_phone_input")
                )
                if (errorMsg != null) {
                    Text(errorMsg!!, color = NeonScarlet, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lvl = ffLevelText.toLongOrNull()
                    when {
                        name.trim().isEmpty() -> errorMsg = "Player name is required."
                        inGameName.trim().isEmpty() -> errorMsg = "Enter your Free Fire In-Game Name (IGN)."
                        ffId.trim().length < 5 -> errorMsg = "Enter a valid Free Fire UID (min 5 characters)."
                        lvl == null || lvl !in 1L..120L -> errorMsg = "Level must be between 1 and 120."
                        else -> onSave(name.trim(), inGameName.trim(), phone.trim(), ffId.trim(), lvl)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = CoolSlate)
            }
        }
    )
}
