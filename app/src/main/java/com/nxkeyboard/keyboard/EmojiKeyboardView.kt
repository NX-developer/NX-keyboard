package com.nxkeyboard.keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nxkeyboard.R
import com.nxkeyboard.theme.ThemeManager
import com.nxkeyboard.utils.HapticHelper
import com.nxkeyboard.utils.RecentEmojiManager

class EmojiKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    private var themeManager: ThemeManager? = null
    private var onEmojiSelected: ((String) -> Unit)? = null
    private var onBackspace: (() -> Unit)? = null
    private var onCloseEmoji: (() -> Unit)? = null

    private val tabsContainer: LinearLayout
    private val grid: RecyclerView
    private val bottomBar: LinearLayout
    private var currentCategory: EmojiData.Category = EmojiData.Category.SMILEYS

    init {
        orientation = VERTICAL
        setPadding(0, 0, 0, 0)

        tabsContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val tabsScroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            addView(tabsContainer, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }
        addView(tabsScroll, LayoutParams(LayoutParams.MATCH_PARENT, dp(40)))

        grid = RecyclerView(context).apply {
            layoutManager = GridLayoutManager(context, 8)
            overScrollMode = OVER_SCROLL_NEVER
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        addView(grid, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        bottomBar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        addView(bottomBar, LayoutParams(LayoutParams.MATCH_PARENT, dp(44)))
    }

    fun configure(
        themeManager: ThemeManager,
        onEmojiSelected: (String) -> Unit,
        onBackspace: () -> Unit,
        onCloseEmoji: () -> Unit
    ) {
        this.themeManager = themeManager
        this.onEmojiSelected = onEmojiSelected
        this.onBackspace = onBackspace
        this.onCloseEmoji = onCloseEmoji
        setupTabs()
        setupBottomBar()
        applyTheme()
        showCategory(EmojiData.Category.SMILEYS)
    }

    private fun setupTabs() {
        tabsContainer.removeAllViews()
        val categories = EmojiData.Category.values()
        for (category in categories) {
            val tab = TextView(context).apply {
                text = category.icon
                textSize = 18f
                gravity = Gravity.CENTER
                typeface = Typeface.DEFAULT
                setPadding(dp(14), dp(8), dp(14), dp(8))
                setOnClickListener {
                    HapticHelper.keyPress(this)
                    showCategory(category)
                }
            }
            tabsContainer.addView(tab, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
        }
    }

    private fun setupBottomBar() {
        bottomBar.removeAllViews()
        val abcBtn = TextView(context).apply {
            text = "ABC"
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(10), dp(16), dp(10))
            setOnClickListener {
                HapticHelper.keyPress(this)
                onCloseEmoji?.invoke()
            }
        }
        bottomBar.addView(abcBtn, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))

        val backBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_nx_backspace)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(dp(16), dp(10), dp(16), dp(10))
            setOnClickListener {
                HapticHelper.keyPress(this)
                onBackspace?.invoke()
            }
        }
        bottomBar.addView(backBtn, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
    }

    private fun showCategory(category: EmojiData.Category) {
        currentCategory = category
        val emojis = if (category == EmojiData.Category.RECENT) {
            RecentEmojiManager.get(context)
        } else {
            EmojiData.forCategory(category)
        }
        grid.adapter = EmojiAdapter(emojis, themeManager?.isDarkActive() ?: false) { emoji ->
            HapticHelper.keyPress(this)
            RecentEmojiManager.add(context, emoji)
            onEmojiSelected?.invoke(emoji)
        }
        for (i in 0 until tabsContainer.childCount) {
            val tab = tabsContainer.getChildAt(i) as? TextView ?: continue
            val isActive = i == EmojiData.Category.values().indexOf(category)
            tab.alpha = if (isActive) 1.0f else 0.55f
        }
    }

    fun applyTheme() {
        val dark = themeManager?.isDarkActive() ?: false
        val bg = if (dark) Color.parseColor("#0A0A0A") else Color.parseColor("#F2F2F2")
        val barBg = if (dark) Color.parseColor("#151515") else Color.parseColor("#E0E0E0")
        val text = if (dark) Color.WHITE else Color.parseColor("#212121")
        setBackgroundColor(bg)
        (tabsContainer.parent as? View)?.setBackgroundColor(barBg)
        bottomBar.setBackgroundColor(barBg)
        for (i in 0 until tabsContainer.childCount) {
            (tabsContainer.getChildAt(i) as? TextView)?.setTextColor(text)
        }
        for (i in 0 until bottomBar.childCount) {
            val child = bottomBar.getChildAt(i)
            if (child is TextView) child.setTextColor(text)
            if (child is ImageView) child.imageTintList = android.content.res.ColorStateList.valueOf(text)
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private class EmojiAdapter(
        private var items: List<String>,
        private val dark: Boolean,
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<EmojiAdapter.VH>() {

        class VH(val text: TextView) : RecyclerView.ViewHolder(text)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val tv = TextView(parent.context).apply {
                textSize = 24f
                gravity = Gravity.CENTER
                setPadding(0, dp(parent.context, 8), 0, dp(parent.context, 8))
                includeFontPadding = false
                typeface = Typeface.DEFAULT
                paintFlags = paintFlags or Paint.SUBPIXEL_TEXT_FLAG
                val color = if (dark) Color.WHITE else Color.parseColor("#212121")
                setTextColor(color)
            }
            return VH(tv)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val emoji = items[position]
            holder.text.text = emoji
            holder.text.setOnClickListener { onClick(emoji) }
        }

        override fun getItemCount(): Int = items.size

        fun update(newItems: List<String>) {
            items = newItems
            notifyDataSetChanged()
        }

        private fun dp(ctx: Context, value: Int): Int =
            (value * ctx.resources.displayMetrics.density).toInt()
    }
}
