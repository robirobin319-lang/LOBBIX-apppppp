package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Tournament
import com.example.model.TournamentStatus
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

enum class EmptyStateVariant {
    TROPHY,
    MATCHES,
    WALLET,
    SUPPORT,
    NOTIFICATIONS
}

@Composable
fun LobbixBrandLockup(
    modifier: Modifier = Modifier,
    logoSize: Dp = 36.dp,
    showTagline: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_lobbix_logo),
            contentDescription = "LOBBIX Logo",
            modifier = Modifier
                .size(logoSize)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, CrimsonRed.copy(alpha = 0.65f), RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "LOBB",
                    style = MaterialTheme.typography.headlineMedium,
                    color = CrispWhite,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "IX",
                    style = MaterialTheme.typography.headlineMedium,
                    color = CrimsonRed,
                    fontWeight = FontWeight.Bold
                )
            }
            if (showTagline) {
                Text(
                    text = "FREE FIRE ESPORTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = CoolSlate,
                    letterSpacing = 1.4.sp
                )
            }
        }
    }
}

@Composable
fun LobbixTopBar(
    title: String,
    walletBalance: Long,
    unreadNotificationCount: Int,
    onWalletClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MatteBlack.copy(alpha = 0.98f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 20.dp, start = 16.dp, end = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (title == "LOBBIX") {
                LobbixBrandLockup(
                    showTagline = true,
                    modifier = Modifier.weight(1f, fill = false)
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_lobbix_logo),
                        contentDescription = "LOBBIX Emblem",
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, CrimsonRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = CrispWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = GunmetalCard,
                    border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onWalletClick)
                        .testTag("top_wallet_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountBalanceWallet,
                            contentDescription = "Wallet Balance",
                            tint = GoldTrophy,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = walletBalance.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = CrispWhite,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onNotificationsClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("top_notifications_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadNotificationCount > 0) {
                                Badge(containerColor = CrimsonRed, contentColor = CrispWhite) {
                                    Text(
                                        text = if (unreadNotificationCount > 9) "9+" else unreadNotificationCount.toString(),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notifications",
                            tint = CrispWhite
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TacticalCard(
    modifier: Modifier = Modifier,
    accentBorder: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val borderColor = if (accentBorder) CrimsonRed.copy(alpha = 0.55f) else SteelBorder
    val cardModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
    } else {
        modifier.fillMaxWidth()
    }
    Card(
        modifier = cardModifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = GunmetalCard),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color = CrispWhite,
    icon: ImageVector? = null
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun FeaturedTournamentBanner(
    tournament: Tournament,
    isJoined: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bannerRes = if (tournament.bannerKey == "clash") R.drawable.img_hero_clash else R.drawable.img_hero_bermuda
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(onClick = onClick)
            .testTag("featured_tournament_${tournament.id}"),
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.65f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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
                                MatteBlack.copy(alpha = 0.72f),
                                MatteBlack.copy(alpha = 0.96f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusPill(
                            text = tournament.modeEnum.label,
                            containerColor = CrimsonRed,
                            contentColor = CrispWhite
                        )
                        if (tournament.statusEnum == TournamentStatus.LIVE) {
                            StatusPill(
                                text = "LIVE",
                                containerColor = EmeraldLive.copy(alpha = 0.2f),
                                contentColor = EmeraldLive,
                                icon = Icons.Filled.Bolt
                            )
                        }
                    }
                    if (isJoined) {
                        StatusPill(
                            text = "JOINED",
                            containerColor = EmeraldLive.copy(alpha = 0.22f),
                            contentColor = EmeraldLive,
                            icon = Icons.Filled.CheckCircle
                        )
                    }
                }

                Column {
                    Text(
                        text = tournament.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = CrispWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Free Fire • ${tournament.mapName} • ${tournament.scheduleLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CoolSlate,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = null,
                                tint = GoldTrophy,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PRIZE ${tournament.prizePool}",
                                style = MaterialTheme.typography.labelLarge,
                                color = GoldTrophy
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CrimsonRed
                        ) {
                            Text(
                                text = if (isJoined) "OPEN ROOM" else "VIEW MATCH",
                                style = MaterialTheme.typography.labelMedium,
                                color = CrispWhite,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TournamentCard(
    tournament: Tournament,
    isJoined: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (tournament.maxPlayers > 0) {
        (tournament.joinedCount.toFloat() / tournament.maxPlayers.toFloat()).coerceIn(0f, 1f)
    } else 0f

    TacticalCard(
        modifier = modifier.testTag("tournament_card_${tournament.id}"),
        accentBorder = isJoined || tournament.statusEnum == TournamentStatus.LIVE,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusPill(
                    text = tournament.modeEnum.label,
                    containerColor = CrimsonTint,
                    contentColor = NeonScarlet
                )
                StatusPill(
                    text = tournament.mapName,
                    containerColor = GunmetalElevated,
                    contentColor = CoolSlate
                )
            }
            if (isJoined) {
                StatusPill(
                    text = if (tournament.roomPublished) "ROOM LIVE" else "JOINED",
                    containerColor = EmeraldLive.copy(alpha = 0.18f),
                    contentColor = EmeraldLive,
                    icon = if (tournament.roomPublished) Icons.Filled.VpnKey else Icons.Filled.CheckCircle
                )
            } else {
                StatusPill(
                    text = tournament.statusEnum.label,
                    containerColor = if (tournament.statusEnum == TournamentStatus.LIVE) {
                        CrimsonRed
                    } else {
                        GunmetalElevated
                    },
                    contentColor = CrispWhite
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = tournament.title,
            style = MaterialTheme.typography.titleLarge,
            color = CrispWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Schedule,
                contentDescription = null,
                tint = CoolSlate,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = tournament.scheduleLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = CoolSlate
            )
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = if (isJoined && tournament.roomPublished) Icons.Filled.VpnKey else Icons.Filled.Lock,
                contentDescription = null,
                tint = if (isJoined && tournament.roomPublished) EmeraldLive else CoolSlate,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isJoined) {
                    if (tournament.roomPublished) "Room Ready" else "Awaiting Room"
                } else {
                    "Room Locked"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (isJoined && tournament.roomPublished) EmeraldLive else CoolSlate
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CarbonSurface)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricMiniStat("PRIZE POOL", "${tournament.prizePool}", GoldTrophy)
            MetricMiniStat("PER KILL", "${tournament.perKillReward}", CrispWhite)
            MetricMiniStat(
                "ENTRY",
                if (tournament.entryFee == 0L) "FREE" else "${tournament.entryFee}",
                if (tournament.entryFee == 0L) EmeraldLive else NeonScarlet
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Groups,
                    contentDescription = null,
                    tint = CoolSlate,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "${tournament.joinedCount} / ${tournament.maxPlayers} Joined",
                    style = MaterialTheme.typography.bodySmall,
                    color = CoolSlate
                )
            }
            Text(
                text = "${(tournament.maxPlayers - tournament.joinedCount).coerceAtLeast(0)} slots left",
                style = MaterialTheme.typography.labelSmall,
                color = CrimsonRed
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = CrimsonRed,
            trackColor = GunmetalElevated
        )
    }
}

@Composable
private fun MetricMiniStat(label: String, value: String, valueColor: Color) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = CoolSlate
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = valueColor
        )
    }
}

@Composable
fun EmptyArenaCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    variant: EmptyStateVariant = EmptyStateVariant.TROPHY
) {
    TacticalCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (variant) {
                EmptyStateVariant.TROPHY -> {
                    Image(
                        painter = painterResource(id = R.drawable.img_empty_trophy),
                        contentDescription = title,
                        modifier = Modifier
                            .size(116.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                EmptyStateVariant.MATCHES -> {
                    TacticalEmblemGraphic(
                        icon = Icons.Filled.SportsEsports,
                        primaryColor = CrimsonRed,
                        secondaryColor = NeonScarlet,
                        contentDescription = title
                    )
                }

                EmptyStateVariant.WALLET -> {
                    TacticalEmblemGraphic(
                        icon = Icons.Filled.ReceiptLong,
                        primaryColor = GoldTrophy,
                        secondaryColor = EmeraldLive,
                        contentDescription = title
                    )
                }

                EmptyStateVariant.SUPPORT -> {
                    TacticalEmblemGraphic(
                        icon = Icons.Filled.SupportAgent,
                        primaryColor = NeonScarlet,
                        secondaryColor = AmberPending,
                        contentDescription = title
                    )
                }

                EmptyStateVariant.NOTIFICATIONS -> {
                    TacticalEmblemGraphic(
                        icon = Icons.Filled.NotificationsActive,
                        primaryColor = CrimsonRed,
                        secondaryColor = GoldTrophy,
                        contentDescription = title
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = CrispWhite,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = CoolSlate,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TacticalEmblemGraphic(
    icon: ImageVector,
    primaryColor: Color,
    secondaryColor: Color,
    contentDescription: String
) {
    Box(
        modifier = Modifier
            .size(104.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(CarbonSurface)
            .border(1.dp, primaryColor.copy(alpha = 0.4f), RoundedCornerShape(22.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerR = size.minDimension * 0.38f
            val innerR = size.minDimension * 0.25f

            drawCircle(
                color = primaryColor.copy(alpha = 0.14f),
                radius = outerR,
                center = center
            )
            drawCircle(
                color = primaryColor.copy(alpha = 0.45f),
                radius = outerR,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = secondaryColor.copy(alpha = 0.35f),
                radius = innerR,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
            // Tactical corner crosshair ticks
            val tick = 10.dp.toPx()
            drawLine(
                color = primaryColor.copy(alpha = 0.65f),
                start = Offset(center.x, 8.dp.toPx()),
                end = Offset(center.x, 8.dp.toPx() + tick),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = primaryColor.copy(alpha = 0.65f),
                start = Offset(center.x, size.height - 8.dp.toPx()),
                end = Offset(center.x, size.height - 8.dp.toPx() - tick),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = secondaryColor.copy(alpha = 0.65f),
                start = Offset(8.dp.toPx(), center.y),
                end = Offset(8.dp.toPx() + tick, center.y),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = secondaryColor.copy(alpha = 0.65f),
                start = Offset(size.width - 8.dp.toPx(), center.y),
                end = Offset(size.width - 8.dp.toPx() - tick, center.y),
                strokeWidth = 2.dp.toPx()
            )
        }
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = primaryColor,
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
fun CenteredResponsiveColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp),
            content = content
        )
    }
}
