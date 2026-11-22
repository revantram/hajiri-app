package com.example.hajiri.util

import android.content.Context
import com.example.hajiri.model.AppThemeStyle

/**
 * Synchronous local persistence for the user's selected UI theme so app startup
 * renders the chosen theme immediately (including OLED Black) without flicker or restart.
 */
object ThemePreferences {
    private const val PREFS_NAME = "hajiri_theme_prefs"
    private const val KEY_THEME_STYLE = "selected_theme_style"

    fun getSavedTheme(context: Context): AppThemeStyle {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedId = prefs.getString(KEY_THEME_STYLE, null)
        return AppThemeStyle.fromId(savedId)
    }

    fun saveTheme(context: Context, themeStyle: AppThemeStyle) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_STYLE, themeStyle.id)
            .apply()
    }
}
