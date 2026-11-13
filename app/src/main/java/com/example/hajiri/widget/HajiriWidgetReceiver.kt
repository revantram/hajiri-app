package com.example.hajiri.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.example.hajiri.MainActivity
import com.example.hajiri.R
import com.example.hajiri.data.HajiriRepository
import com.example.hajiri.model.AttendanceStatus
import com.example.hajiri.util.DailyReminderScheduler
import com.example.hajiri.util.DateUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class HajiriWidgetSnapshot(
    val todayKey: String = DateUtils.getTodayKey(),
    val todayStatusId: String = "",
    val monthlyEarningsText: String = "₹0",
    val workedDaysText: String = "0 दिन",
    val activeSiteId: String = "default",
    val activeSiteName: String = "मुख्य काम",
    val isSignedIn: Boolean = false
)

object HajiriWidgetManager {
    private const val PREFS_NAME = "hajiri_widget_prefs"
    private const val KEY_TODAY_DATE = "today_date"
    private const val KEY_TODAY_STATUS = "today_status"
    private const val KEY_EARNINGS_TEXT = "monthly_earnings_text"
    private const val KEY_WORKED_DAYS_TEXT = "worked_days_text"
    private const val KEY_SITE_ID = "active_site_id"
    private const val KEY_SITE_NAME = "active_site_name"
    private const val KEY_SIGNED_IN = "is_signed_in"

    fun loadSnapshot(context: Context): HajiriWidgetSnapshot {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentTodayKey = DateUtils.getTodayKey()
        val storedDate = prefs.getString(KEY_TODAY_DATE, "") ?: ""
        val storedStatus = if (storedDate == currentTodayKey) {
            prefs.getString(KEY_TODAY_STATUS, "") ?: ""
        } else {
            ""
        }
        return HajiriWidgetSnapshot(
            todayKey = currentTodayKey,
            todayStatusId = storedStatus,
            monthlyEarningsText = prefs.getString(KEY_EARNINGS_TEXT, "₹0") ?: "₹0",
            workedDaysText = prefs.getString(KEY_WORKED_DAYS_TEXT, "0 दिन") ?: "0 दिन",
            activeSiteId = prefs.getString(KEY_SITE_ID, "default") ?: "default",
            activeSiteName = prefs.getString(KEY_SITE_NAME, "मुख्य काम") ?: "मुख्य काम",
            isSignedIn = prefs.getBoolean(KEY_SIGNED_IN, false)
        )
    }

