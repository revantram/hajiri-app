package com.example.hajiri.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class AppReleaseInfo(
    val currentVersion: String,
    val latestTag: String,
    val releaseName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val releasePageUrl: String,
    val isUpdateAvailable: Boolean
)

sealed class UpdateCheckState {
    data object Idle : UpdateCheckState()
    data object Checking : UpdateCheckState()
    data class UpToDate(val currentVersion: String, val latestTag: String) : UpdateCheckState()
    data class UpdateAvailable(val releaseInfo: AppReleaseInfo) : UpdateCheckState()
    data class Error(val message: String) : UpdateCheckState()
}

sealed class UpdateDownloadState {
    data object Idle : UpdateDownloadState()
    data class Downloading(
        val percent: Int,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : UpdateDownloadState()

    data class ReadyToInstall(
        val apkFile: File,
        val versionTag: String
    ) : UpdateDownloadState()

    data class Error(val message: String) : UpdateDownloadState()
}

object AppUpdateChecker {

    private const val PREFS_NAME = "hajiri_update_prefs"
    private const val KEY_AUTO_CHECK = "auto_update_check_enabled"
    private const val KEY_REPO_SLUG = "github_repo_slug"
    const val DEFAULT_GITHUB_REPO = "revantram/hajiri-app"

    fun isAutoCheckEnabled(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_CHECK, true)
    }

