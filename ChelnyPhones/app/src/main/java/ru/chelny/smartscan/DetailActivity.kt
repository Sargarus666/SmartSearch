package ru.chelny.smartscan

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.google.android.material.color.DynamicColors

class DetailActivity : AppCompatActivity() {
    companion object {
        fun intent(ctx: Context, id: Int) = Intent(ctx, DetailActivity::class.java).putExtra("id", id)
    }

    override fun onCreate(b: Bundle?) {
        try {
            when (Prefs.theme(this)) {
                "light" -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
                "dark" -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
                else -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        } catch (_: Exception) {}
        super.onCreate(b)
        try { DynamicColors.applyToActivityIfAvailable(this) } catch (_: Exception) {}
        try {
            buildContent()
        } catch (e: Exception) {
            try {
                setContentView(TextView(this).apply {
                    text = "Не удалось открыть карточку.\n${e.message}"
                    textSize = 15f; setPadding(48, 48, 48, 48)
                })
            } catch (_: Exception) { finish() }
        }
    }

    private fun buildContent() {
        applyBars(this)
        val ctx = this
        val dp = resources.displayMetrics.density
        val id = intent.getIntExtra("id", 1)
        val all = (RemoteCatalog.live ?: emptyList()) + Catalog.phones
        val seen = HashSet<Int>()
        val ph = all.firstOrNull { it.id == id && seen.add(it.id) } ?: Catalog.phones[0]
        val ac = accent2(ctx)

        val scroll = ScrollView(ctx).apply { setBackgroundColor(bgColor(ctx)) }
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((20 * dp).toInt(), (16 * dp).toInt(), (20 * dp).toInt(), (28 * dp).toInt())
        }

        // галерея: 3 квадратных превью, тап — полный размер с листанием
        val galleryUrls = if (ph.gallery.isNotEmpty()) ph.gallery.take(3)
            else listOf(ph.photoUrl).filter { it.isNotBlank() }
        val galRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        galleryUrls.forEachIndexed { gi, url ->
            val th = ImageView(ctx).apply {
                val s = (104 * dp).toInt()
                layoutParams = LinearLayout.LayoutParams(s, s).apply {
                    if (gi > 0) marginStart = (10 * dp).toInt()
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = GradientDrawable().apply {
                    setColor(0xFFE8EAF6.toInt()); cornerRadius = 18 * dp
                }
                clipToOutline = true
                isClickable = true; isFocusable = true
                contentDescription = "Фото ${gi + 1}"
            }
            try {
                th.load(url) {
                    crossfade(true)
                    fallback(photoResId(ctx, ph))
                    error(photoResId(ctx, ph))
                    placeholder(photoResId(ctx, ph))
                }
            } catch (_: Exception) {
                try { th.setImageResource(photoResId(ctx, ph)) } catch (_: Exception) {}
            }
            th.setOnClickListener {
                try {
                    startActivity(GalleryActivity.intent(ctx, galleryUrls, gi, "${ph.brand} ${ph.model}"))
                } catch (_: Exception) {}
            }
            galRow.addView(th)
        }
        col.addView(galRow)

        col.addView(TextView(ctx).apply {
            text = "${ph.brand} ${ph.model}"
            textSize = 22f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(fgColor(ctx))
            setPadding(0, (14 * dp).toInt(), 0, 0)
        })
        col.addView(TextView(ctx).apply {
            text = "RAM ${ph.ramGb} ГБ физическая • ${ph.storageGb} ГБ • ${ph.cpu}"
            textSize = 13.5f
            setTextColor(subColor(ctx))
        })
        val priceRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (8 * dp).toInt(), 0, 0)
        }
        priceRow.addView(TextView(ctx).apply {
            text = fmtPrice(ph.price); textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); setTextColor(ac)
        })
        priceRow.addView(TextView(ctx).apply {
            text = fmtPrice(ph.oldPrice); textSize = 14f
            setTextColor(0xFF9AA0A6.toInt())
            paintFlags = paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginStart = (10 * dp).toInt()
            }
        })
        priceRow.addView(chip(ctx, "−${ph.discountPct}%", 0xFFE11D2E.toInt(), 0xFFFFFFFF.toInt()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginStart = (10 * dp).toInt()
            }
        })
        col.addView(priceRow)
        col.addView(TextView(ctx).apply { text = "⚡ ${ph.promo}"; textSize = 13.5f })

        // ★ ⤴ в карточке
        val btnRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, (12 * dp).toInt(), 0, 0)
        }
        lateinit var favB: Button
        favB = mButton(ctx, if (Prefs.isFav(ctx, ph.id)) "★ В избранном" else "☆ В избранное") {
            val now = Prefs.toggleFav(ctx, ph.id)
            favB.text = if (now) "★ В избранном" else "☆ В избранное"
        }.apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val shB = tonalButton(ctx, "⤴ Поделиться") { sharePhone(ctx, ph) }.apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = (8 * dp).toInt()
            }
        }
        btnRow.addView(favB); btnRow.addView(shB)
        col.addView(btnRow)

        col.addView(h("Характеристики"))
        col.addView(spec("Экран", ph.screen))
        col.addView(spec("Батарея", ph.battery))
        col.addView(spec("Камера", ph.camera))
        col.addView(spec("Процессор", ph.cpu))
        col.addView(spec("Корпус", ph.accent))

        col.addView(h("🏬 Офлайн • Набережные Челны"))
        ph.offline.forEach { col.addView(storeCard(it)) }
        col.addView(h("🛒 Онлайн • доставка в Челны"))
        ph.online.forEach { col.addView(storeCard(it)) }

        col.addView(tonalButton(ctx, "Назад к списку") { finish() }.also {
            (it.layoutParams as LinearLayout.LayoutParams).topMargin = (16 * dp).toInt()
        })
        scroll.addView(col)
        setContentView(scroll)
    }

    private fun h(t: String) = TextView(this).apply {
        text = t; textSize = 15f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); setPadding(0, 22, 0, 8)
    }
    private fun spec(k: String, v: String) = TextView(this).apply {
        text = "$k  ·  $v"; textSize = 14f; setPadding(0, 3, 0, 3)
    }
    private fun storeCard(o: StoreOffer): LinearLayout {
        val dp = resources.displayMetrics.density
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(cardColor(context))
                cornerRadius = 20 * dp
            }
            setPadding((14 * dp).toInt(), (12 * dp).toInt(), (14 * dp).toInt(), (12 * dp).toInt())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (8 * dp).toInt()
            }
            isClickable = true; isFocusable = true
        }
        c.addView(storeBadge(this, o.shop))
        val tx = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        tx.addView(TextView(this).apply {
            text = "${o.shop} — ${fmtPrice(o.price)}"
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = 15f
            setTextColor(fgColor(this@DetailActivity))
        })
        tx.addView(TextView(this).apply { text = o.address; textSize = 12.5f; setTextColor(subColor(this@DetailActivity)) })
        tx.addView(TextView(this).apply { text = o.note; textSize = 12.5f; setTextColor(subColor(this@DetailActivity)) })
        c.addView(tx)
        c.addView(TextView(this).apply {
            text = "›"; textSize = 26f; setTextColor(subColor(this@DetailActivity))
            setPadding((8 * dp).toInt(), 0, 0, 0)
        })
        c.setOnClickListener {
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(o.url))) }
            catch (_: Exception) { Toast.makeText(this@DetailActivity, o.url, Toast.LENGTH_LONG).show() }
        }
        return c
    }
}