    fun syncFromApp(
        context: Context,
        todayKey: String,
        todayStatus: AttendanceStatus?,
        totalMonthlyEarnings: Double,
        workedDays: Float,
        activeSiteId: String,
        activeSiteName: String,
        isSignedIn: Boolean
    ) {
        val appContext = context.applicationContext
        val earningsStr = "₹${DateUtils.formatInr(totalMonthlyEarnings)}"
        val daysStr = if (workedDays % 1f == 0f) "${workedDays.toInt()} दिन" else "${workedDays} दिन"
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TODAY_DATE, todayKey)
            .putString(KEY_TODAY_STATUS, todayStatus?.id ?: "")
            .putString(KEY_EARNINGS_TEXT, earningsStr)
            .putString(KEY_WORKED_DAYS_TEXT, daysStr)
            .putString(KEY_SITE_ID, activeSiteId.ifBlank { "default" })
            .putString(KEY_SITE_NAME, activeSiteName.ifBlank { "मुख्य काम" })
            .putBoolean(KEY_SIGNED_IN, isSignedIn)
            .apply()
        refreshAllWidgets(appContext)
    }

    fun updateLocalTodayStatus(context: Context, todayKey: String, status: AttendanceStatus) {
        val appContext = context.applicationContext
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TODAY_DATE, todayKey)
            .putString(KEY_TODAY_STATUS, status.id)
            .putBoolean(KEY_SIGNED_IN, true)
            .apply()
        refreshAllWidgets(appContext)
    }

    fun refreshAllWidgets(context: Context) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        val component = ComponentName(appContext, HajiriWidgetReceiver::class.java)
        val ids = manager.getAppWidgetIds(component)
        if (ids.isEmpty()) return
        val snapshot = loadSnapshot(appContext)
        ids.forEach { widgetId ->
            manager.updateAppWidget(widgetId, buildRemoteViews(appContext, snapshot))
        }
    }

    fun buildRemoteViews(context: Context, snapshot: HajiriWidgetSnapshot): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_hajiri_summary)
        val todayStatus = AttendanceStatus.fromId(snapshot.todayStatusId)

        val titleText = if (snapshot.activeSiteName.isNotBlank() && snapshot.activeSiteName != "मुख्य काम") {
            "📋 हाजिरी • ${snapshot.activeSiteName}"
        } else {
            "📋 हाजिरी • आज"
        }
        views.setTextViewText(R.id.widget_title, titleText)

        val statusLabel = when {
            !snapshot.isSignedIn && Firebase.auth.currentUser == null -> "लॉगिन करें"
            todayStatus != null -> "आज: ${todayStatus.labelHindi} ✓"
            else -> "आज की हाजिरी बाकी • ${snapshot.workedDaysText}"
        }
        views.setTextViewText(R.id.widget_today_status, statusLabel)
        views.setTextViewText(R.id.widget_earnings_value, snapshot.monthlyEarningsText)

        views.setTextViewText(
            R.id.widget_btn_p,
            if (todayStatus == AttendanceStatus.PRESENT) "P हाजिर ✓" else "P हाजिर"
        )
        views.setTextViewText(
            R.id.widget_btn_a,
            if (todayStatus == AttendanceStatus.ABSENT) "A गैरहाजिर ✓" else "A गैरहाजिर"
        )
        views.setTextViewText(
            R.id.widget_btn_hd,
            if (todayStatus == AttendanceStatus.HALF) "HD आधा ✓" else "HD आधा"
        )

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            100,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_header_row, openPendingIntent)

        views.setOnClickPendingIntent(
            R.id.widget_btn_p,
            buildMarkPendingIntent(context, 101, AttendanceStatus.PRESENT)
        )
        views.setOnClickPendingIntent(
            R.id.widget_btn_a,
            buildMarkPendingIntent(context, 102, AttendanceStatus.ABSENT)
        )
        views.setOnClickPendingIntent(
            R.id.widget_btn_hd,
            buildMarkPendingIntent(context, 103, AttendanceStatus.HALF)
        )

        return views
    }

    private fun buildMarkPendingIntent(
        context: Context,
        requestCode: Int,
        status: AttendanceStatus
    ): PendingIntent {
        val intent = Intent(context, HajiriWidgetReceiver::class.java).apply {
            action = HajiriWidgetReceiver.ACTION_WIDGET_MARK
            putExtra(HajiriWidgetReceiver.EXTRA_STATUS_ID, status.id)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun requestPinWidget(context: Context): Boolean {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.isRequestPinAppWidgetSupported) {
            val component = ComponentName(appContext, HajiriWidgetReceiver::class.java)
            return manager.requestPinAppWidget(component, null, null)
        }
        return false
    }
}

class HajiriWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val snapshot = HajiriWidgetManager.loadSnapshot(context)
        appWidgetIds.forEach { widgetId ->
            appWidgetManager.updateAppWidget(
                widgetId,
                HajiriWidgetManager.buildRemoteViews(context, snapshot)
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_WIDGET_MARK) return

        val statusId = intent.getStringExtra(EXTRA_STATUS_ID) ?: return
        val status = AttendanceStatus.fromId(statusId) ?: return
        val appContext = context.applicationContext

        if (Firebase.auth.currentUser == null) {
            val launchIntent = Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            appContext.startActivity(launchIntent)
            return
        }

        val todayKey = DateUtils.getTodayKey()
        val snapshot = HajiriWidgetManager.loadSnapshot(appContext)

        // Update widget UI and reminder cache immediately so it feels 1-tap instant even offline
        HajiriWidgetManager.updateLocalTodayStatus(appContext, todayKey, status)
        DailyReminderScheduler.recordTodayAttendanceStatus(appContext, todayKey, true)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                HajiriRepository(appContext).setAttendance(
                    dateKey = todayKey,
                    status = status,
                    siteId = snapshot.activeSiteId
                )
            } catch (_: Exception) {
                // Local widget & Firestore cache remain intact; will sync once network returns
            } finally {
                HajiriWidgetManager.refreshAllWidgets(appContext)
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_WIDGET_MARK = "com.example.hajiri.ACTION_WIDGET_MARK"
        const val EXTRA_STATUS_ID = "extra_status_id"
    }
}