    fun setAutoCheckEnabled(context: Context, enabled: Boolean) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_CHECK, enabled)
            .apply()
    }

    fun getSavedRepoSlug(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_REPO_SLUG, DEFAULT_GITHUB_REPO).orEmpty()
        return normalizeRepoSlug(raw).ifBlank { DEFAULT_GITHUB_REPO }
    }

    fun saveRepoSlug(context: Context, input: String): String {
        val clean = normalizeRepoSlug(input).ifBlank { DEFAULT_GITHUB_REPO }
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_REPO_SLUG, clean)
            .apply()
        return clean
    }

    fun normalizeRepoSlug(input: String): String {
        val trimmed = input.trim()
            .removePrefix("https://github.com/")
            .removePrefix("http://github.com/")
            .removePrefix("github.com/")
            .removeSuffix(".git")
            .trim('/')
        val parts = trimmed.split('/').filter { it.isNotBlank() }
        return if (parts.size >= 2) "${parts[0]}/${parts[1]}" else ""
    }

    fun buildLatestReleaseUrl(repoSlug: String): String {
        val cleanSlug = normalizeRepoSlug(repoSlug).ifBlank { DEFAULT_GITHUB_REPO }
        return "https://github.com/$cleanSlug/releases/latest"
    }

    fun getInstalledVersionName(context: Context): String {
        return try {
            val pkgManager = context.packageManager
            val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pkgManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                pkgManager.getPackageInfo(context.packageName, 0)
            }
            pkgInfo.versionName?.ifBlank { "1.6" } ?: "1.6"
        } catch (_: Exception) {
            "1.6"
        }
    }

    internal fun parseVersionParts(rawVersion: String): List<Int> {
        val cleaned = rawVersion.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore('-')
            .substringBefore('+')
        if (cleaned.isBlank()) return listOf(0)
        return cleaned.split('.').map { segment ->
            segment.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
        }
    }

    fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
        val remote = parseVersionParts(remoteTag)
        val local = parseVersionParts(currentVersion)
        val maxLen = maxOf(remote.size, local.size)
        for (i in 0 until maxLen) {
            val r = remote.getOrElse(i) { 0 }
            val l = local.getOrElse(i) { 0 }
            if (r != l) return r > l
        }
        return false
    }

    internal fun parseReleaseJson(jsonBody: String, currentVersion: String): AppReleaseInfo {
        val obj = JSONObject(jsonBody)
        val tagName = obj.optString("tag_name", "").ifBlank { "v$currentVersion" }
        val releaseName = obj.optString("name", "").ifBlank { "नया वर्ज़न $tagName" }
        val body = obj.optString("body", "").trim()
        val htmlUrl = obj.optString("html_url", "")

        var preferredApkUrl = ""
        var fallbackApkUrl = ""
        val assets = obj.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val assetName = asset.optString("name", "")
                val assetUrl = asset.optString("browser_download_url", "")
                if (assetName.endsWith(".apk", ignoreCase = true) && assetUrl.isNotBlank()) {
                    if (assetName.contains("release", ignoreCase = true) && preferredApkUrl.isBlank()) {
                        preferredApkUrl = assetUrl
                    } else if (fallbackApkUrl.isBlank()) {
                        fallbackApkUrl = assetUrl
                    }
                }
            }
        }

        val fallbackPage = htmlUrl.ifBlank { buildLatestReleaseUrl(DEFAULT_GITHUB_REPO) }
        val resolvedDownloadUrl = preferredApkUrl.ifBlank { fallbackApkUrl }.ifBlank { fallbackPage }
        val isNewer = isNewerVersion(tagName, currentVersion)

        return AppReleaseInfo(
            currentVersion = currentVersion,
            latestTag = tagName,
            releaseName = releaseName,
            releaseNotes = body,
            downloadUrl = resolvedDownloadUrl,
            releasePageUrl = htmlUrl,
            isUpdateAvailable = isNewer
        )
    }

    fun formatBytesMb(bytes: Long): String {
        if (bytes <= 0L) return "0 MB"
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return if (mb >= 0.1) {
            String.format(Locale.US, "%.1f MB", mb)
        } else {
            val kb = bytes / 1024L
            "$kb KB"
        }
    }

    suspend fun checkForUpdate(
        context: Context,
        repoSlug: String
    ): UpdateCheckState = withContext(Dispatchers.IO) {
        val currentVersion = getInstalledVersionName(context)
        val cleanSlug = normalizeRepoSlug(repoSlug).ifBlank { DEFAULT_GITHUB_REPO }
        val apiUrl = "https://api.github.com/repos/$cleanSlug/releases/latest"

        var connection: HttpURLConnection? = null
        try {
            connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Hajiri-Android-App")
            }

            val code = connection.responseCode
            if (code == 404) {
                return@withContext UpdateCheckState.Error("इस रिपॉज़िटरी में अभी कोई रिलीज़ नहीं मिली ($cleanSlug)")
            }
            if (code !in 200..299) {
                return@withContext UpdateCheckState.Error("अपडेट सर्वर से जवाब नहीं मिला (HTTP $code)")
            }

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val info = parseReleaseJson(responseText, currentVersion)
            if (info.isUpdateAvailable) {
                UpdateCheckState.UpdateAvailable(info)
            } else {
                UpdateCheckState.UpToDate(currentVersion = currentVersion, latestTag = info.latestTag)
            }
        } catch (_: Exception) {
            UpdateCheckState.Error("इंटरनेट कनेक्शन जाँचें और फिर से प्रयास करें")
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Downloads the release APK inside the app into cacheDir/updates/ while reporting
     * real-time download progress (percentage, downloaded bytes, total bytes).
     */
    suspend fun downloadApkWithProgress(
        context: Context,
        downloadUrl: String,
        versionTag: String,
        onProgress: (percent: Int, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val updatesDir = File(appContext.cacheDir, "updates").apply { mkdirs() }
        val safeTag = versionTag.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val targetFile = File(updatesDir, "Hajiri-$safeTag.apk")
        val tempFile = File(updatesDir, "Hajiri-$safeTag.apk.part")

        var connection: HttpURLConnection? = null
        try {
            // Follow up to 5 HTTP redirects (GitHub releases redirect to objects.githubusercontent.com)
            var currentUrl = downloadUrl
            var redirects = 0
            while (redirects < 5) {
                connection?.disconnect()
                val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    instanceFollowRedirects = true
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    setRequestProperty("User-Agent", "Hajiri-Android-App")
                }
                val code = conn.responseCode
                if (code in 300..399) {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (location.isNullOrBlank()) error("Redirect location missing")
                    currentUrl = location
                    redirects++
                } else {
                    connection = conn
                    break
                }
            }

            val activeConn = connection ?: error("कनेक्शन स्थापित नहीं हो सका")
            val code = activeConn.responseCode
            if (code !in 200..299) {
                error("डाउनलोड विफल रहा (HTTP $code)")
            }

            val totalBytes = activeConn.contentLengthLong
            var downloadedBytes = 0L
            var lastReportedPct = -1
            var lastReportedTime = 0L

            onProgress(0, 0L, totalBytes)

            activeConn.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloadedBytes += read

                        val pct = if (totalBytes > 0L) {
                            ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
                        } else {
                            -1
                        }
                        val now = System.currentTimeMillis()
                        if (pct != lastReportedPct || (now - lastReportedTime) >= 120L) {
                            lastReportedPct = pct
                            lastReportedTime = now
                            onProgress(pct.coerceAtLeast(0), downloadedBytes, totalBytes)
                        }
                    }
                    output.flush()
                }
            }

            if (downloadedBytes <= 0L) {
                tempFile.delete()
                error("डाउनलोड की गई फ़ाइल खाली है")
            }

            if (targetFile.exists()) targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            onProgress(100, downloadedBytes, if (totalBytes > 0L) totalBytes else downloadedBytes)
            Result.success(targetFile)
        } catch (e: Exception) {
            runCatching { tempFile.delete() }
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Launches Android's system package installer for a downloaded APK using FileProvider.
     * Returns true if installer opened, or false if the user was directed to allow Unknown App Sources.
     */
    fun launchApkInstaller(context: Context, apkFile: File): Boolean {
        val appContext = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !appContext.packageManager.canRequestPackageInstalls()
        ) {
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${appContext.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(settingsIntent)
            return false
        }

        val apkUri = FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            apkFile
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        appContext.startActivity(installIntent)
        return true
    }
}
