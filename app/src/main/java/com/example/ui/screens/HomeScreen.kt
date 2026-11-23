package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.GameMode
import com.example.model.JoinedTournamentRecord
import com.example.model.PlayerProfile
import com.example.model.Tournament
import com.example.model.TournamentStatus
import com.example.model.UiState
import com.example.ui.components.CenteredResponsiveColumn
import com.example.ui.components.EmptyArenaCard
import com.example.ui.components.EmptyStateVariant
import com.example.ui.components.FeaturedTournamentBanner
import com.example.ui.components.StatusPill
import com.example.ui.components.TacticalCard
import com.example.ui.components.TournamentCard
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.CrimsonDark
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.CrimsonTint
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.GunmetalCard
import com.example.ui.theme.GunmetalElevated
import com.example.ui.theme.NeonScarlet
import com.example.ui.theme.SteelBorder

@Composable
fun HomeScreen(
    profileState: UiState<PlayerProfile?>,
    tournamentsState: UiState<List<Tournament>>,
    joinedState: UiState<List<JoinedTournamentRecord>>,
    onPreviewTournament: (Tournament) -> Unit,
    onOpenTournament: (String) -> Unit,
    onOpenModes: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSupport: () -> Unit
) {
    val profile = (profileState as? UiState.Success)?.data
    val tournaments = (tournamentsState as? UiState.Success)?.data.orEmpty()
    val joinedRecords = (joinedState as? UiState.Success)?.data.orEmpty()
    val joinedIds = joinedRecords.map { it.tournamentId }.toSet()

    var selectedModeWire by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedMode = selectedModeWire?.let { GameMode.fromWire(it) }

    val modeFiltered = if (selectedMode == null) {
        tournaments
    } else {
        tournaments.filter { it.modeEnum == selectedMode }
    }
    val featured = modeFiltered.filter { it.statusEnum == TournamentStatus.LIVE }.ifEmpty { modeFiltered.take(2) }
    val upcoming = modeFiltered.filter { it.statusEnum != TournamentStatus.COMPLETED }

    CenteredResponsiveColumn {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Player Profile Summary + Quick Support Shortcut (Wallet badge sits cleanly in TopBar)
            item {
                PlayerSummaryBanner(
                    profile = profile,
                    onProfileClick = onOpenProfile,
                    onSupportClick = onOpenSupport
                )
            }

            // 2. Category Mode Filter (All, Solo, Duo, Squad + additional Free Fire modes)
            item {
                HomeModeFilterBar(
                    tournaments = tournaments,
                    selectedMode = selectedMode,
                    onSelectMode = { mode ->
                        selectedModeWire = if (selectedMode == mode) null else mode?.wireValue
                    }
                )
            }

            // 3. Featured Free Fire Tournaments
            item {
                SectionHeader(
                    title = selectedMode?.let { "FEATURED ${it.label.uppercase()} ARENA" }
                        ?: "FEATURED FREE FIRE ARENA",
                    actionLabel = "All Modes",
                    onActionClick = onOpenModes
                )
                Spacer(modifier = Modifier.height(10.dp))
                when (tournamentsState) {
                    is UiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = CrimsonRed)
                        }
                    }

                    is UiState.Error -> {
                        TacticalCard {
                            Text(
                                text = tournamentsState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = NeonScarlet
                            )
                        }
                    }

                    is UiState.Success -> {
                        if (featured.isEmpty()) {
                            EmptyArenaCard(
                                title = if (selectedMode != null) {
                                    "No Featured ${selectedMode.label} Matches"
                                } else {
                                    "No Featured Matches Yet"
                                },
                                subtitle = if (selectedMode != null) {
                                    "Try another Free Fire mode filter or tap All to view every open tournament."
                                } else {
                                    "Tournaments published from the LOBBIX Admin Panel appear here in real time."
                                },
                                variant = EmptyStateVariant.TROPHY
                            )
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(featured, key = { it.id }) { tournament ->
                                    Box(modifier = Modifier.width(320.dp)) {
                                        FeaturedTournamentBanner(
                                            tournament = tournament,
                                            isJoined = tournament.id in joinedIds,
                                            onClick = { onPreviewTournament(tournament) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. My Joined Tournaments
            item {
                SectionHeader(
                    title = "MY JOINED TOURNAMENTS",
                    actionLabel = "${joinedRecords.size} Active",
                    onActionClick = onOpenProfile
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (joinedRecords.isEmpty()) {
                    TacticalCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "No Matches Joined Yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = CrispWhite
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Join any Free Fire match below to unlock live Room ID & Password.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CoolSlate
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CrimsonRed,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(onClick = onOpenModes)
                            ) {
                                Text(
                                    text = "BROWSE",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CrispWhite,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(joinedRecords, key = { it.tournamentId }) { record ->
                            val matchingTourney = tournaments.firstOrNull { it.id == record.tournamentId }
                            val roomLive = matchingTourney?.roomPublished == true
                            TacticalCard(
                                modifier = Modifier
                                    .width(260.dp)
                                    .testTag("home_joined_${record.tournamentId}"),
                                accentBorder = roomLive,
                                onClick = { onOpenTournament(record.tournamentId) }
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
                                        text = if (roomLive) "ROOM LIVE" else "JOINED",
                                        containerColor = EmeraldLive.copy(alpha = 0.2f),
                                        contentColor = EmeraldLive,
                                        icon = if (roomLive) Icons.Filled.VpnKey else null
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = record.tournamentTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = CrispWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${record.mapName} • ${record.scheduleLabel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CoolSlate,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 5. Upcoming Matches (Filtered by selected category)
            item {
                SectionHeader(
                    title = selectedMode?.let { "${it.label.uppercase()} MATCHES (${upcoming.size})" }
                        ?: "UPCOMING FREE FIRE MATCHES",
                    actionLabel = if (selectedMode != null) "Show All" else "Filter Modes",
                    onActionClick = {
                        if (selectedMode != null) {
                            selectedModeWire = null
                        } else {
                            onOpenModes()
                        }
                    }
                )
            }

            if (tournamentsState is UiState.Success && upcoming.isEmpty()) {
                item {
                    EmptyArenaCard(
                        title = selectedMode?.let { "No Upcoming ${it.label} Matches" }
                            ?: "No Upcoming Free Fire Brackets",
                        subtitle = if (selectedMode != null) {
                            "No ${selectedMode.label} tournaments are open right now. Switch modes or tap Show All."
                        } else {
                            "New Solo, Duo, Squad, Lone Wolf, and 1v1 lobbies appear here once scheduled."
                        },
                        variant = EmptyStateVariant.MATCHES
                    )
                }
            } else if (tournamentsState is UiState.Success) {
                items(upcoming, key = { it.id }) { tournament ->
                    TournamentCard(
                        tournament = tournament,
                        isJoined = tournament.id in joinedIds,
                        onClick = { onPreviewTournament(tournament) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun PlayerSummaryBanner(
    profile: PlayerProfile?,
    onProfileClick: () -> Unit,
    onSupportClick: () -> Unit
) {
    val displayName = profile?.inGameName?.ifBlank { profile.fullName }?.ifBlank { "Free Fire Player" }
        ?: "Loading Player..."
    val uidSummary = when {
        profile == null -> "Syncing profile..."
        profile.freeFireId.isBlank() -> "Tap Profile to set Free Fire UID & IGN • LVL ${profile.freeFireLevel}"
        else -> "FF UID: ${profile.freeFireId} • LVL ${profile.freeFireLevel}"
    }

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = GunmetalCard,
        border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(GunmetalCard, CrimsonDark.copy(alpha = 0.35f))
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onProfileClick)
                    .testTag("home_profile_summary"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_avatar_player),
                    contentDescription = "Player Avatar",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .border(2.dp, CrimsonRed, CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleLarge,
                        color = CrispWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = uidSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (profile?.freeFireId?.isBlank() == true) NeonScarlet else CoolSlate,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = "Open Profile",
                    tint = CoolSlate
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onSupportClick)
                    .testTag("home_support_button"),
                color = CarbonSurface,
                border = BorderStroke(1.dp, SteelBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GunmetalElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SupportAgent,
                                contentDescription = "Support",
                                tint = CrispWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("MATCH SUPPORT", style = MaterialTheme.typography.labelSmall, color = CoolSlate)
                            Text(
                                text = "Live Desk & Complaint Tickets",
                                style = MaterialTheme.typography.titleSmall,
                                color = CrispWhite,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = CoolSlate
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeModeFilterBar(
    tournaments: List<Tournament>,
    selectedMode: GameMode?,
    onSelectMode: (GameMode?) -> Unit
) {
    val activeCount = tournaments.count { it.statusEnum != TournamentStatus.COMPLETED }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "FILTER BY MODE",
            style = MaterialTheme.typography.labelMedium,
            color = CoolSlate
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                HomeFilterChip(
                    label = "All",
                    count = activeCount,
                    icon = Icons.Filled.SportsEsports,
                    selected = selectedMode == null,
                    onClick = { onSelectMode(null) },
                    tag = "home_filter_ALL"
                )
            }
            items(GameMode.entries) { mode ->
                val count = tournaments.count {
                    it.modeEnum == mode && it.statusEnum != TournamentStatus.COMPLETED
                }
                HomeFilterChip(
                    label = mode.label,
                    count = count,
                    icon = when (mode) {
                        GameMode.SOLO -> Icons.Filled.Person
                        GameMode.DUO -> Icons.Filled.Group
                        GameMode.SQUAD -> Icons.Filled.Groups
                        GameMode.LONE_WOLF -> Icons.Filled.Shield
                        GameMode.ONE_V_ONE -> Icons.Filled.SportsMma
                    },
                    selected = selectedMode == mode,
                    onClick = { onSelectMode(mode) },
                    tag = "home_filter_${mode.wireValue}"
                )
            }
        }
    }
}

@Composable
private fun HomeFilterChip(
    label: String,
    count: Int,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) CrimsonRed else GunmetalCard,
        border = BorderStroke(1.dp, if (selected) NeonScarlet else SteelBorder),
        modifier = Modifier
            .height(48.dp)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) CrispWhite else CrimsonRed,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = CrispWhite,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
            Surface(
                shape = CircleShape,
                color = if (selected) CrispWhite.copy(alpha = 0.22f) else CarbonSurface
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) CrispWhite else CoolSlate,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(18.dp)
                    .clip(CircleShape)
                    .background(CrimsonRed)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = CrispWhite
            )
        }
        TextButton(onClick = onActionClick) {
            Text(text = actionLabel, color = NeonScarlet, style = MaterialTheme.typography.labelMedium)
        }
    }
}
