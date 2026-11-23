package com.example.data

import android.content.Context
import com.example.R
import com.example.model.AppNotification
import com.example.model.DepositRequest
import com.example.model.GameMode
import com.example.model.JoinedTournamentRecord
import com.example.model.LeaderboardEntry
import com.example.model.NotificationCategory
import com.example.model.PaymentConfig
import com.example.model.PlayerProfile
import com.example.model.RoomCredentials
import com.example.model.SupportMessage
import com.example.model.SupportTicket
import com.example.model.TicketCategory
import com.example.model.TicketStatus
import com.example.model.Tournament
import com.example.model.TournamentStatus
import com.example.model.WalletTransaction
import com.example.model.WithdrawalRequest
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class LobbixRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun handleStreamError(error: Throwable, op: OperationType, path: String, uid: String): Nothing {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
        throw error
    }

    suspend fun ensurePlayerSetup(
        pendingFullName: String? = null,
        pendingPhone: String? = null
    ): Result<Unit> {
        val currentUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("Not authenticated"))
        val uid = currentUser.uid
        val profilePath = "users/$uid"
        return try {
            val profileRef = db.collection("users").document(uid)
            val snapshot = profileRef.get().await()
            if (!snapshot.exists()) {
                val safeName = pendingFullName?.takeIf { it.isNotBlank() }?.take(60)
                    ?: currentUser.displayName?.takeIf { it.isNotBlank() }?.take(60)
                    ?: "Free Fire Player"
                val safeEmail = currentUser.email?.take(120) ?: ""
                val safePhone = pendingPhone?.takeIf { it.isNotBlank() }?.take(25)
                val initialProfile = buildMap<String, Any> {
                    put("userId", uid)
                    put("fullName", safeName)
                    put("inGameName", "")
                    put("email", safeEmail)
                    if (safePhone != null) put("phone", safePhone)
                    put("freeFireId", "")
                    put("freeFireLevel", 1L)
                    put("avatarPreset", "cyber_crimson")
                    put("walletBalance", 0L)
                    put("lockedBalance", 0L)
                    put("lastWithdrawalId", "")
                    put("totalMatches", 0L)
                    put("totalWins", 0L)
                    put("totalKills", 0L)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                val batch = db.batch()
                batch.set(profileRef, initialProfile)

                val notifRef = profileRef.collection("notifications").document("welcome_lobbix")
                batch.set(
                    notifRef,
                    mapOf(
                        "userId" to uid,
                        "title" to "Welcome to LOBBIX Arena",
                        "message" to "Add your Free Fire UID and In-Game Name (IGN) in Profile, then join a tournament to unlock Room IDs.",
                        "category" to NotificationCategory.TOURNAMENT_UPDATE.wireValue,
                        "tournamentId" to "",
                        "readStatus" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.commit().await()
            }

            if (currentUser.isEmailVerified && currentUser.email == OWNER_EMAIL) {
                seedInitialArenaIfEmpty()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, profilePath)
            Result.failure(e)
        }
    }

    private suspend fun seedInitialArenaIfEmpty() {
        val paymentRef = db.collection("app_config").document("payment")
        val paymentSnap = paymentRef.get().await()
        if (!paymentSnap.exists()) {
            paymentRef.set(
                mapOf(
                    "upiId" to "lobbix.esports@okaxis",
                    "merchantName" to "LOBBIX Esports",
                    "instruction" to "Pay via any UPI app using QR or UPI ID, then submit your Transaction ID / UTR and payment screenshot.",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        val existingTourneys = db.collection("tournaments")
            .whereEqualTo("gameName", "Free Fire")
            .limit(1)
            .get()
            .await()
        if (existingTourneys.isEmpty) {
            val batch = db.batch()
            val seeds = listOf(
                SeedTournament(
                    id = "ff_squad_bermuda_pro",
                    title = "LOBBIX Bermuda Pro Scrims",
                    mode = GameMode.SQUAD,
                    mapName = "Bermuda",
                    scheduleLabel = "Today • 8:30 PM IST",
                    entryFee = 0L,
                    prizePool = 5000L,
                    perKillReward = 50L,
                    maxPlayers = 48L,
                    joinedCount = 28L,
                    status = TournamentStatus.LIVE,
                    roomPublished = true,
                    rulesSummary = "4-player Free Fire Squad clash on Bermuda. Mobile only, no emulators or third-party scripts. Screen recording mandatory for top 3 squads.",
                    bannerKey = "bermuda"
                ),
                SeedTournament(
                    id = "ff_solo_rush_cup",
                    title = "Crimson Solo Rush Cup",
                    mode = GameMode.SOLO,
                    mapName = "Purgatory",
                    scheduleLabel = "Today • 10:00 PM IST",
                    entryFee = 20L,
                    prizePool = 1500L,
                    perKillReward = 25L,
                    maxPlayers = 48L,
                    joinedCount = 34L,
                    status = TournamentStatus.UPCOMING,
                    roomPublished = true,
                    rulesSummary = "Solo survival on Purgatory. Room ID & Password are published 15 mins prior to drop. Teaming leads to instant disqualification.",
                    bannerKey = "bermuda"
                ),
                SeedTournament(
                    id = "ff_duo_kalahari_strike",
                    title = "Kalahari Duo Strike Series",
                    mode = GameMode.DUO,
                    mapName = "Kalahari",
                    scheduleLabel = "Tomorrow • 6:00 PM IST",
                    entryFee = 40L,
                    prizePool = 3000L,
                    perKillReward = 40L,
                    maxPlayers = 48L,
                    joinedCount = 18L,
                    status = TournamentStatus.UPCOMING,
                    roomPublished = false,
                    rulesSummary = "Classic Duo rotation on Kalahari. Points awarded per elimination plus placement multiplier.",
                    bannerKey = "clash"
                ),
                SeedTournament(
                    id = "ff_lone_wolf_cage",
                    title = "Lone Wolf Iron Cage Showdown",
                    mode = GameMode.LONE_WOLF,
                    mapName = "Iron Cage",
                    scheduleLabel = "Tomorrow • 8:00 PM IST",
                    entryFee = 25L,
                    prizePool = 1200L,
                    perKillReward = 100L,
                    maxPlayers = 16L,
                    joinedCount = 10L,
                    status = TournamentStatus.UPCOMING,
                    roomPublished = false,
                    rulesSummary = "Best of 9 rounds in Free Fire Lone Wolf mode. Headshot accuracy & fair play rules enforced.",
                    bannerKey = "clash"
                ),
                SeedTournament(
                    id = "ff_1v1_custom_headshot",
                    title = "1v1 Custom Desert Eagle Duel",
                    mode = GameMode.ONE_V_ONE,
                    mapName = "Bermuda Custom",
                    scheduleLabel = "Today • 9:15 PM IST",
                    entryFee = 50L,
                    prizePool = 900L,
                    perKillReward = 0L,
                    maxPlayers = 2L,
                    joinedCount = 1L,
                    status = TournamentStatus.LIVE,
                    roomPublished = true,
                    rulesSummary = "1v1 Free Fire Custom Room. Unlimited gloo walls enabled. Desert Eagle & M1887 only. No roof camping.",
                    bannerKey = "clash"
                )
            )

            seeds.forEach { item ->
                val ref = db.collection("tournaments").document(item.id)
                batch.set(
                    ref,
                    mapOf(
                        "title" to item.title,
                        "gameName" to "Free Fire",
                        "gameMode" to item.mode.wireValue,
                        "mapName" to item.mapName,
                        "scheduleLabel" to item.scheduleLabel,
                        "entryFee" to item.entryFee,
                        "prizePool" to item.prizePool,
                        "perKillReward" to item.perKillReward,
                        "maxPlayers" to item.maxPlayers,
                        "joinedCount" to item.joinedCount,
                        "status" to item.status.wireValue,
                        "roomPublished" to item.roomPublished,
                        "rulesSummary" to item.rulesSummary,
                        "bannerKey" to item.bannerKey,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (item.roomPublished) {
                    val roomRef = ref.collection("room_credentials").document("current")
                    batch.set(
                        roomRef,
                        mapOf(
                            "tournamentId" to item.id,
                            "roomId" to when (item.mode) {
                                GameMode.SQUAD -> "49201847"
                                GameMode.SOLO -> "77391042"
                                else -> "11894025"
                            },
                            "roomPassword" to when (item.mode) {
                                GameMode.SQUAD -> "LBX-PRO9"
                                GameMode.SOLO -> "RUSH2026"
                                else -> "DUEL1V1"
                            },
                            "serverRegion" to "India / Singapore",
                            "matchNote" to "Join room at least 10 minutes before start. Slot verification via Free Fire UID.",
                            "publishedAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            val leaders = listOf(
                Triple("lb_1", "ViperX_FF", Triple("284719305", 1L, 142L)) to Triple(18L, 26L, 2840L) to "BOOYAH #1 • 11 Kills",
                Triple("lb_2", "KiraHeadshot", Triple("993817264", 2L, 129L)) to Triple(15L, 25L, 2610L) to "BOOYAH #1 • 9 Kills",
                Triple("lb_3", "RedShadow_IN", Triple("552810394", 3L, 118L)) to Triple(14L, 26L, 2490L) to "#2 • 8 Kills",
                Triple("lb_4", "BlazeRusher", Triple("441092837", 4L, 104L)) to Triple(11L, 23L, 2180L) to "#3 • 7 Kills",
                Triple("lb_5", "NovaClutch77", Triple("719283645", 5L, 96L)) to Triple(10L, 24L, 1990L) to "#2 • 6 Kills",
                Triple("lb_6", "ApexGloo", Triple("812394012", 6L, 84L)) to Triple(8L, 20L, 1740L) to "#4 • 5 Kills"
            )

            leaders.forEach { (pair, lastResult) ->
                val (identity, stats) = pair
                val (docId, playerName, idRankKills) = identity
                val (ffId, rank, kills) = idRankKills
                val (wins, matches, pts) = stats
                batch.set(
                    db.collection("leaderboard").document(docId),
                    mapOf(
                        "playerId" to docId,
                        "playerName" to playerName,
                        "freeFireId" to ffId,
                        "rank" to rank,
                        "kills" to kills,
                        "wins" to wins,
                        "matchesPlayed" to matches,
                        "points" to pts,
                        "lastMatchResult" to lastResult,
                        "seasonLabel" to "Season 1",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
        }
    }

    fun observePlayerProfile(userId: String): Flow<PlayerProfile?> = flow {
        val uid = requireUserId()
        if (uid != userId) throw IllegalStateException("Cross-user profile observation forbidden")
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        PlayerProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error -> handleStreamError(error, OperationType.GET, path, uid) }
        )
    }

    fun observePaymentConfig(): Flow<PaymentConfig> = flow {
        val uid = requireUserId()
        val path = "app_config/payment"
        emitAll(
            db.collection("app_config").document("payment")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        PaymentConfig::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: PaymentConfig()
                }
                .catch { error -> handleStreamError(error, OperationType.GET, path, uid) }
        )
    }

    fun observeDepositRequests(userId: String): Flow<List<DepositRequest>> = flow {
        val uid = requireUserId()
        val path = "users/$userId/deposit_requests"
        emitAll(
            db.collection("users").document(userId)
                .collection("deposit_requests")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DepositRequest::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
        )
    }

    fun observeWithdrawalRequests(userId: String): Flow<List<WithdrawalRequest>> = flow {
        val uid = requireUserId()
        val path = "users/$userId/withdrawal_requests"
        emitAll(
            db.collection("users").document(userId)
                .collection("withdrawal_requests")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WithdrawalRequest::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
        )
    }

    fun observeTournaments(): Flow<List<Tournament>> = flow {
        val uid = requireUserId()
        val path = "tournaments"
        emitAll(
            db.collection("tournaments")
                .whereEqualTo("gameName", "Free Fire")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Tournament::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
        )
    }

    fun observeTournamentById(tournamentId: String): Flow<Tournament?> = flow {
        val uid = requireUserId()
        val path = "tournaments/$tournamentId"
        emitAll(
            db.collection("tournaments").document(tournamentId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        Tournament::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error -> handleStreamError(error, OperationType.GET, path, uid) }
        )
    }

    fun observeParticipantStatus(tournamentId: String, userId: String): Flow<Boolean> = flow {
        val uid = requireUserId()
        val path = "tournaments/$tournamentId/participants/$userId"
        emitAll(
            db.collection("tournaments").document(tournamentId)
                .collection("participants").document(userId)
                .snapshots()
                .map { it.exists() }
                .catch { error -> handleStreamError(error, OperationType.GET, path, uid) }
        )
    }

    fun observeRoomCredentials(tournamentId: String): Flow<RoomCredentials?> = flow {
        val uid = requireUserId()
        val path = "tournaments/$tournamentId/room_credentials/current"
        emitAll(
            db.collection("tournaments").document(tournamentId)
                .collection("room_credentials").document("current")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            RoomCredentials::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error -> handleStreamError(error, OperationType.GET, path, uid) }
        )
    }

    fun observeJoinedTournaments(userId: String): Flow<List<JoinedTournamentRecord>> = flow {
        val uid = requireUserId()
        val path = "users/$userId/joined_tournaments"
        emitAll(
            db.collection("users").document(userId)
                .collection("joined_tournaments")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        JoinedTournamentRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.joinedAt ?: Timestamp(0, 0) }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
        )
    }

    fun observeLeaderboard(): Flow<List<LeaderboardEntry>> = flow {
        val uid = requireUserId()
        val path = "leaderboard"
        emitAll(
            db.collection("leaderboard")
                .whereEqualTo("seasonLabel", "Season 1")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        LeaderboardEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.rank }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
        )
    }

    fun observeTransactions(userId: String): Flow<List<WalletTransaction>> = flow {
        val uid = requireUserId()
        val path = "users/$userId/transactions"
        emitAll(
            db.collection("users").document(userId)
                .collection("transactions")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WalletTransaction::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
        )
    }

    fun observeNotifications(userId: String): Flow<List<AppNotification>> = flow {
        val uid = requireUserId()
        val path = "users/$userId/notifications"
        emitAll(
            db.collection("users").document(userId)
                .collection("notifications")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AppNotification::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
        )
    }

    fun observeTickets(userId: String): Flow<List<SupportTicket>> = flow {
        val uid = requireUserId()
        val path = "users/$userId/tickets"
        emitAll(
            db.collection("users").document(userId)
                .collection("tickets")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SupportTicket::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
        )
    }

    fun observeTicketMessages(userId: String, ticketId: String): Flow<List<SupportMessage>> = flow {
        val uid = requireUserId()
        val path = "users/$userId/tickets/$ticketId/messages"
        emitAll(
            db.collection("users").document(userId)
                .collection("tickets").document(ticketId)
                .collection("messages")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SupportMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(Long.MAX_VALUE / 2, 0) }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
        )
    }

    suspend fun joinTournament(
        tournament: Tournament,
        profile: PlayerProfile,
        overrideIgn: String = "",
        overrideFfUid: String = "",
        teamTag: String = ""
    ): Result<Unit> {
        val tournamentPath = "tournaments/${tournament.id}"
        return try {
            val uid = requireUserId()
            require(tournament.id.isNotBlank()) { "Tournament ID is missing." }
            require(tournament.status != TournamentStatus.COMPLETED.wireValue) { "Tournament is already completed." }
            require(tournament.joinedCount < tournament.maxPlayers) { "Tournament slots are full." }

            val cleanName = overrideIgn.ifBlank { profile.inGameName.ifBlank { profile.fullName } }
                .trim().take(60)
            val cleanFfId = overrideFfUid.ifBlank { profile.freeFireId }.trim().take(30)
            require(cleanName.isNotEmpty()) { "Enter your In-Game Name (IGN) before joining." }
            require(cleanFfId.isNotEmpty()) { "Enter your Free Fire UID in Profile before joining." }

            val cleanTeamTag = teamTag.trim().take(30)
            val tourneyRef = db.collection("tournaments").document(tournament.id)
            val participantRef = tourneyRef.collection("participants").document(uid)
            val joinedIndexRef = db.collection("users").document(uid)
                .collection("joined_tournaments").document(tournament.id)
            val notifRef = db.collection("users").document(uid)
                .collection("notifications").document("join_${tournament.id}")

            val participantPayload = buildMap<String, Any> {
                put("userId", uid)
                put("tournamentId", tournament.id)
                put("playerName", cleanName)
                put("freeFireId", cleanFfId)
                if (cleanTeamTag.isNotEmpty()) put("teamTag", cleanTeamTag)
                put("joinedAt", FieldValue.serverTimestamp())
            }

            val joinedRecordPayload = mapOf(
                "userId" to uid,
                "tournamentId" to tournament.id,
                "tournamentTitle" to tournament.title.take(100),
                "gameMode" to tournament.gameMode,
                "scheduleLabel" to tournament.scheduleLabel.take(60),
                "mapName" to tournament.mapName.take(40),
                "entryFee" to tournament.entryFee,
                "prizePool" to tournament.prizePool,
                "status" to "JOINED",
                "placementLabel" to "Slot Confirmed",
                "killsLogged" to 0L,
                "joinedAt" to FieldValue.serverTimestamp()
            )

            val notifPayload = mapOf(
                "userId" to uid,
                "title" to if (tournament.roomPublished) "Room Live: ${tournament.title}" else "Joined: ${tournament.title}",
                "message" to if (tournament.roomPublished) {
                    "You're in! Open Room Details to view your live Room ID & Password."
                } else {
                    "Slot confirmed for ${tournament.scheduleLabel}. Room credentials will appear once published by Admin."
                },
                "category" to if (tournament.roomPublished) {
                    NotificationCategory.ROOM_PUBLISHED.wireValue
                } else {
                    NotificationCategory.TOURNAMENT_UPDATE.wireValue
                },
                "tournamentId" to tournament.id,
                "readStatus" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            if (overrideIgn.isNotBlank() || overrideFfUid.isNotBlank()) {
                val userRef = db.collection("users").document(uid)
                batch.update(
                    userRef,
                    mapOf(
                        "inGameName" to cleanName.take(40),
                        "freeFireId" to cleanFfId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.set(participantRef, participantPayload)
            batch.set(joinedIndexRef, joinedRecordPayload)
            batch.update(
                tourneyRef,
                mapOf(
                    "joinedCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(notifRef, notifPayload)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, tournamentPath)
            Result.failure(e)
        }
    }

    suspend fun updatePlayerProfile(
        fullName: String,
        inGameName: String,
        phone: String,
        freeFireId: String,
        freeFireLevel: Long,
        avatarPreset: String
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val cleanName = fullName.trim()
            val cleanIgn = inGameName.trim()
            val cleanFfId = freeFireId.trim()
            val cleanPhone = phone.trim()
            require(cleanName.length in 1..60) { "Full name must be 1 to 60 characters." }
            require(cleanIgn.length <= 40) { "In-Game Name (IGN) must be at most 40 characters." }
            require(cleanFfId.length <= 30) { "Free Fire UID must be at most 30 characters." }
            require(cleanPhone.length <= 25) { "Phone number must be at most 25 characters." }
            require(freeFireLevel in 1L..120L) { "Free Fire level must be between 1 and 120." }

            val docRef = db.collection("users").document(uid)
            path = docRef.path
            docRef.update(
                mapOf(
                    "fullName" to cleanName,
                    "inGameName" to cleanIgn,
                    "phone" to cleanPhone,
                    "freeFireId" to cleanFfId,
                    "freeFireLevel" to freeFireLevel,
                    "avatarPreset" to avatarPreset.take(40),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    /**
     * Submits a Pending deposit request with UTR + screenshot proof.
     * NEVER auto-credits walletBalance; Admin approval updates balance externally.
     */
    suspend fun submitDepositRequest(
        amount: Long,
        utrNumber: String,
        adminUpiId: String,
        screenshotName: String,
        screenshotPreviewBase64: String
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/deposit_requests"
        return try {
            val uid = requireUserId()
            val cleanUtr = utrNumber.trim()
            val cleanAdminUpi = adminUpiId.trim()
            val cleanScreenName = screenshotName.trim().ifBlank { "payment_proof.jpg" }.take(120)
            require(amount in 1L..100000L) { "Deposit amount must be between 1 and 100,000." }
            require(cleanUtr.length in 4..60) { "Enter a valid Transaction ID / UTR (4 to 60 characters)." }
            require(cleanAdminUpi.length in 5..80) { "Admin UPI ID is missing." }
            require(screenshotPreviewBase64.length in 10..150000) { "Select a valid payment screenshot before submitting." }

            val reqId = "dep_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val txId = "tx_${reqId}"
            val userRef = db.collection("users").document(uid)
            val depRef = userRef.collection("deposit_requests").document(reqId)
            val txRef = userRef.collection("transactions").document(txId)
            path = depRef.path

            val batch = db.batch()
            batch.set(
                depRef,
                mapOf(
                    "userId" to uid,
                    "amount" to amount,
                    "utrNumber" to cleanUtr,
                    "adminUpiId" to cleanAdminUpi,
                    "screenshotName" to cleanScreenName,
                    "screenshotPreviewBase64" to screenshotPreviewBase64,
                    "status" to "Pending",
                    "adminNote" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                txRef,
                mapOf(
                    "userId" to uid,
                    "title" to "Add Money Request (UTR $cleanUtr)".take(80),
                    "type" to "DEPOSIT",
                    "amount" to amount,
                    "status" to "Pending",
                    "referenceCode" to cleanUtr.take(40),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    /**
     * Atomically verifies sufficient balance, deducts/locks amount, and creates a Pending withdrawal request.
     */
    suspend fun submitWithdrawalRequest(
        upiId: String,
        amount: Long
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/withdrawal_requests"
        return try {
            val uid = requireUserId()
            val cleanUpi = upiId.trim()
            require(UPI_REGEX.matches(cleanUpi) && cleanUpi.length <= 80) {
                "Enter a valid UPI ID (e.g. name@upi)."
            }
            require(amount in 1L..100000L) { "Withdrawal amount must be between 1 and 100,000." }

            val reqId = "wd_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val txId = "tx_${reqId}"
            val userRef = db.collection("users").document(uid)
            val wdRef = userRef.collection("withdrawal_requests").document(reqId)
            val txRef = userRef.collection("transactions").document(txId)
            path = wdRef.path

            db.runTransaction { transaction ->
                val userSnap = transaction.get(userRef)
                val currentBalance = userSnap.getLong("walletBalance") ?: 0L
                val currentLocked = userSnap.getLong("lockedBalance") ?: 0L
                if (currentBalance < amount) {
                    throw IllegalStateException("Insufficient balance ($currentBalance available).")
                }

                transaction.update(
                    userRef,
                    mapOf(
                        "walletBalance" to (currentBalance - amount),
                        "lockedBalance" to (currentLocked + amount),
                        "lastWithdrawalId" to reqId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.set(
                    wdRef,
                    mapOf(
                        "userId" to uid,
                        "upiId" to cleanUpi,
                        "amount" to amount,
                        "status" to "Pending",
                        "refundProcessed" to false,
                        "adminNote" to "",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.set(
                    txRef,
                    mapOf(
                        "userId" to uid,
                        "title" to "Withdrawal to $cleanUpi".take(80),
                        "type" to "WITHDRAWAL",
                        "amount" to amount,
                        "status" to "Pending",
                        "referenceCode" to reqId.take(40),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    /**
     * Automatically refunds any Admin-rejected withdrawal back to walletBalance once.
     */
    suspend fun processRejectedWithdrawalRefund(requestId: String): Result<Long> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/withdrawal_requests/$requestId"
        return try {
            val uid = requireUserId()
            require(requestId.isNotBlank()) { "Withdrawal ID cannot be blank." }
            val userRef = db.collection("users").document(uid)
            val wdRef = userRef.collection("withdrawal_requests").document(requestId)
            val txRef = userRef.collection("transactions").document("refund_$requestId")
            path = wdRef.path

            val refundedAmount = db.runTransaction { transaction ->
                val wdSnap = transaction.get(wdRef)
                val userSnap = transaction.get(userRef)
                val status = wdSnap.getString("status") ?: ""
                val alreadyRefunded = wdSnap.getBoolean("refundProcessed") ?: false
                val amount = wdSnap.getLong("amount") ?: 0L
                val upiId = wdSnap.getString("upiId") ?: ""

                if (status != "Rejected" || alreadyRefunded || amount <= 0L) {
                    return@runTransaction 0L
                }

                val currentBalance = userSnap.getLong("walletBalance") ?: 0L
                val currentLocked = userSnap.getLong("lockedBalance") ?: 0L
                val nextLocked = (currentLocked - amount).coerceAtLeast(0L)

                transaction.update(
                    wdRef,
                    mapOf(
                        "refundProcessed" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.update(
                    userRef,
                    mapOf(
                        "walletBalance" to (currentBalance + amount),
                        "lockedBalance" to nextLocked,
                        "lastWithdrawalId" to requestId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.set(
                    txRef,
                    mapOf(
                        "userId" to uid,
                        "title" to "Withdrawal Rejected Refund ($upiId)".take(80),
                        "type" to "REFUND",
                        "amount" to amount,
                        "status" to "Completed",
                        "referenceCode" to "RF-$requestId".take(40),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                amount
            }.await()

            Result.success(refundedAmount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/notifications/$notificationId"
        return try {
            val uid = requireUserId()
            require(notificationId.isNotBlank()) { "Notification ID cannot be blank." }
            val docRef = db.collection("users").document(uid)
                .collection("notifications").document(notificationId)
            path = docRef.path
            docRef.update(
                mapOf(
                    "readStatus" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun fetchRoomCredentialsOnce(tournamentId: String): RoomCredentials? {
        return try {
            requireUserId()
            if (tournamentId.isBlank()) return null
            val snap = db.collection("tournaments").document(tournamentId)
                .collection("room_credentials").document("current")
                .get()
                .await()
            if (snap.exists()) {
                snap.toObject(RoomCredentials::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    suspend fun upsertMatchReminderNotification(
        tournament: Tournament,
        roomCredentials: RoomCredentials?
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/notifications/reminder_${tournament.id}"
        return try {
            val uid = requireUserId()
            val notifId = "reminder_${tournament.id}"
            val docRef = db.collection("users").document(uid)
                .collection("notifications").document(notifId)
            path = docRef.path

            val roomId = roomCredentials?.roomId.orEmpty()
            val roomPassword = roomCredentials?.roomPassword.orEmpty()
            val message = if (roomId.isNotBlank() && roomPassword.isNotBlank()) {
                "Starts in 10 minutes (${tournament.scheduleLabel}) • Room ID: $roomId | Password: $roomPassword"
            } else {
                "Starts in 10 minutes (${tournament.scheduleLabel}). Open Room Details once Admin publishes Room ID & Password."
            }.take(400)

            val existing = docRef.get().await()
            if (existing.exists()) {
                val existingMsg = existing.getString("message").orEmpty()
                if (existingMsg == message) {
                    return Result.success(Unit)
                }
                docRef.delete().await()
            }

            docRef.set(
                mapOf(
                    "userId" to uid,
                    "title" to "10m Alert: ${tournament.title}".take(100),
                    "message" to message,
                    "category" to NotificationCategory.MATCH_REMINDER.wireValue,
                    "tournamentId" to tournament.id,
                    "readStatus" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun createSupportTicket(
        playerName: String,
        subject: String,
        category: TicketCategory,
        tournamentName: String,
        description: String
    ): Result<String> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/tickets"
        return try {
            val uid = requireUserId()
            val cleanSubject = subject.trim()
            val cleanDesc = description.trim()
            val cleanTourney = tournamentName.trim().take(100)
            require(cleanSubject.length in 3..100) { "Subject must be 3 to 100 characters." }
            require(cleanDesc.length in 5..1000) { "Description must be 5 to 1000 characters." }

            val ticketId = "tkt_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val ticketRef = db.collection("users").document(uid)
                .collection("tickets").document(ticketId)
            path = ticketRef.path

            val batch = db.batch()
            batch.set(
                ticketRef,
                mapOf(
                    "userId" to uid,
                    "subject" to cleanSubject,
                    "category" to category.wireValue,
                    "tournamentName" to cleanTourney,
                    "description" to cleanDesc,
                    "status" to TicketStatus.OPEN.wireValue,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                ticketRef.collection("messages").document("msg_01"),
                mapOf(
                    "userId" to uid,
                    "senderRole" to "PLAYER",
                    "senderName" to playerName.ifBlank { "Player" }.take(60),
                    "body" to cleanDesc,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                ticketRef.collection("messages").document("msg_02"),
                mapOf(
                    "userId" to uid,
                    "senderRole" to "SYSTEM",
                    "senderName" to "LOBBIX Desk",
                    "body" to "Ticket logged under ${category.label}. Match admins review active tickets in real time; reply here with screenshots or UID details.",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(ticketId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendSupportMessage(
        ticketId: String,
        playerName: String,
        body: String
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/tickets/$ticketId/messages"
        return try {
            val uid = requireUserId()
            val cleanBody = body.trim()
            require(ticketId.isNotBlank()) { "Ticket ID required." }
            require(cleanBody.length in 1..1000) { "Message must be 1 to 1000 characters." }

            val ticketRef = db.collection("users").document(uid).collection("tickets").document(ticketId)
            val msgId = "msg_${System.currentTimeMillis()}"
            val msgRef = ticketRef.collection("messages").document(msgId)
            path = msgRef.path

            val batch = db.batch()
            batch.set(
                msgRef,
                mapOf(
                    "userId" to uid,
                    "senderRole" to "PLAYER",
                    "senderName" to playerName.ifBlank { "Player" }.take(60),
                    "body" to cleanBody,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                ticketRef,
                mapOf(
                    "status" to TicketStatus.OPEN.wireValue,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun resolveSupportTicket(ticketId: String): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/tickets/$ticketId"
        return try {
            val uid = requireUserId()
            val ticketRef = db.collection("users").document(uid).collection("tickets").document(ticketId)
            path = ticketRef.path
            ticketRef.update(
                mapOf(
                    "status" to TicketStatus.RESOLVED.wireValue,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    private data class SeedTournament(
        val id: String,
        val title: String,
        val mode: GameMode,
        val mapName: String,
        val scheduleLabel: String,
        val entryFee: Long,
        val prizePool: Long,
        val perKillReward: Long,
        val maxPlayers: Long,
        val joinedCount: Long,
        val status: TournamentStatus,
        val roomPublished: Boolean,
        val rulesSummary: String,
        val bannerKey: String
    )

    companion object {
        private const val OWNER_EMAIL = "robirobin319@gmail.com"
        val UPI_REGEX = Regex("^[a-zA-Z0-9._\\-]{2,}@[a-zA-Z]{2,}$")
    }
}
