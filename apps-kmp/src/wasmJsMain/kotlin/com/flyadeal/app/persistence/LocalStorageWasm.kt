package com.flyadeal.app.persistence

import com.russhwolf.settings.Settings
import com.russhwolf.settings.StorageSettings
import kotlinx.browser.localStorage

/**
 * WasmJS implementation of Settings factory.
 * Uses browser localStorage for persistent storage.
 */
actual fun createSettings(): Settings {
    return StorageSettings(localStorage)
}

/**
 * JS interface to get current timestamp.
 */
@JsFun("() => Date.now()")
private external fun dateNow(): Double

/**
 * WasmJS implementation of currentTimeMillis.
 * Uses JavaScript Date.now() for current timestamp.
 */
actual fun currentTimeMillis(): Long {
    return dateNow().toLong()
}
