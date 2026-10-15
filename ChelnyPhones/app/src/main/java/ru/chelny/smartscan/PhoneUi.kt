package ru.chelny.smartscan

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale

val RU_FMT: NumberFormat = NumberFormat.getInstance(Locale("ru", "RU"))
fun fmtPrice(p: Int): String = RU_FMT.format(p) + " ₽"

/** Акцент: dynamic Material You primary, иначе выбранный в настройках. */
fun accent(ctx: Context): Int {
    val tv = TypedValue()
    return if (ctx.theme.resolveAttribute(
            com.google.android.material.R.attr.colorPrimary, tv, true)) tv.data
    else Prefs.ACCENTS[Prefs.accent(ctx) % Prefs.ACCENTS.size]
}

fun surface(ctx: Context, attr: Int, fallback: Int): Int {
    val tv = TypedValue()
    return if (ctx.theme.resolveAttribute(attr, tv, true)) tv.data else fallback
}

fun sharePhone(ctx: Context, ph: Phone) {
    val minOff = ph.offline.minOfOrNull { it.price } ?: ph.price
    val minOn = ph.online.minOfOrNull { it.price } ?: ph.price
    val text = "📱 ${ph.brand} ${ph.model}\n" +
        "RAM ${ph.ramGb} ГБ (физическая) • ${ph.storageGb} ГБ\n" +
        "Цена: ${fmtPrice(ph.price)} (−${ph.discountPct}%), было ${fmtPrice(ph.oldPrice)}\n" +
        "Акция: ${ph.promo}\n" +
        "Офлайн в Челнах от ${fmtPrice(minOff)}, онлайн от ${fmtPrice(minOn)}\n" +
        "Сканер: Челны СмартСкан • Набережные Челны"
    val i = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
    }
    try {
        ctx.startActivity(Intent.createChooser(i, "Поделиться смартфоном"))
    } catch (_: Exception) {
        try {
            android.content.ClipboardManager::class.java
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            cm.setPrimaryClip(android.content.ClipData.newPlainText("phone", text))
            Toast.makeText(ctx, "Скопировано в буфер", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {}
    }
}

fun chip(ctx: Context, text: String, bg: Int, fg: Int): TextView {
    val dp = ctx.resources.displayMetrics.density
    return TextView(ctx).apply {
        this.text = text; setTextColor(fg); textSize = 11.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        gravity = Gravity.CENTER
        background = GradientDrawable().apply { setColor(bg); cornerRadius = 20 * dp }
        setPadding((10 * dp).toInt(), (5 * dp).toInt(), (10 * dp).toInt(), (5 * dp).toInt())
    }
}

class PhoneAdapter(
    private var items: List<Phone>,
    private val onOpen: (Phone) -> Unit,
    private val onFavChanged: (() -> Unit)? = null
) : RecyclerView.Adapter<PhoneAdapter.VH>() {

    inner class VH(val root: LinearLayout) : RecyclerView.ViewHolder(root)

    fun submit(list: List<Phone>) { items = list; notifyDataSetChanged() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val dp = ctx.resources.displayMetrics.density
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins((16 * dp).toInt(), (7 * dp).toInt(), (16 * dp).toInt(), (7 * dp).toInt()) }
            background = GradientDrawable().apply {
                setColor(cardColor(ctx))
                cornerRadius = 24 * dp
            }
            setPadding((14 * dp).toInt(), (14 * dp).toInt(), (14 * dp).toInt(), (14 * dp).toInt())
            elevation = 2 * dp
            isClickable = true; isFocusable = true
        }
        return VH(card)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        try {
            bindItem(h, pos)
        } catch (_: Exception) {
            try {
                h.root.removeAllViews()
                h.root.addView(TextView(h.root.context).apply {
                    text = "Не удалось показать карточку"
                    textSize = 14f
                    setPadding(28, 28, 28, 28)
                })
            } catch (_: Exception) {}
        }
    }

    private fun bindItem(h: VH, pos: Int) {
        val ctx = h.root.context
        val dp = ctx.resources.displayMetrics.density
        val ph = items[pos]
        val ac = accent2(ctx)
        val onSurf = fgColor(ctx)
        val onVar = subColor(ctx)
        h.root.removeAllViews()
        h.root.addView(phonePhoto(ctx, ph, 104, 128))

        val info = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = (14 * dp).toInt()
            }
        }
        // заголовок + ★ ⤴
        val top = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(TextView(ctx).apply {
            text = "${ph.brand} ${ph.model}"; setTextColor(onSurf)
            textSize = 16f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        lateinit var favB: ImageButton
        favB = iconBtn2(ctx,
            if (Prefs.isFav(ctx, ph.id)) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off,
            "В избранное") {
            val now = Prefs.toggleFav(ctx, ph.id)
            favB.setImageResource(
                if (now) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off)
            Toast.makeText(ctx, if (now) "В избранном" else "Убрано из избранного", Toast.LENGTH_SHORT).show()
            onFavChanged?.invoke()
        }
        top.addView(favB)
        top.addView(iconBtn2(ctx, android.R.drawable.ic_menu_share, "Поделиться") { sharePhone(ctx, ph) })
        info.addView(top)

        info.addView(TextView(ctx).apply {
            text = "${ph.ramGb} ГБ • ${ph.storageGb} ГБ • ${ph.cpu}"
            setTextColor(onVar); textSize = 12.5f
            setPadding(0, (2 * dp).toInt(), 0, 0)
        })
        // цена + чипы
        val priceRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (6 * dp).toInt(), 0, 0)
        }
        priceRow.addView(TextView(ctx).apply {
            text = fmtPrice(ph.price); setTextColor(onSurf); textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        })
        priceRow.addView(TextView(ctx).apply {
            text = fmtPrice(ph.oldPrice); setTextColor(0xFF9AA0A6.toInt()); textSize = 12f
            paintFlags = paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginStart = (8 * dp).toInt()
            }
        })
        val chipWrap = LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginStart = (8 * dp).toInt()
            }
        }
        chipWrap.addView(chip(ctx, "−${ph.discountPct}%", 0xFFE11D2E.toInt(), 0xFFFFFFFF.toInt()))
        priceRow.addView(chipWrap)
        info.addView(priceRow)

        info.addView(TextView(ctx).apply {
            text = ph.promo; setTextColor(onVar); textSize = 12f
            setPadding(0, (4 * dp).toInt(), 0, 0)
        })
        info.addView(TextView(ctx).apply {
            val minOff = ph.offline.minOfOrNull { it.price } ?: ph.price
            val minOn = ph.online.minOfOrNull { it.price } ?: ph.price
            text = "★ ${ph.rating} · 🏬 ${fmtPrice(minOff)} · 🛒 ${fmtPrice(minOn)}"
            setTextColor(onVar); textSize = 12f
            setPadding(0, (2 * dp).toInt(), 0, 0)
        })
        h.root.addView(info)
        h.root.setOnClickListener { onOpen(ph) }
    }
}
