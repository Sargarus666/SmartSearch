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
import coil.load

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
    return try {
        // Монохром: чёрная пилюля + белый текст (и в тёмной, и в светлой теме)
        if (monoMode(ctx)) return 0xFF000000.toInt()
        if (Prefs.style(ctx) == "classic") return Prefs.ACCENTS[Prefs.accent(ctx) % Prefs.ACCENTS.size]
        // Material You: системный динамический цвет, иначе выбранный
        if (Build.VERSION.SDK_INT >= 31) {
            val tv = android.util.TypedValue()
            if (ctx.theme.resolveAttribute(
                    com.google.android.material.R.attr.colorPrimary, tv, true) && tv.data != 0) return tv.data
        }
        Prefs.ACCENTS[Prefs.accent(ctx) % Prefs.ACCENTS.size]
    } catch (_: Exception) {
        Prefs.ACCENTS[0]
    }
}

fun pillBg(color: Int, radiusDp: Float = 99f, stroke: Int = 0, strokeColor: Int = 0): GradientDrawable =
    GradientDrawable().apply {
        setColor(color); cornerRadius = radiusDp * 100 // большое значение = пилюля
        if (stroke > 0) setStroke(stroke, strokeColor)
    }

/** Главная кнопка-пилюля: акцентный фон, белый текст. */
fun mButton(ctx: Context, text: String, onClick: () -> Unit): Button =
    androidx.appcompat.widget.AppCompatButton(ctx).apply {
        this.text = text
        isAllCaps = false
        textSize = 14f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setTextColor(0xFFFFFFFF.toInt())
        val ac = accent2(ctx)
        background = pillBg(ac)
        setPadding(ctx.dp(20), ctx.dp(9), ctx.dp(20), ctx.dp(9))
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        try { stateListAnimator = null } catch (_: Exception) {}
        setOnClickListener { try { onClick() } catch (_: Exception) {} }
    }

/** Тональная кнопка-пилюля. */
fun tonalButton(ctx: Context, text: String, onClick: () -> Unit): Button =
    androidx.appcompat.widget.AppCompatButton(ctx).apply {
        this.text = text
        isAllCaps = false
        textSize = 13f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val ac = accent2(ctx)
        setTextColor(ac)
        background = pillBg(
            if (isDark(ctx)) blend(ac, 0xFF0E0B1A.toInt(), 0.22f)
            else blend(ac, 0xFFFFFFFF.toInt(), 0.16f))
        setPadding(ctx.dp(18), ctx.dp(8), ctx.dp(18), ctx.dp(8))
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        try { stateListAnimator = null } catch (_: Exception) {}
        setOnClickListener { try { onClick() } catch (_: Exception) {} }
    }

/** Круглая иконка магазина: фирменная буква на фирменном фоне. */
fun storeBadge(ctx: Context, shop: String): TextView {
    data class B(val bg: Int, val fg: Int, val letter: String)
    val b = when {
        shop.contains("DNS", true) -> B(0xFFFFD500.toInt(), 0xFF000000.toInt(), "D")
        shop.contains("М.Видео", true) -> B(0xFFE11D2E.toInt(), 0xFFFFFFFF.toInt(), "М")
        shop.contains("Эльдорадо", true) -> B(0xFFFF6F00.toInt(), 0xFFFFFFFF.toInt(), "Э")
        shop.contains("OZON", true) -> B(0xFF005BFF.toInt(), 0xFFFFFFFF.toInt(), "O")
        shop.contains("Wildberries", true) || shop == "WB" -> B(0xFF7B2FF7.toInt(), 0xFFFFFFFF.toInt(), "W")
        shop.contains("Маркет", true) -> B(0xFFFFCC00.toInt(), 0xFF000000.toInt(), "Я")
        shop.contains("Ситилинк", true) -> B(0xFF0096DC.toInt(), 0xFFFFFFFF.toInt(), "С")
        shop.contains("МТС", true) -> B(0xFFE30613.toInt(), 0xFFFFFFFF.toInt(), "М")
        shop.contains("Билайн", true) -> B(0xFFFFD200.toInt(), 0xFF000000.toInt(), "Б")
        shop.contains("МегаФон", true) -> B(0xFF00B956.toInt(), 0xFFFFFFFF.toInt(), "М")
        else -> B(accent2(ctx), 0xFFFFFFFF.toInt(), shop.trim().take(1).uppercase())
    }
    return TextView(ctx).apply {
        text = b.letter; setTextColor(b.fg); textSize = 18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(ctx.dp(46), ctx.dp(46)).apply {
            marginEnd = ctx.dp(12)
        }
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL; setColor(b.bg)
        }
    }
}

fun blend(a: Int, b: Int, t: Float): Int {
    fun c(v: Int, s: Int) = (v shr s) and 0xFF
    val r = (c(a, 16) * t + c(b, 16) * (1 - t)).toInt()
    val g = (c(a, 8) * t + c(b, 8) * (1 - t)).toInt()
    val bl = (c(a, 0) * t + c(b, 0) * (1 - t)).toInt()
    return (0xFF shl 24) or (r shl 16) or (g shl 8) or bl
}

fun photoResId(ctx: Context, ph: Phone): Int {
    val id = ctx.resources.getIdentifier(ph.photoRes, "drawable", ctx.packageName)
    return if (id != 0) id else android.R.drawable.ic_menu_camera
}

/** Реальное фото: сначала сеть (Coil), без сети — встроенный официальный рендер. */
fun phonePhoto(ctx: Context, ph: Phone, wDp: Int, hDp: Int): ImageView {
    val dp = ctx.resources.displayMetrics.density
    return ImageView(ctx).apply {
        layoutParams = LinearLayout.LayoutParams((wDp * dp).toInt(), (hDp * dp).toInt())
        scaleType = ImageView.ScaleType.CENTER_CROP
        background = GradientDrawable().apply {
            setColor(0xFFE8EAF6.toInt()); cornerRadius = 20 * dp
        }
        try { clipToOutline = true } catch (_: Exception) {}
        try {
            load(ph.photoUrl) {
                crossfade(true)
                fallback(photoResId(ctx, ph))
                error(photoResId(ctx, ph))
                placeholder(photoResId(ctx, ph))
            }
        } catch (_: Exception) {
            try { setImageResource(photoResId(ctx, ph)) } catch (_: Exception) {}
        }
    }
}

/** Фильтр-чип пилюлей: выбран = акцент, нет = поверхность. */
fun chipView(ctx: Context, text: String, selected: Boolean, onClick: () -> Unit): TextView {    val ac = accent2(ctx)
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
    try {
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
    } catch (_: Exception) {}
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
        layoutParams = LinearLayout.LayoutParams(ctx.dp(34), ctx.dp(34))
        try { setImageResource(res) } catch (_: Exception) {}
        contentDescription = desc
        background = null
        try { imageTintList = ColorStateList.valueOf(accent2(ctx)) } catch (_: Exception) {}
        setOnClickListener { try { act() } catch (_: Exception) {} }
    }
