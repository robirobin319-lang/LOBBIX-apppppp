package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.GameMode
import com.example.model.Tournament
import com.example.model.TournamentStatus
import com.example.ui.theme.AmberPending
import com.example.ui.theme.BronzeRank
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
import com.example.ui.theme.SilverRank
import com.example.ui.theme.SteelBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentPreviewBottomSheet(
    tournament: Tournament,
    isJoined: Boolean,
    walletBalance: Long,
    onDismiss: () -> Unit,
    onOpenFullDetails: (String) -> Unit,
    onOpenWallet: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var expandedBreakdown by remember(tournament.id) { mutableStateOf(true) }

    val bannerRes = if (tournament.bannerKey == "clash") R.drawable.img_hero_clash else R.drawable.img_hero_bermuda
    val slotsProgress = if (tournament.maxPlayers > 0) {
        (tournament.joinedCount.toFloat() / tournament.maxPlayers.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val hasSufficientBalance = tournament.entryFee <= 0L || walletBalance >= tournament.entryFee

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GunmetalCard,
        contentColor = CrispWhite,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = SteelBorder)
        },
        modifier = Modifier.testTag("tournament_preview_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Hero Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(144.dp)
                    .clip(RoundedCornerShape(18.dp))
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
                                    MatteBlack.copy(alpha = 0.25f),
                                    MatteBlack.copy(alpha = 0.92f)
                                )
                            )
                        )
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MatteBlack.copy(alpha = 0.6f))
                        .testTag("sheet_close_button")
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close Preview", tint = CrispWhite)
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusPill(
                            text = "FREE FIRE • ${tournament.modeEnum.label}",
                            containerColor = CrimsonRed
                        )
                        StatusPill(
                            text = if (isJoined) "JOINED" else tournament.statusEnum.label,
                            containerColor = if (isJoined) EmeraldLive.copy(alpha = 0.22f) else GunmetalElevated,
                            contentColor = if (isJoined) EmeraldLive else CrispWhite,
                            icon = if (isJoined) Icons.Filled.CheckCircle else null
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = tournament.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = CrispWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, tint = CoolSlate, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = tournament.scheduleLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = CoolSlate
                        )
                    }
                }
            }

            // 2. Specific Map Details Card
            val mapDetails = remember(tournament.mapName, tournament.modeEnum) {
                mapProfileFor(tournament.mapName, tournament.modeEnum)
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CarbonSurface,
                border = BorderStroke(1.dp, SteelBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Map, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("MAP & ARENA DETAILS", style = MaterialTheme.typography.labelLarge, color = CrispWhite)
                        }
                        StatusPill(
                            text = tournament.mapName,
                            containerColor = CrimsonTint,
                            contentColor = NeonScarlet
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SheetInfoPill("FORMAT", tournament.modeEnum.teamSize, Modifier.weight(1f))
                        SheetInfoPill("PERSPECTIVE", mapDetails.perspective, Modifier.weight(1f))
                        SheetInfoPill("LOOT / ZONE", mapDetails.zoneTempo, Modifier.weight(1f))
                    }

                    Text(
                        text = mapDetails.tacticalNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = CoolSlate
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Groups, contentDescription = null, tint = CoolSlate, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "${tournament.joinedCount} / ${tournament.maxPlayers} Players Joined",
                                style = MaterialTheme.typography.bodySmall,
                                color = CrispWhite
                            )
                        }
                        Text(
                            text = "${(tournament.maxPlayers - tournament.joinedCount).coerceAtLeast(0)} slots open",
                            style = MaterialTheme.typography.labelSmall,
                            color = CrimsonRed
                        )
                    }
                    LinearProgressIndicator(
                        progress = { slotsProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = CrimsonRed,
                        trackColor = GunmetalElevated
                    )
                }
            }

            // 3. Expandable Prize Pool Breakdown
            val prizeBreakdown = remember(tournament.prizePool, tournament.perKillReward, tournament.modeEnum) {
                computePrizeBreakdown(tournament)
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CarbonSurface,
                border = BorderStroke(1.dp, GoldTrophy.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { expandedBreakdown = !expandedBreakdown }
                            .padding(vertical = 2.dp)
                            .testTag("sheet_prize_expand_toggle"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = GoldTrophy, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("PRIZE POOL BREAKDOWN", style = MaterialTheme.typography.labelLarge, color = CrispWhite)
                                Text(
                                    text = "Total Pool: ${tournament.prizePool} • Per Kill: ${tournament.perKillReward}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GoldTrophy
                                )
                            }
                        }
                        Icon(
                            imageVector = if (expandedBreakdown) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (expandedBreakdown) "Collapse Prize Breakdown" else "Expand Prize Breakdown",
                            tint = CoolSlate
                        )
                    }

                    AnimatedVisibility(visible = expandedBreakdown) {
                        Column(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HorizontalDivider(color = SteelBorder)
                            prizeBreakdown.forEach { tier ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(tier.accent)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = tier.label,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = CrispWhite
                                        )
                                    }
                                    Text(
                                        text = tier.amountLabel,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = tier.accent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (tournament.perKillReward > 0L) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.LocalFireDepartment,
                                            contentDescription = null,
                                            tint = CrimsonRed,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Elimination Bonus (Per Kill)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = CoolSlate
                                        )
                                    }
                                    Text(
                                        text = "${tournament.perKillReward}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = CrispWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Entry Fee Requirements & Eligibility
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CarbonSurface,
                border = BorderStroke(
                    1.dp,
                    if (hasSufficientBalance) EmeraldLive.copy(alpha = 0.4f) else AmberPending.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = GoldTrophy, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ENTRY FEE & REQUIREMENTS", style = MaterialTheme.typography.labelLarge, color = CrispWhite)
                        }
                        StatusPill(
                            text = if (tournament.entryFee == 0L) "FREE ENTRY" else "ENTRY ${tournament.entryFee}",
                            containerColor = if (tournament.entryFee == 0L) EmeraldLive.copy(alpha = 0.18f) else CrimsonTint,
                            contentColor = if (tournament.entryFee == 0L) EmeraldLive else NeonScarlet
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (tournament.entryFee == 0L) {
                                "No entry fee required. Join directly with your Free Fire UID."
                            } else if (hasSufficientBalance) {
                                "Wallet Ready • Available Balance: $walletBalance"
                            } else {
                                "Wallet Balance: $walletBalance (Need ${tournament.entryFee - walletBalance} more)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasSufficientBalance) EmeraldLive else AmberPending,
                            modifier = Modifier.weight(1f)
                        )
                        if (!hasSufficientBalance && !isJoined) {
                            TextButton(
                                onClick = {
                                    onDismiss()
                                    onOpenWallet()
                                },
                                modifier = Modifier.testTag("sheet_add_money_button")
                            ) {
                                Text("Add Money", color = NeonScarlet, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    HorizontalDivider(color = SteelBorder)
                    RequirementLine(
                        icon = Icons.Filled.Shield,
                        text = tournament.rulesSummary.ifBlank {
                            "Mobile devices only. Verified Free Fire UID & IGN required for room entry."
                        }
                    )
                    RequirementLine(
                        icon = if (isJoined && tournament.roomPublished) Icons.Filled.VpnKey else Icons.Filled.Lock,
                        text = if (isJoined) {
                            if (tournament.roomPublished) "Room ID & Password are unlocked for your slot."
                            else "Slot confirmed. Room ID unlocks when Admin publishes it."
                        } else {
                            "Room ID & Password remain hidden until you join this tournament."
                        }
                    )
                }
            }

            // 5. Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(0.36f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SteelBorder)
                ) {
                    Text("Close", color = CoolSlate, style = MaterialTheme.typography.labelLarge)
                }
                Button(
                    onClick = {
                        onDismiss()
                        onOpenFullDetails(tournament.id)
                    },
                    modifier = Modifier
                        .weight(0.64f)
                        .height(50.dp)
                        .testTag("sheet_open_tournament_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
                ) {
                    Text(
                        text = if (isJoined) "OPEN ROOM DETAILS" else "VIEW & JOIN MATCH",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetInfoPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = GunmetalCard,
        border = BorderStroke(1.dp, SteelBorder)
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = CoolSlate, maxLines = 1)
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = CrispWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RequirementLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = CoolSlate, modifier = Modifier.size(15.dp).padding(top = 2.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = CoolSlate
        )
    }
}

private data class MapArenaProfile(
    val perspective: String,
    val zoneTempo: String,
    val tacticalNote: String
)

private fun mapProfileFor(mapName: String, mode: GameMode): MapArenaProfile {
    val normalized = mapName.trim().lowercase()
    return when {
        "purgatory" in normalized -> MapArenaProfile(
            perspective = "TPP Mobile",
            zoneTempo = "Fast Crossings",
            tacticalNote = "Rivers and ziplines around Brasilia & Central favor disciplined mid-to-long-range rotations."
        )
        "kalahari" in normalized -> MapArenaProfile(
            perspective = "TPP Mobile",
            zoneTempo = "High Vertical",
            tacticalNote = "Desert cliffs, Refinery, and Stone Ridge reward high-ground control and gloo-wall positioning."
        )
        "iron" in normalized || mode == GameMode.LONE_WOLF -> MapArenaProfile(
            perspective = "Cage Arena",
            zoneTempo = "Best of 9",
            tacticalNote = "Compact Iron Cage duel arena with rotating weapon picks and zero outside interference."
        )
        "custom" in normalized || mode == GameMode.ONE_V_ONE -> MapArenaProfile(
            perspective = "Custom 1v1",
            zoneTempo = "Duel Rules",
            tacticalNote = "Custom Room head-to-head duel with unlimited gloo walls and verified Fair-Play loadout."
        )
        else -> MapArenaProfile(
            perspective = "TPP Mobile",
            zoneTempo = "Classic Br",
            tacticalNote = "Classic Bermuda island drop featuring Clock Tower, Peak, and Pochinok competitive endzones."
        )
    }
}

private data class PrizeTier(
    val label: String,
    val amountLabel: String,
    val accent: Color
)

private fun computePrizeBreakdown(tournament: Tournament): List<PrizeTier> {
    val pool = tournament.prizePool.coerceAtLeast(0L)
    if (tournament.modeEnum == GameMode.ONE_V_ONE || tournament.modeEnum == GameMode.LONE_WOLF) {
        val winner = (pool * 80L) / 100L
        val runner = pool - winner
        return listOf(
            PrizeTier("#1 Match Winner (Booyah)", winner.toString(), GoldTrophy),
            PrizeTier("#2 Finalist", runner.toString(), SilverRank)
        )
    }
    val first = (pool * 50L) / 100L
    val second = (pool * 25L) / 100L
    val third = (pool * 15L) / 100L
    val killPool = (pool - first - second - third).coerceAtLeast(0L)
    return listOf(
        PrizeTier("#1 Booyah Champion", first.toString(), GoldTrophy),
        PrizeTier("#2 Runner-Up", second.toString(), SilverRank),
        PrizeTier("#3 Third Place", third.toString(), BronzeRank),
        PrizeTier("MVP / Top Eliminator Pool", killPool.toString(), CrimsonRed)
    )
}
