package com.example.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.JoinedTournamentRecord
import com.example.model.RoomCredentials
import com.example.model.Tournament
import com.example.model.TournamentStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object MatchScheduleParser {
    const val TEN_MINUTES_MILLIS: Long = 10L * 60L * 1000L
    private const val FIVE_MINUTES_MILLIS: Long = 5L * 60L * 1000L

    private val TIME_12H_REGEX = Regex("""(\d{1,2}):(\d{2})\s*(AM|PM)""", RegexOption.IGNORE_CASE)
    private val TIME_24H_REGEX = Regex("""\b(\d{1,2}):(\d{2})\b""")
    private val IN_MINUTES_REGEX = Regex("""\bin\s+(\d{1,3})\s*min""", RegexOption.IGNORE_CASE)
    private val DAY_MONTH_REGEX = Regex("""\b(\d{1,2})\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\b""", RegexOption.IGNORE_CASE)
    private val MONTH_DAY_REGEX = Regex("""\b(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\s+(\d{1,2})\b""", RegexOption.IGNORE_CASE)

    private val MONTH_INDEX = mapOf(
        "jan" to Calendar.JANUARY, "feb" to Calendar.FEBRUARY, "mar" to Calendar.MARCH,
        "apr" to Calendar.APRIL, "may" to Calendar.MAY, "jun" to Calendar.JUNE,
        "jul" to Calendar.JULY, "aug" to Calendar.AUGUST, "sep" to Calendar.SEPTEMBER,
        "oct" to Calendar.OCTOBER, "nov" to Calendar.NOVEMBER, "dec" to Calendar.DECEMBER
    )

    fun isRelativeMinutesSchedule(scheduleLabel: String): Boolean =
        IN_MINUTES_REGEX.containsMatchIn(scheduleLabel.trim())

    fun resolveStartTimeMillis(
        scheduleLabel: String,
        explicitStartMillis: Long = 0L,
        nowMillis: Long = System.currentTimeMillis()
    ): Long? {
        if (explicitStartMillis > 0L) return explicitStartMillis
        val trimmed = scheduleLabel.trim()
        if (trimmed.isEmpty()) return null

        IN_MINUTES_REGEX.find(trimmed)?.let { match ->
            val mins = match.groupValues[1].toLongOrNull()
            if (mins != null && mins > 0L) {
                return nowMillis + mins * 60_000L
            }
        }

        val tz = if (trimmed.contains("IST", ignoreCase = true)) {
            TimeZone.getTimeZone("Asia/Kolkata")
        } else {
            TimeZone.getDefault()
        }
        val cal = Calendar.getInstance(tz).apply { timeInMillis = nowMillis }
        if (trimmed.contains("Tomorrow", ignoreCase = true)) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        } else {
            val dm = DAY_MONTH_REGEX.find(trimmed)
            val md = MONTH_DAY_REGEX.find(trimmed)
            when {
                dm != null -> {
                    val day = dm.groupValues[1].toIntOrNull()
                    val month = MONTH_INDEX[dm.groupValues[2].lowercase(Locale.US)]
                    if (day != null && month != null) {
                        cal.set(Calendar.MONTH, month)
                        cal.set(Calendar.DAY_OF_MONTH, day)
                    }
                }
                md != null -> {
                    val month = MONTH_INDEX[md.groupValues[1].lowercase(Locale.US)]
                    val day = md.groupValues[2].toIntOrNull()
                    if (day != null && month != null) {
                        cal.set(Calendar.MONTH, month)
                        cal.set(Calendar.DAY_OF_MONTH, day)
                    }
                }
            }
        }

        val match12 = TIME_12H_REGEX.find(trimmed)
        if (match12 != null) {
            val rawHour = match12.groupValues[1].toIntOrNull() ?: return null
            val minute = match12.groupValues[2].toIntOrNull() ?: return null
            val isPm = match12.groupValues[3].equals("PM", ignoreCase = true)
            val hour24 = when {
                rawHour == 12 && !isPm -> 0
                rawHour == 12 && isPm -> 12
                isPm -> rawHour + 12
                else -> rawHour
            }
            cal.set(Calendar.HOUR_OF_DAY, hour24)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        val match24 = TIME_24H_REGEX.find(trimmed)
        if (match24 != null) {
            val hour24 = match24.groupValues[1].toIntOrNull() ?: return null
            val minute = match24.groupValues[2].toIntOrNull() ?: return null
            if (hour24 in 0..23 && minute in 0..59) {
                cal.set(Calendar.HOUR_OF_DAY, hour24)
                cal.set(Calendar.MINUTE, minute)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                return cal.timeInMillis
            }
        }
        return null
    }

    fun reminderTriggerMillis(startTimeMillis: Long): Long =
        startTimeMillis - TEN_MINUTES_MILLIS

    fun isInTenMinutePreStartWindow(
        startTimeMillis: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        val triggerAt = reminderTriggerMillis(startTimeMillis)
        return nowMillis in triggerAt..(startTimeMillis + FIVE_MINUTES_MILLIS)
    }

    fun formatLocalTime(epochMillis: Long): String {
        val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        return formatter.format(Date(epochMillis))
    }
}

