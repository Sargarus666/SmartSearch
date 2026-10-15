package ru.chelny.smartscan

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

/** Кастомный UI-кит в духе MMO Calendar: всё рисуется кодом, детерминировано. */

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

fun isDark(ctx: Context): Boolean = when (Prefs.theme(ctx)) {
    "light" -> false
    "dark" -> true
    else -> (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES
}

fun bgColor(ctx: Context): Int =
    if (isDark(ctx)) 0xFF0E0B1A.toInt() else 0xFFFFFBFE.toInt()

fun cardColor(ctx: Context): Int =
    if (isDark(ctx)) 0xFF1B1626.toInt() else 0xFFFFFFFF.toInt()

fun fgColor(ctx: Context): Int =
    if (isDark(ctx)) 0xFFF2EDFF.toInt() else 0xFF1A1C1E.toInt()

fun subColor(ctx: Context): Int =
    if (isDark(ctx)) 0xFFB9B0CC.toInt() else 0xFF5A5B6B.toInt()

fun monoMode(ctx: Context): Boolean = Prefs.style(ctx) == "mono"

fun accent2(ctx: Context): Int {
    if (monoMode(ctx)) return if (isDark(ctx)) 0xFFD8D8E0.toInt() else 0xFF333338.toInt()
    if (Prefs.style(ctx) == "classic") return Prefs.ACCENTS[Prefs.accent(ctx) % Prefs.ACCENTS.size]
    // Material You: системный динамический цвет, иначе выбранный
    if (Build.VERSION.SDK_INT >= 31) {
        val tv = android.util.TypedValue()
        if (ctx.theme.resolveAttribute(
                com.google.android.material.R.attr.colorPrimary, tv, true) && tv.data != 0) return tv.data
    }
    return Prefs.ACCENTS[Prefs.accent(ctx) % Prefs.ACCENTS.size]
}

fun pillBg(color: Int, radiusDp: Float = 99f, stroke: Int = 0, strokeColor: Int = 0): GradientDrawable =
    GradientDrawable().apply {
        setColor(color); cornerRadius = radiusDp * 100 // большое значение = пилюля
        if (stroke > 0) setStroke(stroke, strokeColor)
    }

/** Главная кнопка-пилюля: акцентный фон, белый текст. */
fun mButton(ctx: Context, text: String, onClick: () -> Unit): Button =
    Button(ctx, null, android.R.attr.borderlessButtonStyle).apply {
        this.text = text
        isAllCaps = false
        textSize = 15f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setTextColor(0xFFFFFFFF.toInt())
        val ac = accent2(ctx)
        background = pillBg(ac)
        setPadding(ctx.dp(20), ctx.dp(13), ctx.dp(20), ctx.dp(13))
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        stateListAnimator = null
        setOnClickListener { onClick() }
    }

/** Тональная кнопка-пилюля. */
fun tonalButton(ctx: Context, text: String, onClick: () -> Unit): Button =
    Button(ctx, null, android.R.attr.borderlessButtonStyle).apply {
        this.text = text
        isAllCaps = false
        textSize = 14f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val ac = accent2(ctx)
        setTextColor(ac)
        background = pillBg(
            if (isDark(ctx)) blend(ac, 0xFF0E0B1A.toInt(), 0.22f)
            else blend(ac, 0xFFFFFFFF.toInt(), 0.16f))
        setPadding(ctx.dp(18), ctx.dp(11), ctx.dp(18), ctx.dp(11))
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        stateListAnimator = null
        setOnClickListener { onClick() }
    }

fun blend(a: Int, b: Int, t: Float): Int {
    fun c(v: Int, s: Int) = (v shr s) and 0xFF
    val r = (c(a, 16) * t + c(b, 16) * (1 - t)).toInt()
    val g = (c(a, 8) * t + c(b, 8) * (1 - t)).toInt()
    val bl = (c(a, 0) * t + c(b, 0) * (1 - t)).toInt()
    return (0xFF shl 24) or (r shl 16) or (g shl 8) or bl
}

/** Фильтр-чип пилюлей: выбран = акцент, нет = поверхность. */
fun chipView(ctx: Context, text: String, selected: Boolean, onClick: () -> Unit): TextView {
    val ac = accent2(ctx)
    return TextView(ctx).apply {
        this.text = text
        textSize = 13f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        gravity = Gravity.CENTER
        setTextColor(if (selected) 0xFFFFFFFF.toInt() else subColor(ctx))
        background = pillBg(if (selected) ac else cardColor(ctx),
            stroke = if (selected) 0 else ctx.dp(1),
            strokeColor = if (isDark(ctx)) 0x33FFFFFF else 0x22000000)
        setPadding(ctx.dp(16), ctx.dp(9), ctx.dp(16), ctx.dp(9))
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            marginEnd = ctx.dp(8)
        }
        setOnClickListener { onClick() }
    }
}

/** Круглое поле поиска без подчёркивания. */
fun searchField(ctx: Context, hint: String, onText: (String) -> Unit): EditText =
    EditText(ctx).apply {
        this.hint = hint
        textSize = 14f
        setTextColor(fgColor(ctx)); setHintTextColor(subColor(ctx))
        background = pillBg(cardColor(ctx), stroke = ctx.dp(1),
            strokeColor = if (isDark(ctx)) 0x33FFFFFF else 0x22000000)
        setPadding(ctx.dp(20), ctx.dp(13), ctx.dp(20), ctx.dp(13))
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) { onText(s.toString()) }
            override fun beforeTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
            override fun onTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
        })
    }

/** Статус-бар и навигация в цвет фона (как в MMO). */
fun applyBars(act: Activity) {
    val bg = bgColor(act)
    act.window.statusBarColor = bg
    act.window.navigationBarColor = bg
    if (Build.VERSION.SDK_INT >= 30) {
        act.window.insetsController?.setSystemBarsAppearance(
            if (isDark(act)) 0 else
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
            android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
    } else {
        @Suppress("DEPRECATION")
        act.window.decorView.systemUiVisibility =
            if (isDark(act)) 0 else
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
    }
}

fun sectionLabel(ctx: Context, t: String): TextView = TextView(ctx).apply {
    text = t; textSize = 13f
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    setTextColor(accent2(ctx))
    setPadding(0, ctx.dp(18), 0, ctx.dp(6))
    letterSpacing = 0.06f
}

fun iconBtn2(ctx: Context, res: Int, desc: String, act: () -> Unit): ImageButton =
    ImageButton(ctx).apply {
        layoutParams = LinearLayout.LayoutParams(ctx.dp(40), ctx.dp(40))
        setImageResource(res); contentDescription = desc
        background = null
        imageTintList = ColorStateList.valueOf(accent2(ctx))
        setOnClickListener { act() }
    }
