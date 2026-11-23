package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class GameMode(val wireValue: String, val label: String, val subtitle: String, val teamSize: String) {
    SOLO("SOLO", "Solo", "1-Player Survival", "48 Slots"),
    DUO("DUO", "Duo", "2-Player Tactical", "24 Teams"),
    SQUAD("SQUAD", "Squad", "4-Player Clash", "12 Squads"),
    LONE_WOLF("LONE_WOLF", "Lone Wolf", "Iron Cage Duel", "1v1 / 2v2"),
    ONE_V_ONE("1V1_MATCH", "1v1 Match", "Custom Head-to-Head", "2 Players");

    companion object {
        fun fromWire(value: String?): GameMode =
            entries.firstOrNull { it.wireValue == value } ?: SOLO
    }
}

enum class TournamentStatus(val wireValue: String, val label: String) {
    UPCOMING("UPCOMING", "Upcoming"),
    LIVE("LIVE", "Live Now"),
    COMPLETED("COMPLETED", "Completed");

    companion object {
        fun fromWire(value: String?): TournamentStatus =
            entries.firstOrNull { it.wireValue == value } ?: UPCOMING
    }
}

enum class NotificationCategory(val wireValue: String, val label: String) {
    TOURNAMENT_UPDATE("TOURNAMENT_UPDATE", "Tournament Update"),
    ROOM_PUBLISHED("ROOM_PUBLISHED", "Room Details"),
    MATCH_REMINDER("MATCH_REMINDER", "Match Reminder"),
    RESULT_ANNOUNCED("RESULT_ANNOUNCED", "Result Announced");

    companion object {
        fun fromWire(value: String?): NotificationCategory =
            entries.firstOrNull { it.wireValue == value } ?: TOURNAMENT_UPDATE
    }
}

enum class TicketCategory(val wireValue: String, val label: String) {
    COMPLAINT("COMPLAINT", "Complaint"),
    MATCH_SUPPORT("MATCH_SUPPORT", "Match Support"),
    ROOM_ISSUE("ROOM_ISSUE", "Room Issue"),
    GENERAL("GENERAL", "General");

    companion object {
        fun fromWire(value: String?): TicketCategory =
            entries.firstOrNull { it.wireValue == value } ?: MATCH_SUPPORT
    }
}

enum class TicketStatus(val wireValue: String, val label: String) {
    OPEN("OPEN", "Open"),
    IN_REVIEW("IN_REVIEW", "In Review"),
    RESOLVED("RESOLVED", "Resolved");

    companion object {
        fun fromWire(value: String?): TicketStatus =
            entries.firstOrNull { it.wireValue == value } ?: OPEN
    }
}

@Keep
@IgnoreExtraProperties
data class PlayerProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val inGameName: String = "",
    val email: String = "",
    val phone: String = "",
    val freeFireId: String = "",
    val freeFireLevel: Long = 1L,
    val avatarPreset: String = "cyber_crimson",
    val walletBalance: Long = 0L,
    val lockedBalance: Long = 0L,
    val lastWithdrawalId: String = "",
    val totalMatches: Long = 0L,
    val totalWins: Long = 0L,
    val totalKills: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PaymentConfig(
    @DocumentId val id: String = "",
    val upiId: String = "lobbix.esports@okaxis",
    val merchantName: String = "LOBBIX Esports",
    val instruction: String = "Scan the QR code or copy the Admin UPI ID, complete payment, then submit UTR and screenshot.",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DepositRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val amount: Long = 0L,
    val utrNumber: String = "",
    val adminUpiId: String = "",
    val screenshotName: String = "",
    val screenshotPreviewBase64: String = "",
    val status: String = "Pending",
    val adminNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WithdrawalRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val upiId: String = "",
    val amount: Long = 0L,
    val status: String = "Pending",
    val refundProcessed: Boolean = false,
    val adminNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Tournament(
    @DocumentId val id: String = "",
    val title: String = "",
    val gameName: String = "Free Fire",
    val gameMode: String = GameMode.SOLO.wireValue,
    val mapName: String = "Bermuda",
    val scheduleLabel: String = "",
    val entryFee: Long = 0L,
    val prizePool: Long = 0L,
    val perKillReward: Long = 0L,
    val maxPlayers: Long = 48L,
    val joinedCount: Long = 0L,
    val status: String = TournamentStatus.UPCOMING.wireValue,
    val roomPublished: Boolean = false,
    val rulesSummary: String = "",
    val bannerKey: String = "bermuda",
    val startTimeMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val modeEnum: GameMode get() = GameMode.fromWire(gameMode)
    val statusEnum: TournamentStatus get() = TournamentStatus.fromWire(status)
}

@Keep
@IgnoreExtraProperties
data class TournamentParticipant(
    @DocumentId val id: String = "",
    val userId: String = "",
    val tournamentId: String = "",
    val playerName: String = "",
    val freeFireId: String = "",
    val teamTag: String = "",
    @ServerTimestamp val joinedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RoomCredentials(
    @DocumentId val id: String = "",
    val tournamentId: String = "",
    val roomId: String = "",
    val roomPassword: String = "",
    val serverRegion: String = "",
    val matchNote: String = "",
    @ServerTimestamp val publishedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class JoinedTournamentRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val tournamentId: String = "",
    val tournamentTitle: String = "",
    val gameMode: String = GameMode.SOLO.wireValue,
    val scheduleLabel: String = "",
    val mapName: String = "",
    val entryFee: Long = 0L,
    val prizePool: Long = 0L,
    val status: String = "JOINED",
    val placementLabel: String = "Pending",
    val killsLogged: Long = 0L,
    val startTimeMillis: Long = 0L,
    @ServerTimestamp val joinedAt: Timestamp? = null
) {
    val modeEnum: GameMode get() = GameMode.fromWire(gameMode)
}

@Keep
@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val playerId: String = "",
    val playerName: String = "",
    val freeFireId: String = "",
    val rank: Long = 1L,
    val kills: Long = 0L,
    val wins: Long = 0L,
    val matchesPlayed: Long = 0L,
    val points: Long = 0L,
    val lastMatchResult: String = "",
    val seasonLabel: String = "Season 1",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WalletTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val type: String = "DEPOSIT",
    val amount: Long = 0L,
    val status: String = "Pending",
    val referenceCode: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val category: String = NotificationCategory.TOURNAMENT_UPDATE.wireValue,
    val tournamentId: String = "",
    val readStatus: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val categoryEnum: NotificationCategory get() = NotificationCategory.fromWire(category)
}

@Keep
@IgnoreExtraProperties
data class SupportTicket(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subject: String = "",
    val category: String = TicketCategory.MATCH_SUPPORT.wireValue,
    val tournamentName: String = "",
    val description: String = "",
    val status: String = TicketStatus.OPEN.wireValue,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val categoryEnum: TicketCategory get() = TicketCategory.fromWire(category)
    val statusEnum: TicketStatus get() = TicketStatus.fromWire(status)
}

@Keep
@IgnoreExtraProperties
data class SupportMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val senderRole: String = "PLAYER",
    val senderName: String = "",
    val body: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
