package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.SupportMessage
import com.example.model.SupportTicket
import com.example.model.TicketCategory
import com.example.model.TicketStatus
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
import com.example.ui.theme.GunmetalElevated
import com.example.ui.theme.NeonScarlet
import com.example.ui.theme.SteelBorder

@Composable
fun SupportScreen(
    ticketsState: UiState<List<SupportTicket>>,
    selectedTicketMessagesState: UiState<List<SupportMessage>>,
    isBusy: Boolean,
    onSelectTicket: (String?) -> Unit,
    onCreateTicket: (subject: String, category: TicketCategory, tournamentName: String, description: String) -> Unit,
    onSendMessage: (ticketId: String, body: String) -> Unit,
    onResolveTicket: (ticketId: String) -> Unit,
    onBack: (() -> Unit)? = null
) {
    if (onBack != null) {
        BackHandler(onBack = onBack)
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0: Live Chat, 1: New Ticket, 2: Ticket Status
    var activeTicketId by rememberSaveable { mutableStateOf<String?>(null) }

    val tickets = (ticketsState as? UiState.Success)?.data.orEmpty()
    LaunchedEffect(tickets, activeTicketId) {
        if (activeTicketId == null && tickets.isNotEmpty()) {
            activeTicketId = tickets.first().id
            onSelectTicket(activeTicketId)
        } else if (activeTicketId != null) {
            onSelectTicket(activeTicketId)
        }
    }

    CenteredResponsiveColumn {
        Column(modifier = Modifier.fillMaxSize()) {
            if (onBack != null) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(top = 20.dp, start = 12.dp, end = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CrispWhite)
                    }
                    Text("LOBBIX SUPPORT", style = MaterialTheme.typography.headlineMedium, color = CrispWhite)
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CarbonSurface,
                contentColor = CrispWhite,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = CrimsonRed
                        )
                    }
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Live Chat", style = MaterialTheme.typography.labelLarge) },
                    modifier = Modifier.testTag("support_tab_chat")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Create Ticket", style = MaterialTheme.typography.labelLarge) },
                    modifier = Modifier.testTag("support_tab_create")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Tickets (${tickets.size})", style = MaterialTheme.typography.labelLarge) },
                    modifier = Modifier.testTag("support_tab_status")
                )
            }

            when (selectedTab) {
                0 -> {
                    val activeTicket = tickets.firstOrNull { it.id == activeTicketId } ?: tickets.firstOrNull()
                    LiveChatTab(
                        activeTicket = activeTicket,
                        messagesState = selectedTicketMessagesState,
                        onOpenCreateTicket = { selectedTab = 1 },
                        onSendMessage = { body ->
                            if (activeTicket != null) {
                                onSendMessage(activeTicket.id, body)
                            }
                        },
                        onResolveTicket = {
                            if (activeTicket != null) {
                                onResolveTicket(activeTicket.id)
                            }
                        }
                    )
                }

                1 -> {
                    CreateTicketTab(
                        isBusy = isBusy,
                        onSubmit = { subject, category, tournamentName, description ->
                            onCreateTicket(subject, category, tournamentName, description)
                            selectedTab = 0
                        }
                    )
                }

                else -> {
                    TicketStatusTab(
                        ticketsState = ticketsState,
                        onOpenTicketChat = { ticketId ->
                            activeTicketId = ticketId
                            onSelectTicket(ticketId)
                            selectedTab = 0
                        },
                        onCreateNew = { selectedTab = 1 }
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveChatTab(
    activeTicket: SupportTicket?,
    messagesState: UiState<List<SupportMessage>>,
    onOpenCreateTicket: () -> Unit,
    onSendMessage: (String) -> Unit,
    onResolveTicket: () -> Unit
) {
    var messageInput by remember { mutableStateOf("") }

    if (activeTicket == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            EmptyArenaCard(
                title = "No Active Support Chat",
                subtitle = "Open a Match Support or Complaint ticket to start a real-time support thread.",
                variant = EmptyStateVariant.SUPPORT
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onOpenCreateTicket,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("empty_chat_create_ticket"),
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.AddCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("CREATE SUPPORT TICKET", style = MaterialTheme.typography.labelLarge)
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = GunmetalCard,
            border = BorderStroke(1.dp, SteelBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(activeTicket.subject, style = MaterialTheme.typography.titleSmall, color = CrispWhite)
                    Text(
                        text = "${activeTicket.categoryEnum.label} • ${activeTicket.statusEnum.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CoolSlate
                    )
                }
                if (activeTicket.statusEnum != TicketStatus.RESOLVED) {
                    OutlinedButton(
                        onClick = onResolveTicket,
                        border = BorderStroke(1.dp, EmeraldLive.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = EmeraldLive, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resolve", color = EmeraldLive, style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    StatusPill(text = "RESOLVED", containerColor = EmeraldLive.copy(alpha = 0.18f), contentColor = EmeraldLive)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (messagesState) {
                is UiState.Loading -> {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = CrimsonRed)
                        }
                    }
                }

                is UiState.Error -> {
                    item { Text(messagesState.message, color = NeonScarlet) }
                }

                is UiState.Success -> {
                    items(messagesState.data, key = { it.id }) { msg ->
                        val isPlayer = msg.senderRole == "PLAYER"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isPlayer) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isPlayer) CrimsonRed else GunmetalElevated,
                                border = BorderStroke(1.dp, if (isPlayer) NeonScarlet else SteelBorder),
                                modifier = Modifier.fillMaxWidth(0.84f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = msg.senderName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isPlayer) CrispWhite.copy(alpha = 0.85f) else GoldTrophy
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = msg.body,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = CrispWhite
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CarbonSurface)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageInput,
                onValueChange = { messageInput = it },
                placeholder = { Text("Message LOBBIX Support...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("support_chat_input"),
                colors = lobbixFieldColors(),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    val trimmed = messageInput.trim()
                    if (trimmed.isNotEmpty()) {
                        messageInput = ""
                        onSendMessage(trimmed)
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CrimsonRed)
                    .testTag("support_chat_send_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message", tint = CrispWhite)
            }
        }
    }
}

@Composable
private fun CreateTicketTab(
    isBusy: Boolean,
    onSubmit: (String, TicketCategory, String, String) -> Unit
) {
    var category by remember { mutableStateOf(TicketCategory.MATCH_SUPPORT) }
    var subject by remember { mutableStateOf("") }
    var tournamentName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TacticalCard(accentBorder = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ReportProblem, contentDescription = null, tint = CrimsonRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CREATE COMPLAINT / MATCH SUPPORT", style = MaterialTheme.typography.titleLarge, color = CrispWhite)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(TicketCategory.MATCH_SUPPORT, TicketCategory.COMPLAINT, TicketCategory.ROOM_ISSUE).forEach { item ->
                        FilterChip(
                            selected = category == item,
                            onClick = { category = item },
                            label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CrimsonRed,
                                selectedLabelColor = CrispWhite,
                                containerColor = CarbonSurface,
                                labelColor = CoolSlate
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it; errorMsg = null },
                    label = { Text("Subject (e.g., Hacker report / Room slot issue)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ticket_subject_input"),
                    colors = lobbixFieldColors()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = tournamentName,
                    onValueChange = { tournamentName = it },
                    label = { Text("Tournament Name (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = lobbixFieldColors()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it; errorMsg = null },
                    label = { Text("Describe issue or match complaint") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ticket_description_input"),
                    colors = lobbixFieldColors()
                )
                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorMsg!!, color = NeonScarlet, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        when {
                            subject.trim().length < 3 -> errorMsg = "Subject must be at least 3 characters."
                            description.trim().length < 5 -> errorMsg = "Please describe your issue (min 5 chars)."
                            else -> {
                                onSubmit(subject.trim(), category, tournamentName.trim(), description.trim())
                                subject = ""
                                tournamentName = ""
                                description = ""
                            }
                        }
                    },
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_ticket_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
                ) {
                    Icon(Icons.Filled.SupportAgent, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SUBMIT SUPPORT TICKET", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun TicketStatusTab(
    ticketsState: UiState<List<SupportTicket>>,
    onOpenTicketChat: (String) -> Unit,
    onCreateNew: () -> Unit
) {
    when (ticketsState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CrimsonRed)
            }
        }

        is UiState.Error -> {
            Box(modifier = Modifier.padding(16.dp)) {
                TacticalCard { Text(ticketsState.message, color = NeonScarlet) }
            }
        }

        is UiState.Success -> {
            val list = ticketsState.data
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (list.isEmpty()) {
                    item {
                        EmptyArenaCard(
                            title = "No Support Tickets",
                            subtitle = "Need help with a Free Fire match or Room ID? Create a ticket anytime.",
                            variant = EmptyStateVariant.SUPPORT
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onCreateNew,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
                        ) {
                            Text("Create First Ticket")
                        }
                    }
                } else {
                    items(list, key = { it.id }) { ticket ->
                        val statusColor = when (ticket.statusEnum) {
                            TicketStatus.OPEN -> NeonScarlet
                            TicketStatus.IN_REVIEW -> AmberPending
                            TicketStatus.RESOLVED -> EmeraldLive
                        }
                        TacticalCard(
                            onClick = { onOpenTicketChat(ticket.id) },
                            modifier = Modifier.testTag("ticket_status_card_${ticket.id}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusPill(
                                    text = ticket.categoryEnum.label,
                                    containerColor = CrimsonTint,
                                    contentColor = NeonScarlet
                                )
                                StatusPill(
                                    text = ticket.statusEnum.label,
                                    containerColor = statusColor.copy(alpha = 0.16f),
                                    contentColor = statusColor
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(ticket.subject, style = MaterialTheme.typography.titleMedium, color = CrispWhite)
                            if (ticket.tournamentName.isNotBlank()) {
                                Text("Match: ${ticket.tournamentName}", style = MaterialTheme.typography.labelSmall, color = CoolSlate)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(ticket.description, style = MaterialTheme.typography.bodySmall, color = CoolSlate, maxLines = 2)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Forum, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open Live Chat Thread", style = MaterialTheme.typography.labelMedium, color = CrimsonRed)
                            }
                        }
                    }
                }
            }
        }
    }
}
