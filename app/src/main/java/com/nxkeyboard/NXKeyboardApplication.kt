package com.nxkeyboard

import android.app.Application

class NXKeyboardApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}

object EmojiCompatState {
    @Volatile var ready: Boolean = false
    private val listeners = mutableListOf<() -> Unit>()

    @Synchronized
    fun addListener(listener: () -> Unit) {
        listeners += listener
        if (ready) listener()
    }

    @Synchronized
    fun removeListener(listener: () -> Unit) {
        listeners -= listener
    }

    @Synchronized
    fun notifyReady() {
        for (l in listeners.toList()) {
            try { l() } catch (_: Throwable) {}
        }
    }
}
