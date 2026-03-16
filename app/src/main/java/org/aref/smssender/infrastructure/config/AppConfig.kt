package org.aref.smssender.infrastructure.config

import android.content.Context
import android.content.SharedPreferences
import org.aref.smssender.BuildConfig
import java.util.UUID

class AppConfig(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(BuildConfig.PREFS_NAME, Context.MODE_PRIVATE)

    var apiKey: String
        get() = prefs.getString(BuildConfig.KEY_API_KEY, null) ?: generateAndSaveApiKey()
        set(value) = prefs.edit().putString(BuildConfig.KEY_API_KEY, value).apply()

    var port: Int
        get() = prefs.getInt(BuildConfig.KEY_PORT, BuildConfig.APP_PORT)
        set(value) = prefs.edit().putInt(BuildConfig.KEY_PORT, value).apply()

    var defaultSimSlot: Int
        get() = prefs.getInt(BuildConfig.KEY_DEFAULT_SIM_SLOT, BuildConfig.DEFAULT_SIM_SLOT)
        set(value) = prefs.edit().putInt(BuildConfig.KEY_DEFAULT_SIM_SLOT, value).apply()

    private fun generateAndSaveApiKey(): String {
        val newKey = UUID.randomUUID().toString()
        prefs.edit().putString(BuildConfig.KEY_API_KEY, newKey).apply()
        return newKey
    }
}

