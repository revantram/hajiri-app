package com.example.hajiri.data

import android.content.Context
import com.example.hajiri.R
import com.example.hajiri.model.AdvanceRecord
import com.example.hajiri.model.AppThemeStyle
import com.example.hajiri.model.AttendanceFirestoreDto
import com.example.hajiri.model.AttendanceRecord
import com.example.hajiri.model.AttendanceStatus
import com.example.hajiri.model.OvertimeRecord
import com.example.hajiri.model.UserProfile
import com.example.hajiri.model.UserSettings
import com.example.hajiri.model.WageMode
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

class HajiriRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    fun observeProfile(userId: String): Flow<UserProfile?> {
        val docRef = userDoc(userId)
        return docRef.snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                logFlowFirestoreErrorIfSignedIn(error, userId, OperationType.GET, docRef.path)
                throw error
            }
    }

    fun observeAttendance(userId: String): Flow<Map<String, AttendanceRecord>> {
        val colRef = userDoc(userId).collection("attendance")
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    AttendanceFirestoreDto::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).associate { dto ->
                    dto.id to AttendanceRecord(
                        status = AttendanceStatus.fromId(dto.status) ?: AttendanceStatus.PRESENT,
                        note = dto.note,
                        createdAt = dto.createdAt,
                        updatedAt = dto.updatedAt
                    )
                }
            }
            .catch { error ->
                logFlowFirestoreErrorIfSignedIn(error, userId, OperationType.LIST, colRef.path)
                throw error
            }
    }

    fun observeOvertime(userId: String): Flow<Map<String, Float>> {
        val colRef = userDoc(userId).collection("overtime")
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    OvertimeRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).associate { record ->
                    record.dateKey to record.hours.toFloat()
                }
            }
            .catch { error ->
                logFlowFirestoreErrorIfSignedIn(error, userId, OperationType.LIST, colRef.path)
                throw error
            }
    }

    fun observeAdvances(userId: String): Flow<List<AdvanceRecord>> {
        val colRef = userDoc(userId).collection("advances")
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    AdvanceRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedWith(
                    compareByDescending<AdvanceRecord> { it.dateKey }
                        .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                )
            }
            .catch { error ->
                logFlowFirestoreErrorIfSignedIn(error, userId, OperationType.LIST, colRef.path)
                throw error
            }
    }

    fun observeSettings(userId: String): Flow<UserSettings> {
        val docRef = userDoc(userId).collection("settings").document(SETTINGS_DOC_ID)
        return docRef.snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) UserSettings()
                else snapshot.toObject(
                    UserSettings::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: UserSettings()
            }
            .catch { error ->
                logFlowFirestoreErrorIfSignedIn(error, userId, OperationType.GET, docRef.path)
                throw error
            }
    }

    suspend fun ensureUserProfile(name: String, email: String, phone: String? = null) {
        val uid = requireUserId()
        val docRef = userDoc(uid)
        val cleanName = name.ifBlank { "हाजिरी यूजर" }.take(100)
        val cleanEmail = email.take(150)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "name" to cleanName,
                    "email" to cleanEmail,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!phone.isNullOrBlank()) {
                    payload["phone"] = phone.trim().take(20)
                }
                docRef.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                val existingName = snap.getString("name").orEmpty()
                if (existingName.isBlank() && cleanName.isNotBlank()) {
                    updates["name"] = cleanName
                }
                if (cleanEmail.isNotBlank() && snap.getString("email").orEmpty() != cleanEmail) {
                    updates["email"] = cleanEmail
                }
                if (!phone.isNullOrBlank()) {
                    updates["phone"] = phone.trim().take(20)
                }
                if (updates.size > 1) {
                    docRef.update(updates).await()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun updateProfileDetails(name: String, phone: String) {
        val uid = requireUserId()
        val docRef = userDoc(uid)
        val cleanName = name.trim().ifBlank { "हाजिरी यूजर" }.take(100)
        val cleanPhone = phone.trim().take(20)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "name" to cleanName,
                        "email" to (auth.currentUser?.email ?: "").take(150),
                        "phone" to cleanPhone,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "name" to cleanName,
                        "phone" to cleanPhone,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun setAttendance(dateKey: String, status: AttendanceStatus, note: String? = null) {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("attendance").document(dateKey)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "status" to status.id,
                        "note" to (note ?: "").trim().take(500),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val resolvedNote = (note ?: snap.getString("note") ?: "").trim().take(500)
                docRef.update(
                    mapOf(
                        "status" to status.id,
                        "note" to resolvedNote,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun removeAttendance(dateKey: String) {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("attendance").document(dateKey)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun toggleOrCycleAttendance(dateKey: String): AttendanceStatus? {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("attendance").document(dateKey)
        val statuses = listOf(
            AttendanceStatus.PRESENT,
            AttendanceStatus.ABSENT,
            AttendanceStatus.HALF,
            AttendanceStatus.LEAVE
        )
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "status" to AttendanceStatus.PRESENT.id,
                        "note" to "",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                AttendanceStatus.PRESENT
            } else {
                val curStatus = AttendanceStatus.fromId(snap.getString("status"))
                val curIdx = if (curStatus != null) statuses.indexOf(curStatus) else -1
                if (curIdx == statuses.lastIndex) {
                    docRef.delete().await()
                    null
                } else {
                    val next = statuses[(curIdx + 1).coerceIn(0, statuses.lastIndex)]
                    docRef.update(
                        mapOf(
                            "status" to next.id,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    next
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun saveNote(dateKey: String, note: String) {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("attendance").document(dateKey)
        val cleanNote = note.trim().take(500)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "status" to AttendanceStatus.PRESENT.id,
                        "note" to cleanNote,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "note" to cleanNote,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun addOvertime(dateKey: String, hours: Float) {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("overtime").document(dateKey)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val capped = hours.toDouble().coerceIn(0.01, 24.0)
                docRef.set(
                    mapOf(
                        "hours" to capped,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val prev = snap.getDouble("hours") ?: 0.0
                val capped = (prev + hours.toDouble()).coerceIn(0.01, 24.0)
                docRef.update(
                    mapOf(
                        "hours" to capped,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun addAdvance(amount: Double, reason: String, dateKey: String): String {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("advances").document()
        try {
            docRef.set(
                mapOf(
                    "amount" to amount.coerceIn(0.01, 10000000.0),
                    "reason" to reason.trim().take(200),
                    "dateKey" to dateKey,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteAdvance(id: String) {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("advances").document(id)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun saveSettings(
        salary: Double,
        dailyWage: Double,
        wageMode: String,
        otRate: Double,
        workDays: Int,
        themeStyle: String = AppThemeStyle.LIQUID_LIGHT.id
    ) {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("settings").document(SETTINGS_DOC_ID)
        val cleanSalary = salary.coerceIn(0.0, 100000000.0)
        val cleanDailyWage = dailyWage.coerceIn(0.0, 10000000.0)
        val cleanWageMode = com.example.hajiri.model.WageMode.fromId(wageMode).id
        val cleanOtRate = otRate.coerceIn(0.0, 1000000.0)
        val cleanWorkDays = workDays.coerceIn(1, 31)
        val cleanTheme = com.example.hajiri.model.AppThemeStyle.fromId(themeStyle).id
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "salary" to cleanSalary,
                        "dailyWage" to cleanDailyWage,
                        "wageMode" to cleanWageMode,
                        "otRate" to cleanOtRate,
                        "workDays" to cleanWorkDays,
                        "themeStyle" to cleanTheme,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "salary" to cleanSalary,
                        "dailyWage" to cleanDailyWage,
                        "wageMode" to cleanWageMode,
                        "otRate" to cleanOtRate,
                        "workDays" to cleanWorkDays,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun saveThemeStyle(themeStyle: com.example.hajiri.model.AppThemeStyle) {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("settings").document(SETTINGS_DOC_ID)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "salary" to 0.0,
                        "dailyWage" to 0.0,
                        "wageMode" to com.example.hajiri.model.WageMode.MONTHLY.id,
                        "otRate" to 50.0,
                        "workDays" to 26,
                        "themeStyle" to themeStyle.id,
                        "settledMonths" to emptyList<String>(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "themeStyle" to themeStyle.id,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun setMonthSettled(monthKey: String, isSettled: Boolean) {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("settings").document(SETTINGS_DOC_ID)
        val cleanMonth = monthKey.trim().take(7)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val initialList = if (isSettled && cleanMonth.isNotBlank()) listOf(cleanMonth) else emptyList()
                docRef.set(
                    mapOf(
                        "salary" to 0.0,
                        "dailyWage" to 0.0,
                        "wageMode" to WageMode.MONTHLY.id,
                        "otRate" to 50.0,
                        "workDays" to 26,
                        "themeStyle" to AppThemeStyle.LIQUID_LIGHT.id,
                        "settledMonths" to initialList,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val existing = (snap.get("settledMonths") as? List<*>)
                    ?.mapNotNull { it as? String }
                    ?.toMutableSet()
                    ?: mutableSetOf()
                if (cleanMonth.isNotBlank()) {
                    if (isSettled) existing.add(cleanMonth) else existing.remove(cleanMonth)
                }
                val updatedList = existing.sorted().takeLast(120)
                docRef.update(
                    mapOf(
                        "settledMonths" to updatedList,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun clearMonth(monthKey: String) {
        val uid = requireUserId()
        val userRef = userDoc(uid)
        try {
            val attDocs = userRef.collection("attendance").get().await()
                .documents.filter { it.id.startsWith(monthKey) }
            val otDocs = userRef.collection("overtime").get().await()
                .documents.filter { it.id.startsWith(monthKey) }
            val advDocs = userRef.collection("advances").get().await()
                .documents.filter { (it.getString("dateKey") ?: "").startsWith(monthKey) }

            deleteRefsInBatches((attDocs + otDocs + advDocs).map { it.reference })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "${userRef.path}/*")
            throw e
        }
    }

    suspend fun clearAllData() {
        val uid = requireUserId()
        val userRef = userDoc(uid)
        try {
            val attDocs = userRef.collection("attendance").get().await().documents
            val otDocs = userRef.collection("overtime").get().await().documents
            val advDocs = userRef.collection("advances").get().await().documents
            deleteRefsInBatches((attDocs + otDocs + advDocs).map { it.reference })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "${userRef.path}/*")
            throw e
        }
    }

    private suspend fun deleteRefsInBatches(refs: List<com.google.firebase.firestore.DocumentReference>) {
        if (refs.isEmpty()) return
        refs.chunked(450).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { batch.delete(it) }
            batch.commit().await()
        }
    }

    private fun logFlowFirestoreErrorIfSignedIn(
        error: Throwable,
        userId: String,
        operationType: OperationType,
        path: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }

    companion object {
        const val SETTINGS_DOC_ID = "default"
    }
}
