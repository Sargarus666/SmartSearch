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
import com.google.android.material.color.DynamicColors

class DetailActivity : AppCompatActivity() {
    companion object {
        fun intent(ctx: Context, id: Int) = Intent(ctx, DetailActivity::class.java).putExtra("id", id)
    }

    override fun onCreate(b: Bundle?) {
        when (Prefs.theme(this)) {
            "light" -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
            else -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(b)
        applyBars(this)
        val ctx = this
        val dp = resources.displayMetrics.density
        val id = intent.getIntExtra("id", 1)
        val all = (RemoteCatalog.live ?: emptyList()) + Catalog.phones
        val seen = HashSet<Int>()
        val ph = all.firstOrNull { it.id == id && seen.add(it.id) } ?: Catalog.phones[0]
        val ac = accent(ctx)

        val scroll = ScrollView(ctx).apply { setBackgroundColor(bgColor(ctx)) }
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((20 * dp).toInt(), (16 * dp).toInt(), (20 * dp).toInt(), (28 * dp).toInt())
        }

        // большое фото
        val photo = phonePhoto(ctx, ph, 320, 240).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (240 * dp).toInt())
        }
        col.addView(photo)

        col.addView(TextView(ctx).apply {
            text = "${ph.brand} ${ph.model}"
            textSize = 22f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(fgColor(ctx))
            setPadding(0, (14 * dp).toInt(), 0, 0)
        })
        col.addView(TextView(ctx).apply {
            text = "RAM ${ph.ramGb} ГБ физическая • ${ph.storageGb} ГБ • ${ph.cpu}"
            textSize = 13.5f
            setTextColor(surface(ctx, com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF44474F.toInt()))
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

        col.addView(Button(ctx).apply {
            text = "Назад"; setOnClickListener { finish() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (16 * dp).toInt()
            }
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
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(surface(context, com.google.android.material.R.attr.colorSurfaceVariant, 0xFFF3F4F9.toInt()))
                cornerRadius = 20 * dp
            }
            setPadding((16 * dp).toInt(), (12 * dp).toInt(), (16 * dp).toInt(), (12 * dp).toInt())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (8 * dp).toInt()
            }
        }
        c.addView(TextView(this).apply {
            text = "${o.shop} — ${fmtPrice(o.price)}"
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = 15f
        })
        c.addView(TextView(this).apply { text = o.address; textSize = 12.5f })
        c.addView(TextView(this).apply { text = o.note; textSize = 12.5f })
        c.addView(Button(this, null, android.R.attr.buttonStyleSmall).apply {
            text = "Открыть сайт"
            setOnClickListener {
                try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(o.url))) }
                catch (_: Exception) { Toast.makeText(context, o.url, Toast.LENGTH_LONG).show() }
            }
        })
        return c
    }
}
