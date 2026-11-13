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
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.hajiri.MainActivity
import com.example.hajiri.R
import java.util.Calendar

object DailyReminderScheduler {

    private const val PREFS_NAME = "hajiri_reminder_prefs"
    private const val KEY_ENABLED = "daily_reminder_enabled"
    private const val KEY_HOUR = "daily_reminder_hour"
    private const val KEY_MARKED_DATE = "last_attendance_marked_date"

    const val CHANNEL_ID = "hajiri_daily_reminder"
    const val NOTIFICATION_ID = 1008
    private const val REQUEST_CODE = 4201

    fun isEnabled(context: Context): Boolean {
        return context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)
    }

    fun getReminderHour(context: Context): Int {
        val h = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_HOUR, 20)
        return if (h in 18..23) h else 20
    }

    fun recordTodayAttendanceStatus(context: Context, todayKey: String, isMarked: Boolean) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (isMarked) {
            prefs.edit().putString(KEY_MARKED_DATE, todayKey).apply()
        } else if (prefs.getString(KEY_MARKED_DATE, "") == todayKey) {
            prefs.edit().remove(KEY_MARKED_DATE).apply()
        }
    }

    fun isTodayMarkedCached(context: Context, todayKey: String): Boolean {
        return context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_MARKED_DATE, "") == todayKey
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

    fun setReminder(context: Context, enabled: Boolean, hourOfDay: Int = getReminderHour(context)) {
        val appContext = context.applicationContext
        val safeHour = if (hourOfDay == 21) 21 else 20
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putInt(KEY_HOUR, safeHour)
            .apply()

        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildPendingIntent(appContext)
        alarmManager.cancel(pendingIntent)

        if (!enabled) return

        ensureChannel(appContext)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, safeHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "दैनिक हाजिरी रिमाइंडर",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "शाम को आज की हाजिरी दर्ज करने का रिमाइंडर"
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun showReminderNotification(context: Context) {
        val appContext = context.applicationContext
        if (!hasNotificationPermission(appContext)) return
        ensureChannel(appContext)

        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            appContext,
            REQUEST_CODE + 1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("📋 आज की हाजिरी लगाना न भूलें!")
            .setContentText("टैप करें और 1 क्लिक में P / A / HD या ओवरटाइम दर्ज करें।")
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DailyReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!DailyReminderScheduler.isEnabled(context)) return
        val todayKey = DateUtils.getTodayKey()
        if (!DailyReminderScheduler.isTodayMarkedCached(context, todayKey)) {
            DailyReminderScheduler.showReminderNotification(context)
        }
    }
}
