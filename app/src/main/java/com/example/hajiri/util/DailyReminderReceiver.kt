package com.example.hajiri.util

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.hajiri.MainActivity
import com.example.hajiri.R
import com.example.hajiri.data.HajiriRepository
import com.example.hajiri.widget.HajiriWidgetManager
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.ZoneId
import java.time.ZonedDateTime

object DailyReminderScheduler {

    enum class ScheduleMode {
        DISABLED,
        EXACT,
        INEXACT_FALLBACK,
        UNAVAILABLE
    }

    private const val PREFS_NAME = "hajiri_reminder_prefs"
    private const val KEY_ENABLED = "daily_reminder_enabled"
    private const val KEY_HOUR = "daily_reminder_hour"
    private const val KEY_MINUTE = "daily_reminder_minute"
    private const val KEY_MARKED_DATE = "last_attendance_marked_date"
    private const val KEY_NEXT_TRIGGER_MS = "next_trigger_at_millis"

    const val DEFAULT_HOUR = 20
    const val DEFAULT_MINUTE = 0

    const val CHANNEL_ID = "hajiri_daily_reminder"
    const val NOTIFICATION_ID = 1008
    private const val ALARM_REQUEST_CODE = 4201
    private const val CONTENT_REQUEST_CODE = 4202

    const val ACTION_DAILY_REMINDER = "com.example.hajiri.ACTION_DAILY_REMINDER"
    const val EXTRA_OPEN_HOME_ATTENDANCE = "extra_open_home_attendance"

    const val NOTIFICATION_TITLE = "आज की हाजिरी बाकी है"
    const val NOTIFICATION_BODY = "आज की तारीख की हाजिरी अभी तक दर्ज नहीं हुई है। कृपया हाजिरी लगाएं।"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun normalizeHour(hourOfDay: Int): Int = if (hourOfDay == 21) 21 else DEFAULT_HOUR

    fun normalizeMinute(minute: Int): Int = minute.coerceIn(0, 59)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun getReminderHour(context: Context): Int =
        normalizeHour(prefs(context).getInt(KEY_HOUR, DEFAULT_HOUR))

    fun getReminderMinute(context: Context): Int =
        normalizeMinute(prefs(context).getInt(KEY_MINUTE, DEFAULT_MINUTE))

    fun getNextScheduledTriggerMillis(context: Context): Long =
        prefs(context).getLong(KEY_NEXT_TRIGGER_MS, 0L)

    fun recordTodayAttendanceStatus(context: Context, todayKey: String, isMarked: Boolean) {
        val sharedPrefs = prefs(context)
        if (isMarked) {
            sharedPrefs.edit().putString(KEY_MARKED_DATE, todayKey).apply()
        } else if (sharedPrefs.getString(KEY_MARKED_DATE, "") == todayKey) {
            sharedPrefs.edit().remove(KEY_MARKED_DATE).apply()
        }
    }

    fun clearTodayAttendanceStatus(context: Context) {
        prefs(context).edit().remove(KEY_MARKED_DATE).apply()
    }

    fun isTodayMarkedCached(context: Context, todayKey: String): Boolean {
        return prefs(context).getString(KEY_MARKED_DATE, "") == todayKey
    }

    fun isTodayMarkedLocally(context: Context, todayKey: String): Boolean {
        if (isTodayMarkedCached(context, todayKey)) return true
        val widgetSnapshot = runCatching { HajiriWidgetManager.loadSnapshot(context) }.getOrNull()
        return widgetSnapshot != null &&
            widgetSnapshot.todayKey == todayKey &&
            widgetSnapshot.todayStatusId.isNotBlank()
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        return hasNotificationPermission(context) &&
            NotificationManagerCompat.from(context.applicationContext).areNotificationsEnabled()
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.applicationContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun buildNotificationSettingsIntent(context: Context): Intent {
        val pkg = context.applicationContext.packageName
        return Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, pkg)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun buildExactAlarmSettingsIntent(context: Context): Intent {
        val pkg = context.applicationContext.packageName
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:$pkg")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$pkg")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    /**
     * Computes the next trigger timestamp (epoch millis) in the device's local timezone.
     * - When [forceNextDay] is false: schedules for today if [hourOfDay]:[minute] has not passed yet,
     *   otherwise schedules for tomorrow at [hourOfDay]:[minute].
     * - When [forceNextDay] is true (used after the reminder fires): always schedules for tomorrow
     *   at [hourOfDay]:[minute] so it never fires twice on the same day.
     */
    fun computeNextTriggerMillis(
        now: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault()),
        hourOfDay: Int,
        minute: Int = DEFAULT_MINUTE,
        forceNextDay: Boolean = false
    ): Long {
        val safeHour = normalizeHour(hourOfDay)
        val safeMinute = normalizeMinute(minute)
        var target = now
            .withHour(safeHour)
            .withMinute(safeMinute)
            .withSecond(0)
            .withNano(0)
        if (forceNextDay || !target.isAfter(now)) {
            target = target.plusDays(1)
        }
        return target.toInstant().toEpochMilli()
    }

    fun setReminder(
        context: Context,
        enabled: Boolean,
        hourOfDay: Int = getReminderHour(context),
        minute: Int = getReminderMinute(context)
    ): ScheduleMode {
        val appContext = context.applicationContext
        val safeHour = normalizeHour(hourOfDay)
        val safeMinute = normalizeMinute(minute)

        prefs(appContext)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putInt(KEY_HOUR, safeHour)
            .putInt(KEY_MINUTE, safeMinute)
            .apply()

        if (!enabled) {
            cancelReminder(appContext)
            return ScheduleMode.DISABLED
        }

        ensureChannel(appContext)
        return scheduleNextReminder(appContext, forceNextDay = false)
    }

