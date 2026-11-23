package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.GameMode
import com.example.model.JoinedTournamentRecord
import com.example.model.Tournament
import com.example.model.UiState
import com.example.ui.components.CenteredResponsiveColumn
import com.example.ui.components.EmptyArenaCard
import com.example.ui.components.EmptyStateVariant
import com.example.ui.components.StatusPill
import com.example.ui.components.TacticalCard
import com.example.ui.components.TournamentCard
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.CrimsonTint
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.GunmetalCard
import com.example.ui.theme.GunmetalElevated
import com.example.ui.theme.NeonScarlet
import com.example.ui.theme.SteelBorder

@Composable
fun ModesScreen(
    tournamentsState: UiState<List<Tournament>>,
    joinedState: UiState<List<JoinedTournamentRecord>>,
    onPreviewTournament: (Tournament) -> Unit
) {
    var selectedModeWire by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedMode = selectedModeWire?.let { GameMode.fromWire(it) }

    val tournaments = (tournamentsState as? UiState.Success)?.data.orEmpty()
    val joinedIds = (joinedState as? UiState.Success)?.data.orEmpty().map { it.tournamentId }.toSet()

    val filteredTournaments = if (selectedMode == null) {
        tournaments
    } else {
        tournaments.filter { it.modeEnum == selectedMode }
    }

    CenteredResponsiveColumn {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                TacticalCard(accentBorder = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            StatusPill(
                                text = "FREE FIRE ONLY",
                                containerColor = CrimsonTint,
                                contentColor = NeonScarlet,
                                icon = Icons.Filled.Bolt
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "SELECT COMBAT MODE",
                                style = MaterialTheme.typography.headlineLarge,
                                color = CrispWhite
                            )
                            Text(
                                text = "Choose Solo, Duo, Squad, Lone Wolf, or 1v1 Match.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CoolSlate
                            )
                        }
                    }
                }
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        ModeChipCard(
                            title = "All Modes",
                            subtitle = "${tournaments.size} Matches",
                            teamSize = "Free Fire",
                            icon = Icons.Filled.Shield,
                            selected = selectedMode == null,
                            onClick = { selectedModeWire = null },
                            tag = "mode_chip_ALL"
                        )
                    }
                    items(GameMode.entries) { mode ->
                        val count = tournaments.count { it.modeEnum == mode }
                        ModeChipCard(
                            title = mode.label,
                            subtitle = "$count Live/Upc.",
                            teamSize = mode.teamSize,
                            icon = modeIcon(mode),
                            selected = selectedMode == mode,
                            onClick = { selectedModeWire = mode.wireValue },
                            tag = "mode_chip_${mode.wireValue}"
                        )
                    }
                }
            }

            item {
                Text(
                    text = selectedMode?.let { "${it.label.uppercase()} TOURNAMENTS" } ?: "ALL FREE FIRE TOURNAMENTS",
                    style = MaterialTheme.typography.titleLarge,
                    color = CrispWhite
                )
            }

            when (tournamentsState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = CrimsonRed)
                        }
                    }
                }

                is UiState.Error -> {
                    item {
                        TacticalCard {
                            Text(tournamentsState.message, color = NeonScarlet)
                        }
                    }
                }

                is UiState.Success -> {
                    if (filteredTournaments.isEmpty()) {
                        item {
                            EmptyArenaCard(
                                title = "No ${selectedMode?.label ?: "Free Fire"} Rooms Open",
                                subtitle = "Check back shortly or switch modes while the Admin Panel schedules the next bracket.",
                                variant = EmptyStateVariant.MATCHES
                            )
                        }
                    } else {
                        items(filteredTournaments, key = { it.id }) { tournament ->
                            TournamentCard(
                                tournament = tournament,
                                isJoined = tournament.id in joinedIds,
                                onClick = { onPreviewTournament(tournament) }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun ModeChipCard(
    title: String,
    subtitle: String,
    teamSize: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        modifier = Modifier
            .width(136.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(tag),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) CrimsonRed else GunmetalCard,
        border = BorderStroke(1.dp, if (selected) NeonScarlet else SteelBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) CrispWhite.copy(alpha = 0.2f) else GunmetalElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = CrispWhite,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = CrispWhite,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) CrispWhite.copy(alpha = 0.9f) else CoolSlate
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = teamSize,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) CrispWhite else NeonScarlet
            )
        }
    }
}

private fun modeIcon(mode: GameMode): ImageVector = when (mode) {
    GameMode.SOLO -> Icons.Filled.Person
    GameMode.DUO -> Icons.Filled.Group
    GameMode.SQUAD -> Icons.Filled.Groups
    GameMode.LONE_WOLF -> Icons.Filled.Shield
    GameMode.ONE_V_ONE -> Icons.Filled.SportsMma
}
