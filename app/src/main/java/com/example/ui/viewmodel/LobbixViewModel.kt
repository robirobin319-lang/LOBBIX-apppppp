package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LobbixRepository
import com.example.model.AppNotification
import com.example.model.DepositRequest
import com.example.model.JoinedTournamentRecord
import com.example.model.LeaderboardEntry
import com.example.model.PaymentConfig
import com.example.model.PlayerProfile
import com.example.model.RoomCredentials
import com.example.model.SupportMessage
import com.example.model.SupportTicket
import com.example.model.TicketCategory
import com.example.model.Tournament
import com.example.model.UiState
import com.example.model.WalletTransaction
import com.example.model.WithdrawalRequest
import com.example.notifications.MatchNotificationHelper
import com.example.notifications.MatchReminderScheduler
import com.example.notifications.MatchScheduleParser
import com.example.notifications.ReminderScheduleResult
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class LobbixViewModel(
    private val appContext: Application,
    private val repository: LobbixRepository,
    private val currentUserId: String,
    private val pendingFullName: String? = null,
    private val pendingPhone: String? = null
) : ViewModel() {

    private val _selectedTournamentId = MutableStateFlow<String?>(null)
    private val _selectedTicketId = MutableStateFlow<String?>(null)

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val processedRefundIds = mutableSetOf<String>()

    val playerProfile: StateFlow<UiState<PlayerProfile?>> =
        repository.observePlayerProfile(currentUserId)
            .toUiStateFlow("Failed to load player profile")

    val paymentConfig: StateFlow<UiState<PaymentConfig>> =
        repository.observePaymentConfig()
            .toUiStateFlow("Failed to load payment details")

    val depositRequests: StateFlow<UiState<List<DepositRequest>>> =
        repository.observeDepositRequests(currentUserId)
            .toUiStateFlow("Failed to load deposit requests")

    val withdrawalRequests: StateFlow<UiState<List<WithdrawalRequest>>> =
        repository.observeWithdrawalRequests(currentUserId)
            .toUiStateFlow("Failed to load withdrawal requests")

    val tournaments: StateFlow<UiState<List<Tournament>>> =
        repository.observeTournaments()
            .toUiStateFlow("Failed to load Free Fire tournaments")

    val joinedTournaments: StateFlow<UiState<List<JoinedTournamentRecord>>> =
        repository.observeJoinedTournaments(currentUserId)
            .toUiStateFlow("Failed to load joined tournaments")

    val leaderboard: StateFlow<UiState<List<LeaderboardEntry>>> =
        repository.observeLeaderboard()
            .toUiStateFlow("Failed to load leaderboard")

    val transactions: StateFlow<UiState<List<WalletTransaction>>> =
        repository.observeTransactions(currentUserId)
            .toUiStateFlow("Failed to load wallet history")

    val notifications: StateFlow<UiState<List<AppNotification>>> =
        repository.observeNotifications(currentUserId)
            .toUiStateFlow("Failed to load notifications")

    val tickets: StateFlow<UiState<List<SupportTicket>>> =
        repository.observeTickets(currentUserId)
            .toUiStateFlow("Failed to load support tickets")

    init {
        MatchNotificationHelper.ensureChannel(appContext)

        viewModelScope.launch {
            repository.ensurePlayerSetup(pendingFullName, pendingPhone)
                .onFailure { error ->
                    Log.w(TAG, "Profile bootstrap failed", error)
                }
        }

        // Automatically process one-time refunds for any Admin-rejected withdrawal requests
        viewModelScope.launch {
            withdrawalRequests.collect { state ->
                val list = (state as? UiState.Success)?.data ?: return@collect
                list.filter { it.status == "Rejected" && !it.refundProcessed && it.id.isNotBlank() }
                    .forEach { req ->
                        if (processedRefundIds.add(req.id)) {
                            repository.processRejectedWithdrawalRefund(req.id)
                                .onSuccess { refunded ->
                                    if (refunded > 0L) {
                                        _actionMessage.value =
                                            "Withdrawal rejected by admin. $refunded refunded to your wallet."
                                    }
                                }
                                .onFailure { err ->
                                    processedRefundIds.remove(req.id)
                                    Log.w(TAG, "Auto-refund failed for ${req.id}", err)
                                }
                        }
                    }
            }
        }

        // Continuously keep 10-minute pre-match alerts armed and sync Room ID + Password alerts
        val reminderTicker: Flow<Long> = flow {
            while (true) {
                emit(System.currentTimeMillis())
                delay(30_000L)
            }
        }
        viewModelScope.launch {
            combine(joinedTournaments, tournaments, reminderTicker) { joinedState, tourneyState, now ->
                Triple(joinedState, tourneyState, now)
            }.collect { (joinedState, tourneyState, now) ->
                val joinedList = (joinedState as? UiState.Success)?.data ?: return@collect
                val allTourneys = (tourneyState as? UiState.Success)?.data.orEmpty()
                val tourneyById = allTourneys.associateBy { it.id }

                val credsMap = mutableMapOf<String, RoomCredentials?>()
                joinedList.forEach { record ->
                    val tourney = tourneyById[record.tournamentId]
                    val shouldFetchCreds = tourney == null || tourney.roomPublished
                    credsMap[record.tournamentId] = if (shouldFetchCreds) {
                        repository.fetchRoomCredentialsOnce(record.tournamentId)
                    } else {
                        null
                    }
                }

                MatchReminderScheduler.syncJoinedReminders(
                    context = appContext,
                    joinedRecords = joinedList,
                    tournaments = allTourneys,
                    roomCredentialsByTournament = credsMap
                )

                joinedList.forEach { record ->
                    val tourney = tourneyById[record.tournamentId] ?: return@forEach
                    val startMillis = MatchScheduleParser.resolveStartTimeMillis(
                        scheduleLabel = tourney.scheduleLabel,
                        explicitStartMillis = tourney.startTimeMillis,
                        nowMillis = now
                    )
                    if (startMillis != null &&
                        MatchScheduleParser.isInTenMinutePreStartWindow(startMillis, now)
                    ) {
                        repository.upsertMatchReminderNotification(
                            tournament = tourney,
                            roomCredentials = credsMap[record.tournamentId]
                        )
                    }
                }
            }
        }
    }

    val selectedTournament: StateFlow<UiState<Tournament?>> =
        _selectedTournamentId
            .flatMapLatest { id ->
                if (id.isNullOrBlank()) flowOf(null)
                else repository.observeTournamentById(id)
            }
            .toUiStateFlow("Failed to load tournament details")

    val isJoinedSelectedTournament: StateFlow<Boolean> =
        _selectedTournamentId
            .flatMapLatest { id ->
                if (id.isNullOrBlank()) flowOf(false)
                else repository.observeParticipantStatus(id, currentUserId)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                Log.w(TAG, "Participant status error", firestoreError ?: error)
                emit(false)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = false
            )

    // Only attaches room_credentials listener when player IS a verified participant!
    val selectedRoomCredentials: StateFlow<UiState<RoomCredentials?>> =
        _selectedTournamentId
            .flatMapLatest { id ->
                if (id.isNullOrBlank()) {
                    flowOf<UiState<RoomCredentials?>>(UiState.Success(null))
                } else {
                    repository.observeParticipantStatus(id, currentUserId).flatMapLatest { joined ->
                        if (!joined) {
                            flowOf<UiState<RoomCredentials?>>(UiState.Success(null))
                        } else {
                            repository.observeRoomCredentials(id)
                                .map<RoomCredentials?, UiState<RoomCredentials?>> { UiState.Success(it) }
                                .catch { error ->
                                    val firestoreError = generateSequence(error) { it.cause }
                                        .filterIsInstance<FirebaseFirestoreException>()
                                        .firstOrNull()
                                    if (firestoreError == null && error is CancellationException) throw error
                                    val rootError = firestoreError ?: error
                                    Log.w(TAG, "Room credentials error", rootError)
                                    emit(UiState.Error(rootError.message ?: "Room details unavailable"))
                                }
                        }
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val selectedTicketMessages: StateFlow<UiState<List<SupportMessage>>> =
        _selectedTicketId
            .flatMapLatest { ticketId ->
                if (ticketId.isNullOrBlank()) flowOf(emptyList())
                else repository.observeTicketMessages(currentUserId, ticketId)
            }
            .toUiStateFlow("Failed to load chat messages")

    fun selectTournament(tournamentId: String) {
        _selectedTournamentId.value = tournamentId
    }

    fun selectTicket(ticketId: String?) {
        _selectedTicketId.value = ticketId
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun joinTournament(
        tournament: Tournament,
        overrideIgn: String = "",
        overrideFfUid: String = "",
        teamTag: String = ""
    ) {
        val currentProfile = (playerProfile.value as? UiState.Success)?.data ?: PlayerProfile(
            userId = currentUserId,
            fullName = "Free Fire Player"
        )
        viewModelScope.launch {
            _isBusy.value = true
            repository.joinTournament(
                tournament = tournament,
                profile = currentProfile,
                overrideIgn = overrideIgn,
                overrideFfUid = overrideFfUid,
                teamTag = teamTag
            )
                .onSuccess {
                    val creds = if (tournament.roomPublished) {
                        repository.fetchRoomCredentialsOnce(tournament.id)
                    } else null
                    val schedResult = MatchReminderScheduler.scheduleForTournament(
                        context = appContext,
                        tournament = tournament,
                        roomCredentials = creds
                    )
                    _actionMessage.value = when (schedResult) {
                        is ReminderScheduleResult.Scheduled -> {
                            val at = MatchScheduleParser.formatLocalTime(schedResult.triggerAtMillis)
                            "Joined ${tournament.title}! 10m Room ID alert set for $at."
                        }
                        is ReminderScheduleResult.ActiveWindow -> {
                            repository.upsertMatchReminderNotification(tournament, creds)
                            "Joined ${tournament.title}! Match starts soon — Room alert dispatched."
                        }
                        else -> "Joined ${tournament.title}! Room details unlock once published."
                    }
                }
                .onFailure { error ->
                    _actionMessage.value = error.localizedMessage ?: "Unable to join tournament."
                }
            _isBusy.value = false
        }
    }

    fun sendRoomReminderNotificationNow(tournament: Tournament, roomCredentials: RoomCredentials?) {
        viewModelScope.launch {
            val latestCreds = roomCredentials ?: repository.fetchRoomCredentialsOnce(tournament.id)
            val shown = MatchNotificationHelper.showRoomReminderNotification(
                context = appContext,
                tournamentId = tournament.id,
                tournamentTitle = tournament.title,
                gameMode = tournament.modeEnum.label,
                mapName = tournament.mapName,
                scheduleLabel = tournament.scheduleLabel,
                roomId = latestCreds?.roomId.orEmpty(),
                roomPassword = latestCreds?.roomPassword.orEmpty(),
                serverRegion = latestCreds?.serverRegion.orEmpty()
            )
            repository.upsertMatchReminderNotification(tournament, latestCreds)
            _actionMessage.value = if (shown) {
                if (latestCreds != null) {
                    "Room notification sent (ID: ${latestCreds.roomId})."
                } else {
                    "10-min reminder sent. Room ID will populate once published by Admin."
                }
            } else {
                "Enable system notification permission to display alerts in your notification shade."
            }
        }
    }

    fun updateProfile(
        fullName: String,
        inGameName: String,
        phone: String,
        freeFireId: String,
        freeFireLevel: Long,
        avatarPreset: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.updatePlayerProfile(
                fullName = fullName,
                inGameName = inGameName,
                phone = phone,
                freeFireId = freeFireId,
                freeFireLevel = freeFireLevel,
                avatarPreset = avatarPreset
            )
                .onSuccess { _actionMessage.value = "Free Fire profile updated." }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Update failed." }
            _isBusy.value = false
        }
    }

    fun submitDepositRequest(
        amount: Long,
        utrNumber: String,
        adminUpiId: String,
        screenshotName: String,
        screenshotPreviewBase64: String,
        onSubmitted: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.submitDepositRequest(
                amount = amount,
                utrNumber = utrNumber,
                adminUpiId = adminUpiId,
                screenshotName = screenshotName,
                screenshotPreviewBase64 = screenshotPreviewBase64
            )
                .onSuccess {
                    _actionMessage.value =
                        "Deposit request submitted (Pending). Wallet updates after Admin verification."
                    onSubmitted()
                }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Deposit submission failed." }
            _isBusy.value = false
        }
    }

    fun submitWithdrawalRequest(
        upiId: String,
        amount: Long,
        onSubmitted: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.submitWithdrawalRequest(upiId = upiId, amount = amount)
                .onSuccess {
                    _actionMessage.value =
                        "Withdrawal request submitted (Pending). Amount locked until Admin review."
                    onSubmitted()
                }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Withdrawal failed." }
            _isBusy.value = false
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(notificationId)
        }
    }

    fun createSupportTicket(
        subject: String,
        category: TicketCategory,
        tournamentName: String,
        description: String
    ) {
        val profile = (playerProfile.value as? UiState.Success)?.data
        val playerName = profile?.inGameName?.ifBlank { profile.fullName } ?: "Player"
        viewModelScope.launch {
            _isBusy.value = true
            repository.createSupportTicket(playerName, subject, category, tournamentName, description)
                .onSuccess { newTicketId ->
                    _selectedTicketId.value = newTicketId
                    _actionMessage.value = "Support ticket opened. Live chat is ready."
                }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Could not create ticket." }
            _isBusy.value = false
        }
    }

    fun sendSupportMessage(ticketId: String, body: String) {
        val profile = (playerProfile.value as? UiState.Success)?.data
        val playerName = profile?.inGameName?.ifBlank { profile.fullName } ?: "Player"
        viewModelScope.launch {
            repository.sendSupportMessage(ticketId, playerName, body)
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to send message." }
        }
    }

    fun resolveTicket(ticketId: String) {
        viewModelScope.launch {
            repository.resolveSupportTicket(ticketId)
                .onSuccess { _actionMessage.value = "Ticket marked as Resolved." }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to resolve ticket." }
        }
    }

    private fun <T> Flow<T>.toUiStateFlow(fallbackMessage: String): StateFlow<UiState<T>> {
        return this
            .map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, fallbackMessage, rootError)
                emit(UiState.Error(rootError.message ?: fallbackMessage))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )
    }

    private companion object {
        const val TAG = "LobbixVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
