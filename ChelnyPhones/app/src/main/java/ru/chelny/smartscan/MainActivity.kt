package ru.chelny.smartscan

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.color.DynamicColors

class MainActivity : AppCompatActivity() {
    private var tab = 0
    private val tabIcons = intArrayOf(
        R.drawable.ic_tab_list, R.drawable.ic_tab_fav,
        R.drawable.ic_tab_news, R.drawable.ic_tab_set)
    private val tabNames = arrayOf("Список", "Избранное", "Новости", "Настройки")
    private var tabViews = ArrayList<LinearLayout>()

    override fun onCreate(b: Bundle?) {
        when (Prefs.theme(this)) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(b)
        if (b != null) tab = b.getInt("tab", 0)
        applyBars(this)
        window.setBackgroundDrawableResource(android.R.color.transparent)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(bgColor(this@MainActivity))
        }
        val container = FrameLayout(this).apply {
            id = 1001
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        root.addView(container)
        root.addView(buildNav())
        setContentView(root)
        refreshNav()
        showTab()
    }

    /** Кастомный таб-бар как в MMO Calendar: пилюля + подпись всегда видна. */
    private fun buildNav(): LinearLayout {
        val ctx = this
        tabViews.clear()
        val bar = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(cardColor(ctx))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            elevation = ctx.dp(8).toFloat()
        }
        bar.addView(View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(1))
            setBackgroundColor(if (isDark(ctx)) 0x22FFFFFF else 0x14000000)
        })
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, ctx.dp(8), 0, ctx.dp(10))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        tabNames.forEachIndexed { i, name ->
            val cell = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                isClickable = true; isFocusable = true
                setOnClickListener { tab = i; refreshNav(); showTab() }
            }
            val pill = LinearLayout(ctx).apply {
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(ctx.dp(56), ctx.dp(32))
            }
            val icon = ImageView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(ctx.dp(24), ctx.dp(24))
                setImageResource(tabIcons[i])
            }
            pill.addView(icon)
            val label = TextView(ctx).apply {
                text = name; textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setPadding(0, ctx.dp(3), 0, 0)
            }
            cell.addView(pill); cell.addView(label)
            cell.tag = Pair(pill, label to icon)
            tabViews.add(cell)
            row.addView(cell)
        }
        bar.addView(row)
        return bar
    }

    private fun refreshNav() {
        val ac = accent2(this)
        tabViews.forEachIndexed { i, cell ->
            @Suppress("UNCHECKED_CAST")
            val tagPair = cell.tag as Pair<LinearLayout, Pair<TextView, ImageView>>
            val pill = tagPair.first
            val label = tagPair.second.first
            val icon = tagPair.second.second
            val sel = i == tab
            pill.background = GradientDrawable().apply {
                setColor(if (sel) ac else 0x00000000)
                cornerRadius = dp(99).toFloat() * 100
            }
            icon.imageTintList = ColorStateList.valueOf(
                if (sel) 0xFFFFFFFF.toInt() else subColor(this))
            label.setTextColor(if (sel) fgColor(this) else subColor(this))
        }
    }

    private fun showTab() {
        val f: Fragment = when (tab) {
            1 -> FavFragment()
            2 -> NewsFragment()
            3 -> SettingsFragment()
            else -> ListFragment()
        }
        supportFragmentManager.beginTransaction().replace(1001, f).commit()
    }

    override fun onSaveInstanceState(o: Bundle) {
        super.onSaveInstanceState(o); o.putInt("tab", tab)
    }
}