object MatchNotificationHelper {
    const val CHANNEL_ID = "lobbix_match_reminders"
    const val EXTRA_TOURNAMENT_ID = "extra_tournament_id"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Match Reminders & Room IDs",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts 10 minutes before registered Free Fire tournaments with Room ID & Password."
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        val runtimeGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return runtimeGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission")
    fun showRoomReminderNotification(
        context: Context,
        tournamentId: String,
        tournamentTitle: String,
        gameMode: String,
        mapName: String,
        scheduleLabel: String,
        roomId: String,
        roomPassword: String,
        serverRegion: String
    ): Boolean {
        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "Notification skipped: POST_NOTIFICATIONS permission not granted.")
            return false
        }

        ensureChannel(context)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TOURNAMENT_ID, tournamentId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            tournamentId.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val hasCredentials = roomId.isNotBlank() && roomPassword.isNotBlank()
        val titleText = "Starts in 10m: $tournamentTitle"
        val shortBody = if (hasCredentials) {
            "Room ID: $roomId  |  Pass: $roomPassword"
        } else {
            "$gameMode • $mapName starts soon. Tap to view live Room Details."
        }
        val expandedBody = if (hasCredentials) {
            buildString {
                append("$gameMode • $mapName ($scheduleLabel)")
                append("\nRoom ID: $roomId")
                append("\nPassword: $roomPassword")
                if (serverRegion.isNotBlank()) {
                    append("\nServer: $serverRegion")
                }
            }
        } else {
            "$gameMode • $mapName starts in 10 minutes ($scheduleLabel). Room ID & Password will appear here as soon as Admin publishes them."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_lobbix)
            .setContentTitle(titleText)
            .setContentText(shortBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedBody))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(
            notificationIdFor(tournamentId),
            notification
        )
        return true
    }

    fun notificationIdFor(tournamentId: String): Int =
        (tournamentId.hashCode() and 0x7FFFFFFF).coerceAtLeast(1001)

    private const val TAG = "MatchNotifHelper"
}

object MatchReminderScheduler {
    private const val PREFS_NAME = "lobbix_match_reminders"
    private const val KEY_ARMED_IDS = "armed_ids"
    private const val KEY_DISPATCHED_SIGNATURES = "dispatched_sigs"
    private const val KEY_REL_ANCHOR_PREFIX = "rel_anchor_"
    private const val TAG = "MatchReminderSched"

