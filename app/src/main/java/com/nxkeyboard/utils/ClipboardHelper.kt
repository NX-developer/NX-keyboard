package com.nxkeyboard.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.KeyEvent
import android.view.inputmethod.InputConnection

object ClipboardHelper {

    fun copy(context: Context, ic: InputConnection?): Boolean {
        if (ic == null) return false
        val selected = ic.getSelectedText(0) ?: return false
        if (selected.isEmpty()) return false
        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cb.setPrimaryClip(ClipData.newPlainText("NXKeyboard", selected))
        addToHistory(context, selected.toString())
        return true
    }

    fun cut(context: Context, ic: InputConnection?): Boolean {
        if (ic == null) return false
        val didCopy = copy(context, ic)
        if (didCopy) ic.commitText("", 1)
        return didCopy
    }

    fun paste(context: Context, ic: InputConnection?): Boolean {
        if (ic == null) return false
        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = cb.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString() ?: return false
        ic.commitText(text, 1)
        return true
    }

    fun selectAll(ic: InputConnection?) {
        ic?.performContextMenuAction(android.R.id.selectAll)
    }

    fun moveCursor(ic: InputConnection?, direction: Int) {
        if (ic == null) return
        val keyCode = if (direction < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    private const val HISTORY_KEY = "clipboard_history"
    private const val HISTORY_TIMES_KEY = "clipboard_history_times"
    private const val PINNED_KEY = "clipboard_pinned"
    private const val MAX_HISTORY = 20
    private const val MAX_PINNED = 50

    fun addToHistory(context: Context, text: String) {
        if (text.isBlank()) return
        pruneExpired(context)
        val list = getHistory(context).toMutableList()
        val times = getTimestamps(context).toMutableList()
        val existingIdx = list.indexOf(text)
        if (existingIdx >= 0) {
            list.removeAt(existingIdx)
            if (existingIdx < times.size) times.removeAt(existingIdx)
        }
        list.add(0, text)
        times.add(0, System.currentTimeMillis())
        val trimmedList = list.take(MAX_HISTORY)
        val trimmedTimes = times.take(MAX_HISTORY)
        PrefsHelper.get(context).edit()
            .putString(HISTORY_KEY, trimmedList.joinToString("\u0001"))
            .putString(HISTORY_TIMES_KEY, trimmedTimes.joinToString(","))
            .apply()
    }

    fun getHistory(context: Context): List<String> {
        pruneExpired(context)
        val raw = PrefsHelper.get(context).getString(HISTORY_KEY, "") ?: ""
        return if (raw.isEmpty()) emptyList() else raw.split("\u0001")
    }

    fun removeFromHistory(context: Context, text: String) {
        val list = getHistory(context).toMutableList()
        val times = getTimestamps(context).toMutableList()
        val idx = list.indexOf(text)
        if (idx < 0) return
        list.removeAt(idx)
        if (idx < times.size) times.removeAt(idx)
        PrefsHelper.get(context).edit()
            .putString(HISTORY_KEY, list.joinToString("\u0001"))
            .putString(HISTORY_TIMES_KEY, times.joinToString(","))
            .apply()
    }

    fun clearHistory(context: Context) {
        PrefsHelper.get(context).edit()
            .remove(HISTORY_KEY)
            .remove(HISTORY_TIMES_KEY)
            .apply()
    }

    private fun getTimestamps(context: Context): List<Long> {
        val raw = PrefsHelper.get(context).getString(HISTORY_TIMES_KEY, "") ?: ""
        if (raw.isEmpty()) return emptyList()
        return raw.split(",").mapNotNull { it.toLongOrNull() }
    }

    private fun pruneExpired(context: Context) {
        val expiryPref = PrefsHelper.getString(context, "clipboard_expiry_hours", "24")
        val expiryHours: Long = when (expiryPref) {
            "0" -> 0L
            "custom" -> PrefsHelper.getString(context, "clipboard_expiry_custom_hours", "48").toLongOrNull() ?: 48L
            else -> expiryPref.toLongOrNull() ?: 24L
        }
        if (expiryHours <= 0L) return
        val expiryMs = expiryHours * 3600L * 1000L
        val now = System.currentTimeMillis()
        val raw = PrefsHelper.get(context).getString(HISTORY_KEY, "") ?: ""
        if (raw.isEmpty()) return
        val list = raw.split("\u0001").toMutableList()
        val times = getTimestamps(context).toMutableList()
        var changed = false
        var i = 0
        while (i < list.size) {
            val ts = if (i < times.size) times[i] else now
            if (now - ts > expiryMs) {
                list.removeAt(i)
                if (i < times.size) times.removeAt(i)
                changed = true
            } else {
                i++
            }
        }
        if (changed) {
            PrefsHelper.get(context).edit()
                .putString(HISTORY_KEY, list.joinToString("\u0001"))
                .putString(HISTORY_TIMES_KEY, times.joinToString(","))
                .apply()
        }
    }

    fun getPinned(context: Context): List<String> {
        val raw = PrefsHelper.get(context).getString(PINNED_KEY, "") ?: ""
        return if (raw.isEmpty()) emptyList() else raw.split("\u0001")
    }

    fun pin(context: Context, text: String) {
        if (text.isBlank()) return
        val list = getPinned(context).toMutableList()
        list.remove(text)
        list.add(0, text)
        val trimmed = list.take(MAX_PINNED)
        PrefsHelper.get(context).edit()
            .putString(PINNED_KEY, trimmed.joinToString("\u0001"))
            .apply()
    }

    fun unpin(context: Context, text: String) {
        val list = getPinned(context).toMutableList()
        list.remove(text)
        PrefsHelper.get(context).edit()
            .putString(PINNED_KEY, list.joinToString("\u0001"))
            .apply()
    }

    fun isPinned(context: Context, text: String): Boolean {
        return text in getPinned(context)
    }
}
