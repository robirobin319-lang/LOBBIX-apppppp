package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.R
import com.example.model.NotificationCategory
import com.example.model.RoomCredentials
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class MatchReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val tournamentId = intent.getStringExtra(EXTRA_TOURNAMENT_ID).orEmpty()
        if (tournamentId.isBlank()) return

        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Free Fire Match" }
        val gameMode = intent.getStringExtra(EXTRA_GAME_MODE).orEmpty().ifBlank { "Free Fire" }
        val mapName = intent.getStringExtra(EXTRA_MAP_NAME).orEmpty().ifBlank { "Bermuda" }
        val scheduleLabel = intent.getStringExtra(EXTRA_SCHEDULE_LABEL).orEmpty()
        val fallbackRoomId = intent.getStringExtra(EXTRA_ROOM_ID).orEmpty()
        val fallbackRoomPassword = intent.getStringExtra(EXTRA_ROOM_PASSWORD).orEmpty()
        val fallbackRegion = intent.getStringExtra(EXTRA_SERVER_REGION).orEmpty()

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val user = Firebase.auth.currentUser
                var liveRoomId = fallbackRoomId
                var liveRoomPassword = fallbackRoomPassword
                var liveRegion = fallbackRegion

                if (user != null) {
                    val databaseId = appContext.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(databaseId)
                    val credsSnap = withTimeoutOrNull(4500L) {
                        db.collection("tournaments")
                            .document(tournamentId)
                            .collection("room_credentials")
                            .document("current")
                            .get()
                            .await()
                    }
                    val creds = credsSnap?.toObject(RoomCredentials::class.java)
                    if (creds != null) {
                        liveRoomId = creds.roomId
                        liveRoomPassword = creds.roomPassword
                        liveRegion = creds.serverRegion
                    }

                    // Persist an in-app MATCH_REMINDER notification containing Room ID & Password
                    val inAppMessage = if (liveRoomId.isNotBlank() && liveRoomPassword.isNotBlank()) {
                        "Starts in 10 minutes ($scheduleLabel) • Room ID: $liveRoomId | Password: $liveRoomPassword"
                    } else {
                        "Starts in 10 minutes ($scheduleLabel). Open Room Details once Admin publishes Room ID & Password."
                    }.take(400)
                    withTimeoutOrNull(4000L) {
                        val notifRef = db.collection("users")
                            .document(user.uid)
                            .collection("notifications")
                            .document("reminder_$tournamentId")
                        val existing = notifRef.get().await()
                        val shouldWrite = if (existing.exists()) {
                            if (existing.getString("message").orEmpty() != inAppMessage) {
                                notifRef.delete().await()
                                true
                            } else {
                                false
                            }
                        } else {
                            true
                        }
                        if (shouldWrite) {
                            notifRef.set(
                                mapOf(
                                    "userId" to user.uid,
                                    "title" to "10m Alert: $title".take(100),
                                    "message" to inAppMessage,
                                    "category" to NotificationCategory.MATCH_REMINDER.wireValue,
                                    "tournamentId" to tournamentId,
                                    "readStatus" to false,
                                    "createdAt" to FieldValue.serverTimestamp(),
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            ).await()
                        }
                    }
                }

                MatchNotificationHelper.showRoomReminderNotification(
                    context = appContext,
                    tournamentId = tournamentId,
                    tournamentTitle = title,
                    gameMode = gameMode,
                    mapName = mapName,
                    scheduleLabel = scheduleLabel,
                    roomId = liveRoomId,
                    roomPassword = liveRoomPassword,
                    serverRegion = liveRegion
                )
                MatchReminderScheduler.markSignatureDispatched(
                    context = appContext,
                    tournamentId = tournamentId,
                    roomId = liveRoomId,
                    roomPassword = liveRoomPassword
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_TOURNAMENT_ID = "extra_tournament_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_GAME_MODE = "extra_game_mode"
        const val EXTRA_MAP_NAME = "extra_map_name"
        const val EXTRA_SCHEDULE_LABEL = "extra_schedule_label"
        const val EXTRA_ROOM_ID = "extra_room_id"
        const val EXTRA_ROOM_PASSWORD = "extra_room_password"
        const val EXTRA_SERVER_REGION = "extra_server_region"
    }
}
