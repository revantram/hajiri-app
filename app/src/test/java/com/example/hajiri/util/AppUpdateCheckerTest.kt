package com.example.hajiri.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AppUpdateCheckerTest {

    @Test
    fun isNewerVersion_handlesSemanticAndTagFormats() {
        assertTrue(AppUpdateChecker.isNewerVersion("v1.0.1", "1.0"))
        assertTrue(AppUpdateChecker.isNewerVersion("v1.1.0", "1.0.9"))
        assertTrue(AppUpdateChecker.isNewerVersion("2.0", "1.9.9"))
        assertFalse(AppUpdateChecker.isNewerVersion("v1.0", "1.0"))
        assertFalse(AppUpdateChecker.isNewerVersion("v1.0.0", "1.0"))
        assertFalse(AppUpdateChecker.isNewerVersion("v1.0.1", "1.0.2"))
    }

    @Test
    fun normalizeRepoSlug_extractsOwnerAndRepoFromUrlsAndSlugs() {
        assertEquals("revantram/hajiri-app", AppUpdateChecker.normalizeRepoSlug("revantram/hajiri-app"))
        assertEquals(
            "revantram/hajiri-app",
            AppUpdateChecker.normalizeRepoSlug("https://github.com/revantram/hajiri-app.git")
        )
        assertEquals(
            "myuser/my-hajiri",
            AppUpdateChecker.normalizeRepoSlug("https://github.com/myuser/my-hajiri/releases")
        )
        assertEquals("", AppUpdateChecker.normalizeRepoSlug("invalid"))
    }

    @Test
    fun parseReleaseJson_prefersReleaseApkAndDetectsUpdate() {
        val sampleJson = """
            {
              "tag_name": "v1.0.4",
              "name": "Hajiri v1.0.4",
              "body": "PDF Bill + Theme Update",
              "html_url": "https://github.com/revantram/hajiri-app/releases/tag/v1.0.4",
              "assets": [
                {
                  "name": "Hajiri-v1.0-Debug.apk",
                  "browser_download_url": "https://github.com/revantram/hajiri-app/releases/download/v1.0.4/Hajiri-v1.0-Debug.apk"
                },
                {
                  "name": "Hajiri-v1.0-Release.apk",
                  "browser_download_url": "https://github.com/revantram/hajiri-app/releases/download/v1.0.4/Hajiri-v1.0-Release.apk"
                }
              ]
            }
        """.trimIndent()

        val parsed = AppUpdateChecker.parseReleaseJson(sampleJson, currentVersion = "1.0")
        assertTrue(parsed.isUpdateAvailable)
        assertEquals("v1.0.4", parsed.latestTag)
        assertEquals(
            "https://github.com/revantram/hajiri-app/releases/download/v1.0.4/Hajiri-v1.0-Release.apk",
            parsed.downloadUrl
        )
    }
}