    fun savePreferredTimeOnly(
        context: Context,
        hourOfDay: Int,
        minute: Int = DEFAULT_MINUTE
    ) {
        val safeHour = normalizeHour(hourOfDay)
        val safeMinute = normalizeMinute(minute)
        prefs(context)
            .edit()
            .putInt(KEY_HOUR, safeHour)
            .putInt(KEY_MINUTE, safeMinute)
            .apply()
    }

    fun rescheduleIfEnabled(
        context: Context,
        now: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault())
    ): ScheduleMode {
        val appContext = context.applicationContext
        if (!isEnabled(appContext)) {
            cancelReminder(appContext)
            return ScheduleMode.DISABLED
        }
        ensureChannel(appContext)
        return scheduleNextReminder(appContext, forceNextDay = false, now = now)
    }

    fun scheduleNextReminder(
        context: Context,
        forceNextDay: Boolean,
        now: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault())
    ): ScheduleMode {
        val appContext = context.applicationContext
        if (!isEnabled(appContext)) {
            cancelReminder(appContext)
            return ScheduleMode.DISABLED
        }

        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return ScheduleMode.UNAVAILABLE

        val hour = getReminderHour(appContext)
        val minute = getReminderMinute(appContext)
        val triggerAtMillis = computeNextTriggerMillis(
            now = now,
            hourOfDay = hour,
            minute = minute,
            forceNextDay = forceNextDay
        )

        val pendingIntent = buildAlarmPendingIntent(appContext)
        alarmManager.cancel(pendingIntent)

        val mode = if (canScheduleExactAlarms(appContext)) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                ScheduleMode.EXACT
            } catch (_: SecurityException) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                ScheduleMode.INEXACT_FALLBACK
            }
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
            ScheduleMode.INEXACT_FALLBACK
        }

        prefs(appContext).edit().putLong(KEY_NEXT_TRIGGER_MS, triggerAtMillis).apply()
        return mode
    }

    fun cancelReminder(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val pendingIntent = buildAlarmPendingIntent(appContext)
        alarmManager?.cancel(pendingIntent)
        pendingIntent.cancel()
        prefs(appContext).edit().remove(KEY_NEXT_TRIGGER_MS).apply()
    }

    fun ensureChannel(context: Context) {
        val manager = context.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                "दैनिक हाजिरी रिमाइंडर",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "यदि आज की हाजिरी दर्ज नहीं हुई है तो रोज़ शाम याद दिलाने के लिए"
                enableVibration(true)
                vibrationPattern = longArrayOf(0L, 250L, 180L, 250L)
                enableLights(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(defaultSound, audioAttrs)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun showReminderNotification(context: Context): Boolean {
        val appContext = context.applicationContext
        if (!areNotificationsEnabled(appContext)) return false
        ensureChannel(appContext)

        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_HOME_ATTENDANCE, true)
        }
        val contentIntent = PendingIntent.getActivity(
            appContext,
            CONTENT_REQUEST_CODE,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_reminder)
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText(NOTIFICATION_BODY)
            .setStyle(NotificationCompat.BigTextStyle().bigText(NOTIFICATION_BODY))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return false
        manager.notify(NOTIFICATION_ID, notification)
        return true
    }

    fun buildAlarmPendingIntent(context: Context): PendingIntent {
        val appContext = context.applicationContext
        val intent = Intent(appContext, DailyReminderReceiver::class.java).apply {
            action = ACTION_DAILY_REMINDER
        }
        return PendingIntent.getBroadcast(
            appContext,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class DailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val appContext = context.applicationContext

        // 1. Verify the daily reminder toggle is still enabled
        if (!DailyReminderScheduler.isEnabled(appContext)) {
            DailyReminderScheduler.cancelReminder(appContext)
            return
        }

        // 2. Always schedule the next occurrence for tomorrow at the configured time
        DailyReminderScheduler.scheduleNextReminder(appContext, forceNextDay = true)

        // 3. Check today's attendance in local timezone (yyyy-MM-dd)
        val todayKey = DateUtils.getTodayKey()
        if (DailyReminderScheduler.isTodayMarkedLocally(appContext, todayKey)) {
            return
        }

        // 4. If signed in to Firebase, double-check Firestore (server or local cache) in case
        //    today's attendance was marked on another device or local cache was cleared.
        val signedInUid = runCatching {
            if (FirebaseApp.getApps(appContext).isNotEmpty()) {
                Firebase.auth.currentUser?.uid
            } else {
                null
            }
        }.getOrNull()

        if (signedInUid.isNullOrBlank()) {
            DailyReminderScheduler.showReminderNotification(appContext)
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val markedInCloud = withTimeoutOrNull(4_500L) {
                    HajiriRepository(appContext).hasAttendanceForDate(signedInUid, todayKey)
                } ?: false

                if (markedInCloud) {
                    DailyReminderScheduler.recordTodayAttendanceStatus(appContext, todayKey, true)
                } else {
                    DailyReminderScheduler.showReminderNotification(appContext)
                }
            } catch (_: Exception) {
                // Fall back to showing reminder if cloud check fails while locally unmarked
                DailyReminderScheduler.showReminderNotification(appContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class DailyReminderRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val shouldReschedule = action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)

        if (!shouldReschedule) return
        DailyReminderScheduler.rescheduleIfEnabled(context.applicationContext)
    }
}