    fun resolveStableStartTimeMillis(
        context: Context,
        tournament: Tournament,
        nowMillis: Long = System.currentTimeMillis()
    ): Long? {
        if (tournament.startTimeMillis > 0L) return tournament.startTimeMillis
        if (!MatchScheduleParser.isRelativeMinutesSchedule(tournament.scheduleLabel)) {
            return MatchScheduleParser.resolveStartTimeMillis(
                scheduleLabel = tournament.scheduleLabel,
                explicitStartMillis = 0L,
                nowMillis = nowMillis
            )
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val anchorKey = "$KEY_REL_ANCHOR_PREFIX${tournament.id}_${tournament.scheduleLabel.trim()}"
        val saved = prefs.getLong(anchorKey, 0L)
        if (saved > 0L) return saved
        val resolved = MatchScheduleParser.resolveStartTimeMillis(
            scheduleLabel = tournament.scheduleLabel,
            explicitStartMillis = 0L,
            nowMillis = nowMillis
        ) ?: return null
        prefs.edit().putLong(anchorKey, resolved).apply()
        return resolved
    }

    fun scheduleForTournament(
        context: Context,
        tournament: Tournament,
        roomCredentials: RoomCredentials?,
        nowMillis: Long = System.currentTimeMillis()
    ): ReminderScheduleResult {
        if (tournament.statusEnum == TournamentStatus.COMPLETED) {
            cancelReminder(context, tournament.id)
            return ReminderScheduleResult.Completed
        }

        val startMillis = resolveStableStartTimeMillis(
            context = context,
            tournament = tournament,
            nowMillis = nowMillis
        ) ?: return ReminderScheduleResult.UnknownSchedule

        val triggerAtMillis = MatchScheduleParser.reminderTriggerMillis(startMillis)
        val roomId = roomCredentials?.roomId.orEmpty()
        val roomPassword = roomCredentials?.roomPassword.orEmpty()
        val serverRegion = roomCredentials?.serverRegion.orEmpty()

        return when {
            MatchScheduleParser.isInTenMinutePreStartWindow(startMillis, nowMillis) -> {
                val signature = "${tournament.id}_${roomId}_${roomPassword}"
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val dispatched = prefs.getStringSet(KEY_DISPATCHED_SIGNATURES, emptySet()).orEmpty()
                if (signature !in dispatched) {
                    val shown = MatchNotificationHelper.showRoomReminderNotification(
                        context = context,
                        tournamentId = tournament.id,
                        tournamentTitle = tournament.title,
                        gameMode = tournament.modeEnum.label,
                        mapName = tournament.mapName,
                        scheduleLabel = tournament.scheduleLabel,
                        roomId = roomId,
                        roomPassword = roomPassword,
                        serverRegion = serverRegion
                    )
                    if (shown) {
                        prefs.edit()
                            .putStringSet(KEY_DISPATCHED_SIGNATURES, dispatched + signature)
                            .apply()
                    }
                }
                ReminderScheduleResult.ActiveWindow(triggerAtMillis)
            }

            triggerAtMillis > nowMillis -> {
                scheduleAlarm(
                    context = context,
                    tournamentId = tournament.id,
                    tournamentTitle = tournament.title,
                    gameMode = tournament.modeEnum.label,
                    mapName = tournament.mapName,
                    scheduleLabel = tournament.scheduleLabel,
                    roomId = roomId,
                    roomPassword = roomPassword,
                    serverRegion = serverRegion,
                    triggerAtMillis = triggerAtMillis
                )
                ReminderScheduleResult.Scheduled(triggerAtMillis)
            }

            else -> ReminderScheduleResult.PastWindow
        }
    }

    fun syncJoinedReminders(
        context: Context,
        joinedRecords: List<JoinedTournamentRecord>,
        tournaments: List<Tournament>,
        roomCredentialsByTournament: Map<String, RoomCredentials?>
    ) {
        val tourneyMap = tournaments.associateBy { it.id }
        val joinedIds = joinedRecords.map { it.tournamentId }.toSet()

        // Cancel reminders for any tournaments no longer registered
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val previouslyArmed = prefs.getStringSet(KEY_ARMED_IDS, emptySet()).orEmpty()
        previouslyArmed.filter { it !in joinedIds }.forEach { removedId ->
            cancelReminder(context, removedId)
        }

        joinedRecords.forEach { record ->
            val tournament = tourneyMap[record.tournamentId] ?: Tournament(
                id = record.tournamentId,
                title = record.tournamentTitle,
                gameMode = record.gameMode,
                mapName = record.mapName,
                scheduleLabel = record.scheduleLabel,
                entryFee = record.entryFee,
                prizePool = record.prizePool,
                startTimeMillis = record.startTimeMillis
            )
            scheduleForTournament(
                context = context,
                tournament = tournament,
                roomCredentials = roomCredentialsByTournament[record.tournamentId]
            )
        }
    }

    fun cancelReminder(context: Context, tournamentId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MatchReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeFor(tournamentId),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val armed = prefs.getStringSet(KEY_ARMED_IDS, emptySet()).orEmpty()
        if (tournamentId in armed) {
            prefs.edit().putStringSet(KEY_ARMED_IDS, armed - tournamentId).apply()
        }
    }

    fun cancelAllReminders(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val armed = prefs.getStringSet(KEY_ARMED_IDS, emptySet()).orEmpty()
        armed.forEach { cancelReminder(context, it) }
        prefs.edit().clear().apply()
    }

    fun markSignatureDispatched(context: Context, tournamentId: String, roomId: String, roomPassword: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getStringSet(KEY_DISPATCHED_SIGNATURES, emptySet()).orEmpty()
        val sig = "${tournamentId}_${roomId}_${roomPassword}"
        prefs.edit().putStringSet(KEY_DISPATCHED_SIGNATURES, existing + sig).apply()
    }

    private fun scheduleAlarm(
        context: Context,
        tournamentId: String,
        tournamentTitle: String,
        gameMode: String,
        mapName: String,
        scheduleLabel: String,
        roomId: String,
        roomPassword: String,
        serverRegion: String,
        triggerAtMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MatchReminderReceiver::class.java).apply {
            putExtra(MatchReminderReceiver.EXTRA_TOURNAMENT_ID, tournamentId)
            putExtra(MatchReminderReceiver.EXTRA_TITLE, tournamentTitle)
            putExtra(MatchReminderReceiver.EXTRA_GAME_MODE, gameMode)
            putExtra(MatchReminderReceiver.EXTRA_MAP_NAME, mapName)
            putExtra(MatchReminderReceiver.EXTRA_SCHEDULE_LABEL, scheduleLabel)
            putExtra(MatchReminderReceiver.EXTRA_ROOM_ID, roomId)
            putExtra(MatchReminderReceiver.EXTRA_ROOM_PASSWORD, roomPassword)
            putExtra(MatchReminderReceiver.EXTRA_SERVER_REGION, serverRegion)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeFor(tournamentId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Uses setAndAllowWhileIdle (zero Play-restricted exact-alarm permissions needed)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val armed = prefs.getStringSet(KEY_ARMED_IDS, emptySet()).orEmpty()
        prefs.edit().putStringSet(KEY_ARMED_IDS, armed + tournamentId).apply()
        Log.d(TAG, "Armed 10m match alert for $tournamentId at $triggerAtMillis")
    }

    private fun requestCodeFor(tournamentId: String): Int =
        (tournamentId.hashCode() and 0x7FFFFFFF).coerceAtLeast(2001)
}

sealed interface ReminderScheduleResult {
    data class Scheduled(val triggerAtMillis: Long) : ReminderScheduleResult
    data class ActiveWindow(val triggerAtMillis: Long) : ReminderScheduleResult
    data object PastWindow : ReminderScheduleResult
    data object Completed : ReminderScheduleResult
    data object UnknownSchedule : ReminderScheduleResult
}
