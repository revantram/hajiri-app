package com.example.hajiri.util

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DailyReminderSchedulerTest {

    private lateinit var app: Application
    private lateinit var alarmManager: AlarmManager
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        notificationManager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Reset reminder state & grant notification + exact alarm permissions by default
        DailyReminderScheduler.setReminder(app, enabled = false, hourOfDay = 20)
        DailyReminderScheduler.clearTodayAttendanceStatus(app)
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        shadowOf(notificationManager).setNotificationsEnabled(true)
    }

    @Test
    fun computeNextTriggerMillis_schedulesTodayIfTimeNotPassed() {
        val zone = ZoneId.of("Asia/Kolkata")
        val now = ZonedDateTime.of(2026, 11, 19, 18, 30, 0, 0, zone)

        val triggerMs = DailyReminderScheduler.computeNextTriggerMillis(
            now = now,
            hourOfDay = 20,
            minute = 0,
            forceNextDay = false
        )

        val expected = ZonedDateTime.of(2026, 11, 19, 20, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(expected, triggerMs)
    }

    @Test
    fun computeNextTriggerMillis_schedulesTomorrowIfTimeAlreadyPassedOrEqual() {
        val zone = ZoneId.of("Asia/Kolkata")
        val nowAfter = ZonedDateTime.of(2026, 11, 19, 20, 1, 0, 0, zone)
        val nowExact = ZonedDateTime.of(2026, 11, 19, 20, 0, 0, 0, zone)

        val expectedTomorrow = ZonedDateTime.of(2026, 11, 20, 20, 0, 0, 0, zone).toInstant().toEpochMilli()

        assertEquals(
            expectedTomorrow,
            DailyReminderScheduler.computeNextTriggerMillis(now = nowAfter, hourOfDay = 20)
        )
        assertEquals(
            expectedTomorrow,
            DailyReminderScheduler.computeNextTriggerMillis(now = nowExact, hourOfDay = 20)
        )
    }

    @Test
    fun computeNextTriggerMillis_forceNextDayAlwaysSchedulesTomorrow() {
        val zone = ZoneId.of("Asia/Kolkata")
        val now = ZonedDateTime.of(2026, 11, 19, 19, 59, 50, 0, zone)

        val triggerMs = DailyReminderScheduler.computeNextTriggerMillis(
            now = now,
            hourOfDay = 21,
            minute = 0,
            forceNextDay = true
        )

        val expectedTomorrow = ZonedDateTime.of(2026, 11, 20, 21, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(expectedTomorrow, triggerMs)
    }

    @Test
    fun setReminder_persistsPreferencesAndUsesExactOrFallbackMode() {
        assertFalse(DailyReminderScheduler.isEnabled(app))
        assertEquals(20, DailyReminderScheduler.getReminderHour(app))
        assertEquals(0, DailyReminderScheduler.getReminderMinute(app))

        // Exact alarm granted -> ScheduleMode.EXACT
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        val exactMode = DailyReminderScheduler.setReminder(app, enabled = true, hourOfDay = 21)
        assertEquals(DailyReminderScheduler.ScheduleMode.EXACT, exactMode)
        assertTrue(DailyReminderScheduler.isEnabled(app))
        assertEquals(21, DailyReminderScheduler.getReminderHour(app))
        assertNotNull(shadowOf(alarmManager).nextScheduledAlarm)
        val channel = notificationManager.getNotificationChannel(DailyReminderScheduler.CHANNEL_ID)
        assertNotNull(channel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)

        // Exact alarm denied -> graceful inexact fallback without crash
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        val fallbackMode = DailyReminderScheduler.setReminder(app, enabled = true, hourOfDay = 20)
        assertEquals(DailyReminderScheduler.ScheduleMode.INEXACT_FALLBACK, fallbackMode)
        assertEquals(20, DailyReminderScheduler.getReminderHour(app))
        assertNotNull(shadowOf(alarmManager).nextScheduledAlarm)

        // Disable -> cancels alarm
        val disabledMode = DailyReminderScheduler.setReminder(app, enabled = false)
        assertEquals(DailyReminderScheduler.ScheduleMode.DISABLED, disabledMode)
        assertFalse(DailyReminderScheduler.isEnabled(app))
        assertNull(shadowOf(alarmManager).nextScheduledAlarm)
    }

    @Test
    fun receiver_postsHindiNotificationAndReschedulesNextDayWhenTodayUnmarked() {
        DailyReminderScheduler.setReminder(app, enabled = true, hourOfDay = 20)
        val todayKey = DateUtils.getTodayKey()
        DailyReminderScheduler.recordTodayAttendanceStatus(app, todayKey, isMarked = false)
        shadowOf(alarmManager).scheduledAlarms.clear()

        val receiver = DailyReminderReceiver()
        receiver.onReceive(app, Intent(DailyReminderScheduler.ACTION_DAILY_REMINDER))

        val posted = shadowOf(notificationManager).allNotifications
        assertEquals(1, posted.size)
        val notification = posted.first()
        assertEquals(
            DailyReminderScheduler.NOTIFICATION_TITLE,
            notification.extras.getString(Notification.EXTRA_TITLE)
        )
        assertEquals(
            DailyReminderScheduler.NOTIFICATION_BODY,
            notification.extras.getString(Notification.EXTRA_TEXT)
        )
        assertNotNull(notification.contentIntent)
        val launchedIntent = shadowOf(notification.contentIntent).savedIntent
        assertTrue(launchedIntent.getBooleanExtra(DailyReminderScheduler.EXTRA_OPEN_HOME_ATTENDANCE, false))

        // Tomorrow's reminder must be rescheduled
        val nextAlarm = requireNotNull(shadowOf(alarmManager).nextScheduledAlarm)
        assertTrue(nextAlarm.triggerAtTime > System.currentTimeMillis())
    }

    @Test
    fun receiver_suppressesNotificationWhenTodayAlreadyMarked_butStillReschedulesNextDay() {
        DailyReminderScheduler.setReminder(app, enabled = true, hourOfDay = 21)
        val todayKey = DateUtils.getTodayKey()
        DailyReminderScheduler.recordTodayAttendanceStatus(app, todayKey, isMarked = true)
        shadowOf(alarmManager).scheduledAlarms.clear()

        val receiver = DailyReminderReceiver()
        receiver.onReceive(app, Intent(DailyReminderScheduler.ACTION_DAILY_REMINDER))

        assertTrue(shadowOf(notificationManager).allNotifications.isEmpty())
        val nextAlarm = requireNotNull(shadowOf(alarmManager).nextScheduledAlarm)
        assertTrue(nextAlarm.triggerAtTime > System.currentTimeMillis())
    }

    @Test
    fun receiver_doesNothingWhenReminderDisabled() {
        DailyReminderScheduler.setReminder(app, enabled = false, hourOfDay = 20)
        shadowOf(alarmManager).scheduledAlarms.clear()

        val receiver = DailyReminderReceiver()
        receiver.onReceive(app, Intent(DailyReminderScheduler.ACTION_DAILY_REMINDER))

        assertTrue(shadowOf(notificationManager).allNotifications.isEmpty())
        assertNull(shadowOf(alarmManager).nextScheduledAlarm)
    }

    @Test
    fun rescheduleReceiver_reschedulesOnBootTimeTimezoneAndPackageReplaceWhenEnabled() {
        val actions = listOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        )
        val receiver = DailyReminderRescheduleReceiver()

        // Enabled -> each system broadcast reschedules alarm
        DailyReminderScheduler.setReminder(app, enabled = true, hourOfDay = 20)
        actions.forEach { action ->
            shadowOf(alarmManager).scheduledAlarms.clear()
            receiver.onReceive(app, Intent(action))
            assertNotNull("Expected alarm to be rescheduled for $action", shadowOf(alarmManager).nextScheduledAlarm)
        }

        // Disabled -> none of the system broadcasts schedule an alarm
        DailyReminderScheduler.setReminder(app, enabled = false, hourOfDay = 20)
        actions.forEach { action ->
            shadowOf(alarmManager).scheduledAlarms.clear()
            receiver.onReceive(app, Intent(action))
            assertNull("Expected no alarm when disabled for $action", shadowOf(alarmManager).nextScheduledAlarm)
        }
    }
}
