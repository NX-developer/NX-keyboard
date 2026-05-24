package com.nxkeyboard.keyboard

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import com.nxkeyboard.R
import com.nxkeyboard.theme.ThemeManager
import com.nxkeyboard.utils.HapticHelper
import com.nxkeyboard.utils.PrefsHelper

class SuggestionBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    interface Callback {
        fun onUndo()
        fun onRedo()
        fun onAiCorrect()
        fun onAiTranslate()
        fun onClipboard()
        fun onEmoji()
        fun onVoice()
        fun onSelectionMode()
        fun onSettings()
        fun onCollapse()
    }

    private var callback: Callback? = null
    private var themeManager: ThemeManager? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(4), dp(4), dp(4), dp(4))
    }

    fun configure(themeManager: ThemeManager, callback: Callback) {
        this.themeManager = themeManager
        this.callback = callback
        rebuild()
    }

    fun rebuild() {
        removeAllViews()
        val ctx = context
        val showAi = PrefsHelper.getBoolean(ctx, "show_ai_button", true)
        val showTranslate = PrefsHelper.getBoolean(ctx, "show_translate_button", false)
        val showUndo = PrefsHelper.getBoolean(ctx, "show_undo_redo", true)
        val showVoice = PrefsHelper.getBoolean(ctx, "show_voice_button", true)
        val showClipboard = PrefsHelper.getBoolean(ctx, "show_clipboard_button", true)
        val showSelection = PrefsHelper.getBoolean(ctx, "show_selection_button", true)

        if (showUndo) {
            addIcon(R.drawable.ic_nx_undo) { callback?.onUndo() }
            addIcon(R.drawable.ic_nx_redo) { callback?.onRedo() }
        }
        if (showAi) addIcon(R.drawable.ic_nx_ai) { callback?.onAiCorrect() }
        if (showTranslate) addIcon(R.drawable.ic_nx_translate) { callback?.onAiTranslate() }
        if (showVoice) addIcon(R.drawable.ic_nx_voice) { callback?.onVoice() }
        if (showSelection) addIcon(R.drawable.ic_nx_select) { callback?.onSelectionMode() }
        if (showClipboard) addIcon(R.drawable.ic_nx_clipboard) { callback?.onClipboard() }
        addIcon(R.drawable.ic_nx_settings) { callback?.onSettings() }
        addIcon(R.drawable.ic_nx_chevron_down) { callback?.onCollapse() }
        applyTheme()
    }

    private fun addIcon(resId: Int, onClick: () -> Unit) {
        val view = ImageView(context).apply {
            setImageResource(resId)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(dp(6), dp(8), dp(6), dp(8))
            setOnClickListener {
                HapticHelper.keyPress(this)
                onClick()
            }
        }
        addView(view, LayoutParams(0, dp(40), 1f))
    }

    fun applyTheme() {
        val dark = themeManager?.isDarkActive() ?: false
        val bg = if (dark) Color.parseColor("#0F0F0F") else Color.parseColor("#E8E8E8")
        val tintColor = if (dark) Color.WHITE else Color.parseColor("#212121")
        setBackgroundColor(bg)
        val tint = ColorStateList.valueOf(tintColor)
        for (i in 0 until childCount) {
            (getChildAt(i) as? ImageView)?.imageTintList = tint
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
