package ru.chelny.smartscan

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale

val RU_FMT: NumberFormat = NumberFormat.getInstance(Locale("ru", "RU"))
fun fmtPrice(p: Int): String = RU_FMT.format(p) + " ₽"

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
    ctx.startActivity(Intent.createChooser(i, "Поделиться смартфоном"))
}

/** Рисованное «фото»-превью телефона: градиент + корпус + камеры + бренд. */
fun makePhonePreview(ctx: Context, ph: Phone): FrameLayout {
    val dp = ctx.resources.displayMetrics.density
    val root = FrameLayout(ctx).apply {
        layoutParams = ViewGroup.LayoutParams((110 * dp).toInt(), (150 * dp).toInt())
        background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(ph.colorStart, ph.colorEnd)
        ).apply { cornerRadius = 14 * dp }
        clipToOutline = false
    }
    // корпус
    val body = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        val w = (64 * dp).toInt(); val h = (120 * dp).toInt()
        layoutParams = FrameLayout.LayoutParams(w, h, Gravity.CENTER).apply {
            topMargin = (8 * dp).toInt()
        }
        background = GradientDrawable().apply {
            setColor(0xFF111111.toInt()); cornerRadius = 12 * dp
            setStroke((2 * dp).toInt(), 0x55FFFFFF)
        }
        gravity = Gravity.CENTER_HORIZONTAL
    }
    // блок камер
    val cams = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams((44 * dp).toInt(), (44 * dp).toInt()).apply {
            topMargin = (8 * dp).toInt()
        }
        background = GradientDrawable().apply {
            setColor(0xFF222831.toInt()); cornerRadius = 8 * dp
        }
        gravity = Gravity.CENTER
    }
    repeat(2) {
        val dot = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams((14 * dp).toInt(), (14 * dp).toInt()).apply {
                topMargin = (3 * dp).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF4FC3F7.toInt()); setStroke((2 * dp).toInt(), 0xFF0D47A1.toInt())
            }
        }
        cams.addView(dot)
    }
    body.addView(cams)
    val brand = TextView(ctx).apply {
        text = ph.brand.uppercase(); setTextColor(0xFFFFFFFF.toInt())
        textSize = 9f; typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = (8 * dp).toInt() }
    }
    val mod = TextView(ctx).apply {
        text = ph.model.substringAfter(" ").take(10); setTextColor(0xCCFFFFFF.toInt())
        textSize = 7f; gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
    body.addView(brand); body.addView(mod)
    root.addView(body)
    // шильдик RAM
    val ram = TextView(ctx).apply {
        text = "${ph.ramGb} ГБ"; setTextColor(0xFF000000.toInt()); textSize = 10f
        typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.END
        ).apply { setMargins(0, (6 * dp).toInt(), (6 * dp).toInt(), 0) }
        background = GradientDrawable().apply {
            setColor(0xFFFFEB3B.toInt()); cornerRadius = 8 * dp
        }
        setPadding((8 * dp).toInt(), (3 * dp).toInt(), (8 * dp).toInt(), (3 * dp).toInt())
    }
    root.addView(ram)
    return root
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
            ).apply { setMargins((12 * dp).toInt(), (6 * dp).toInt(), (12 * dp).toInt(), (6 * dp).toInt()) }
            background = GradientDrawable().apply {
                setColor(if (Prefs.style(ctx) == "mono") 0xFF111111.toInt() else 0xFFFFFFFF.toInt())
                cornerRadius = 16 * dp
                setStroke((1 * dp).toInt(), 0x22000000)
            }
            setPadding((12 * dp).toInt(), (12 * dp).toInt(), (12 * dp).toInt(), (12 * dp).toInt())
            elevation = 4 * dp
        }
        return VH(card)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val ctx = h.root.context
        val dp = ctx.resources.displayMetrics.density
        val ph = items[pos]
        h.root.removeAllViews()
        h.root.addView(makePhonePreview(ctx, ph))

        val dark = Prefs.style(ctx) == "mono"
        val fg = if (dark) 0xFFFFFFFF.toInt() else 0xFF111111.toInt()
        val sub = if (dark) 0xFFBBBBBB.toInt() else 0xFF666666.toInt()
        val accent = Prefs.ACCENTS[Prefs.accent(ctx) % Prefs.ACCENTS.size]

        val info = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = (12 * dp).toInt()
            }
        }
        val topRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(ctx).apply {
            text = "${ph.brand} ${ph.model}"; setTextColor(fg)
            textSize = 15f; typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        topRow.addView(title)
        val favBtn = ImageButton(ctx).apply {
            layoutParams = LinearLayout.LayoutParams((36 * dp).toInt(), (36 * dp).toInt())
            background = null
            contentDescription = "В избранное"
            setImageResource(if (Prefs.isFav(ctx, ph.id)) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off)
            setColorFilter(accent)
            setOnClickListener { v ->
                val now = Prefs.toggleFav(ctx, ph.id)
                (v as ImageButton).setImageResource(if (now) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off)
                Toast.makeText(ctx, if (now) "В избранном ★" else "Убрано из избранного", Toast.LENGTH_SHORT).show()
                onFavChanged?.invoke()
            }
        }
        val shareBtn = ImageButton(ctx).apply {
            layoutParams = LinearLayout.LayoutParams((36 * dp).toInt(), (36 * dp).toInt())
            background = null
            contentDescription = "Поделиться"
            setImageResource(android.R.drawable.ic_menu_share)
            setColorFilter(accent)
            setOnClickListener { sharePhone(ctx, ph) }
        }
        topRow.addView(favBtn); topRow.addView(shareBtn)
        info.addView(topRow)

        info.addView(TextView(ctx).apply {
            text = "RAM ${ph.ramGb} ГБ физическая • ${ph.storageGb} ГБ • ${ph.screen.take(24)}…"
            setTextColor(sub); textSize = 12f
        })
        val priceRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        priceRow.addView(TextView(ctx).apply {
            text = fmtPrice(ph.price); setTextColor(accent); textSize = 18f; typeface = Typeface.DEFAULT_BOLD
        })
        priceRow.addView(TextView(ctx).apply {
            text = "  ${fmtPrice(ph.oldPrice)}"; setTextColor(0xFF999999.toInt()); textSize = 12f
            paintFlags = paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginStart = (6 * dp).toInt()
            }
        })
        priceRow.addView(TextView(ctx).apply {
            text = "−${ph.discountPct}%"; setTextColor(0xFFFFFFFF.toInt()); textSize = 12f
            typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginStart = (8 * dp).toInt()
            }
            background = GradientDrawable().apply { setColor(0xFFE11D2E.toInt()); cornerRadius = 8 * dp }
            setPadding((8 * dp).toInt(), (2 * dp).toInt(), (8 * dp).toInt(), (2 * dp).toInt())
        })
        info.addView(priceRow)
        info.addView(TextView(ctx).apply {
            text = "⚡ ${ph.promo}"; setTextColor(sub); textSize = 12f
        })
        info.addView(TextView(ctx).apply {
            val minOff = ph.offline.minOf { it.price }; val minOn = ph.online.minOf { it.price }
            text = "★ ${ph.rating}   🏬 от ${fmtPrice(minOff)}   🛒 от ${fmtPrice(minOn)}"
            setTextColor(sub); textSize = 11f
        })
        val openBtn = Button(ctx).apply {
            text = "МАГАЗИНЫ • ОНЛАЙН / ОФЛАЙН"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (8 * dp).toInt()
            }
            setOnClickListener { onOpen(ph) }
        }
        info.addView(openBtn)
        h.root.addView(info)
        h.root.setOnClickListener { onOpen(ph) }
    }
}
