package ru.chelny.smartscan

import android.content.Context

object Prefs {
    private const val NAME = "smartscan_prefs"
    private const val KEY_FAV = "fav_ids"
    private const val KEY_THEME = "theme"       // system / light / dark
    private const val KEY_STYLE = "style"       // material / classic / mono
    private const val KEY_ACCENT = "accent"     // 0..6
    private const val KEY_CITY = "city"

    fun favs(ctx: Context): MutableSet<String> =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_FAV, emptySet())?.toMutableSet() ?: mutableSetOf()

    fun isFav(ctx: Context, id: Int): Boolean = favs(ctx).contains(id.toString())

    fun toggleFav(ctx: Context, id: Int): Boolean {
        val sp = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        val set = favs(ctx)
        val now: Boolean
        if (set.contains(id.toString())) { set.remove(id.toString()); now = false }
        else { set.add(id.toString()); now = true }
        sp.edit().putStringSet(KEY_FAV, set).apply()
        return now
    }

    fun theme(ctx: Context): String =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString(KEY_THEME, "system") ?: "system"
    fun setTheme(ctx: Context, v: String) =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putString(KEY_THEME, v).apply()

    fun style(ctx: Context): String =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString(KEY_STYLE, "material") ?: "material"
    fun setStyle(ctx: Context, v: String) =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putString(KEY_STYLE, v).apply()

    fun accent(ctx: Context): Int =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getInt(KEY_ACCENT, 0)
    fun setAccent(ctx: Context, v: Int) =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putInt(KEY_ACCENT, v).apply()

    fun city(ctx: Context): String =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString(KEY_CITY, "Набережные Челны") ?: "Набережные Челны"

    // 7 акцентных цветов как в MMO Calendar
    val ACCENTS = intArrayOf(
        0xFFE11D2E.toInt(), // красный фирменный
        0xFF6C4DFF.toInt(), // фиолет
        0xFF009688.toInt(), // teal
        0xFF1E88E5.toInt(), // синий
        0xFF43A047.toInt(), // зелёный
        0xFFFF8F00.toInt(), // оранжевый
        0xFFEC407A.toInt()  // розовый
    )
    val ACCENT_NAMES = arrayOf("Красный", "Фиолет", "Бирюза", "Синий", "Зелёный", "Оранжевый", "Розовый")
}
