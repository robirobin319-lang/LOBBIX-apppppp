package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.LeaderboardEntry
import com.example.model.UiState
import com.example.ui.components.CenteredResponsiveColumn
import com.example.ui.components.EmptyArenaCard
import com.example.ui.components.StatusPill
import com.example.ui.components.TacticalCard
import com.example.ui.theme.BronzeRank
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.CrimsonTint
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.GoldTrophy
import com.example.ui.theme.GunmetalCard
import com.example.ui.theme.GunmetalElevated
import com.example.ui.theme.NeonScarlet
import com.example.ui.theme.SilverRank
import com.example.ui.theme.SteelBorder

@Composable
fun LeaderboardScreen(
    leaderboardState: UiState<List<LeaderboardEntry>>
) {
    CenteredResponsiveColumn {
        when (leaderboardState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CrimsonRed)
                }
            }

            is UiState.Error -> {
                Box(modifier = Modifier.padding(16.dp)) {
                    TacticalCard { Text(leaderboardState.message, color = NeonScarlet) }
                }
            }

            is UiState.Success -> {
                val entries = leaderboardState.data
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        TacticalCard(accentBorder = true) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    StatusPill(
                                        text = "SEASON 1 • FREE FIRE",
                                        containerColor = CrimsonTint,
                                        contentColor = NeonScarlet,
                                        icon = Icons.Filled.EmojiEvents
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "PRO RANKINGS",
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = CrispWhite
                                    )
                                    Text(
                                        text = "Verified kills, Booyah wins, and recent match results.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CoolSlate
                                    )
                                }
                            }
                        }
                    }

                    if (entries.isEmpty()) {
                        item {
                            EmptyArenaCard(
                                title = "Season Standings Updating",
                                subtitle = "Official Free Fire leaderboard results will sync here after match verification."
                            )
                        }
                    } else {
                        val topThree = entries.take(3)
                        if (topThree.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    topThree.forEach { entry ->
                                        PodiumCard(
                                            entry = entry,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        items(entries, key = { it.id }) { entry ->
                            LeaderboardRowCard(entry = entry)
                        }
                    }

                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun PodiumCard(
    entry: LeaderboardEntry,
    modifier: Modifier = Modifier
) {
    val rankColor = when (entry.rank) {
        1L -> GoldTrophy
        2L -> SilverRank
        else -> BronzeRank
    }
    Surface(
        modifier = modifier.testTag("podium_rank_${entry.rank}"),
        shape = RoundedCornerShape(16.dp),
        color = GunmetalCard,
        border = BorderStroke(1.dp, rankColor.copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(rankColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#${entry.rank}",
                    style = MaterialTheme.typography.titleMedium,
                    color = rankColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = entry.playerName,
                style = MaterialTheme.typography.titleSmall,
                color = CrispWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = entry.points.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = rankColor
            )
            Text(
                text = "${entry.kills} Kills • ${entry.wins}W",
                style = MaterialTheme.typography.labelSmall,
                color = CoolSlate
            )
        }
    }
}

@Composable
private fun LeaderboardRowCard(entry: LeaderboardEntry) {
    val badgeColor: Color = when (entry.rank) {
        1L -> GoldTrophy
        2L -> SilverRank
        3L -> BronzeRank
        else -> CoolSlate
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = GunmetalCard,
        border = BorderStroke(1.dp, SteelBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("leaderboard_row_${entry.rank}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (entry.rank <= 3L) badgeColor.copy(alpha = 0.16f) else GunmetalElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${entry.rank}",
                        style = MaterialTheme.typography.titleMedium,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.playerName,
                        style = MaterialTheme.typography.titleMedium,
                        color = CrispWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "FF UID: ${entry.freeFireId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CoolSlate
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = entry.points.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = GoldTrophy,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${entry.matchesPlayed} Matches",
                        style = MaterialTheme.typography.labelSmall,
                        color = CoolSlate
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CarbonSurface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${entry.kills} Kills", style = MaterialTheme.typography.labelMedium, color = CrispWhite)
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = GoldTrophy, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${entry.wins} Wins", style = MaterialTheme.typography.labelMedium, color = CrispWhite)
                }
                Text(
                    text = entry.lastMatchResult,
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonScarlet,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
