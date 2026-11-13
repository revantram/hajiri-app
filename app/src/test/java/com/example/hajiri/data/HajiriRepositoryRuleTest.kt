package com.example.hajiri.data

import com.example.hajiri.base.FirestoreEmulatorTestBase
import com.example.hajiri.model.AttendanceStatus
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class HajiriRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun attendance_authenticatedOwner_createsUpdatesAndObserves() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = HajiriRepository(firestore, auth)
        val dateKey = "2026-11-12"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.setAttendance(dateKey, AttendanceStatus.PRESENT, "ऑफिस समय पर")
        }

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeAttendance(uid).first { it.containsKey(dateKey) }
        }
        assertEquals(AttendanceStatus.PRESENT, observed[dateKey]?.status)
        assertEquals("ऑफिस समय पर", observed[dateKey]?.note)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.setAttendance(dateKey, AttendanceStatus.HALF)
        }
        val updated = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeAttendance(uid).first { it[dateKey]?.status == AttendanceStatus.HALF }
        }
        assertEquals("ऑफिस समय पर", updated[dateKey]?.note)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.removeAttendance(dateKey)
        }
        val removed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeAttendance(uid).first { !it.containsKey(dateKey) }
        }
        assertNull(removed[dateKey])
    }

    @Test
    fun settingsOvertimeAndAdvances_authenticatedOwner_persistsAndDeletes() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = HajiriRepository(firestore, auth)
        val dateKey = "2026-11-12"
        val reason = "टेस्ट एडवांस ${UUID.randomUUID().toString().take(6)}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveSitesAndActiveSite(
                sites = listOf(
                    com.example.hajiri.model.WorkSite(
                        id = "default",
                        name = "मुख्य साइट",
                        wageMode = com.example.hajiri.model.WageMode.DAILY.id,
                        dailyWage = 900.0,
                        otRate = 120.0,
                        workDays = 26
                    ),
                    com.example.hajiri.model.WorkSite(
                        id = "site_metro",
                        name = "मेट्रो साइट",
                        wageMode = com.example.hajiri.model.WageMode.DAILY.id,
                        dailyWage = 1100.0,
                        otRate = 150.0,
                        workDays = 26
                    )
                ),
                activeSiteId = "site_metro"
            )
            repository.saveThemeStyle(com.example.hajiri.model.AppThemeStyle.ROSE_GOLD)
            repository.addOvertime(dateKey, 2.5f, siteId = "site_metro")
        }
        val advId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addAdvance(
                amount = 1500.0,
                reason = reason,
                dateKey = "2026-11-05",
                siteId = "site_metro"
            )
        }

        val settings = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeSettings(uid).first {
                it.activeSiteId == "site_metro" && it.sites.size == 2 && it.themeStyle == com.example.hajiri.model.AppThemeStyle.ROSE_GOLD.id
            }
        }
        assertEquals(1100.0, settings.dailyWage, 0.01)
        assertEquals(com.example.hajiri.model.WageMode.DAILY.id, settings.wageMode)
        assertEquals(150.0, settings.otRate, 0.01)
        assertEquals(26, settings.workDays)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.setMonthPayment("2026-10", paidAmount = 10000.0, isSettled = false)
        }
        val partialSettings = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeSettings(uid).first { (it.monthPayments["2026-10"] ?: 0.0) == 10000.0 }
        }
        assertEquals(10000.0, partialSettings.monthPayments["2026-10"] ?: 0.0, 0.01)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.setMonthSettled("2026-10", true)
        }
        val settledSettings = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeSettings(uid).first { "2026-10" in it.settledMonths }
        }
        assertTrue("2026-10" in settledSettings.settledMonths)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.setMonthSettled("2026-10", false)
        }
        val unsettledSettings = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeSettings(uid).first { "2026-10" !in it.settledMonths }
        }
        assertTrue("2026-10" !in unsettledSettings.settledMonths)

        val overtime = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeOvertime(uid).first { (it[dateKey] ?: 0f) >= 2.5f }
        }
        assertTrue((overtime[dateKey] ?: 0f) >= 2.5f)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.setOvertime(dateKey, 1.5f, siteId = "site_metro")
        }
        val updatedOt = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeOvertime(uid).first { (it[dateKey] ?: 0f) == 1.5f }
        }
        assertEquals(1.5f, updatedOt[dateKey] ?: 0f, 0.01f)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.setOvertime(dateKey, 0f)
        }
        val clearedOt = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeOvertime(uid).first { !it.containsKey(dateKey) }
        }
        assertNull(clearedOt[dateKey])

        val advances = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeAdvances(uid).first { list -> list.any { it.id == advId } }
        }
        assertTrue(advances.any { it.id == advId && it.reason == reason && it.dateKey == "2026-11-05" && it.siteId == "site_metro" })

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.deleteAdvance(advId)
        }
    }

    @Test
    fun profile_authenticatedOwner_createsAndUpdatesPhone() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = HajiriRepository(firestore, auth)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureUserProfile("रमेश कुमार", ALICE_EMAIL)
            repository.updateProfileDetails("रमेश शर्मा", "9876543210")
        }

        val profile = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProfile(uid).first { it != null && it.phone == "9876543210" }
        }
        assertNotNull(profile)
        assertEquals("रमेश शर्मा", profile?.name)
        assertEquals(uid, profile?.userId)
    }

    @Test
    fun observeAttendance_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = HajiriRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.setAttendance("2026-11-12", AttendanceStatus.PRESENT, "Alice private")
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = HajiriRepository(firestore, auth)
        val exception = assertThrowsAsync<Exception> {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeAttendance(aliceUid).first()
            }
        }
        val firestoreException = checkNotNull(exception.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $exception"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    @Test
    fun observeAttendance_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = HajiriRepository(firestore, auth)

        val exception = assertThrowsAsync<Exception> {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeAttendance(aliceUid).first()
            }
        }
        val firestoreException = checkNotNull(exception.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $exception"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice.hajiri@test.com"
        const val BOB_EMAIL = "bob.hajiri@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
