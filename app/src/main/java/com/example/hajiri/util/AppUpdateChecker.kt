package com.example.hajiri.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

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
            pkgInfo.versionName?.ifBlank { "1.2" } ?: "1.2"
        } catch (_: Exception) {
            "1.2"
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
}
