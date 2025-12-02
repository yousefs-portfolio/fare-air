package com.fairair.app.persistence

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

/**
 * Android implementation of Settings factory.
 * Uses SharedPreferences for persistent storage.
 */
private var appContext: Context? = null

/**
 * Initialize with application context.
 * Must be called from Application.onCreate() or MainActivity.onCreate().
 */
fun initializeLocalStorage(context: Context) {
    appContext = context.applicationContext
}

actual fun createSettings(): Settings {
    val context = appContext
        ?: throw IllegalStateException("LocalStorage not initialized. Call initializeLocalStorage(context) first.")
    val sharedPreferences = context.getSharedPreferences("fairair_prefs", Context.MODE_PRIVATE)
    return SharedPreferencesSettings(sharedPreferences)
}

/**
 * Android implementation of currentTimeMillis.
 */
actual fun currentTimeMillis(): Long = System.currentTimeMillis()
