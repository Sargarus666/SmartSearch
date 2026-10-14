package ru.chelny.smartscan

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class DetailActivity : AppCompatActivity() {
    companion object {
        fun intent(ctx: Context, id: Int) = Intent(ctx, DetailActivity::class.java).putExtra("id", id)
    }

    override fun onCreate(b: Bundle?) {
        applyThemeMode()
        super.onCreate(b)
        val ctx = this
        val dp = resources.displayMetrics.density
        val id = intent.getIntExtra("id", 1)
        val all = (RemoteCatalog.live ?: emptyList()) + Catalog.phones
        val ph = all.firstOrNull { it.id == id } ?: Catalog.phones[0]
        val accent = Prefs.ACCENTS[Prefs.accent(ctx) % Prefs.ACCENTS.size]

        val scroll = ScrollView(ctx)
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16 * dp).toInt(), (16 * dp).toInt(), (16 * dp).toInt(), (24 * dp).toInt())
        }

        // топ-превью: фото + кнопки избранное/поделиться
        val top = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(makePhonePreview(ctx, ph))
        val tt = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = (14 * dp).toInt()
            }
        }
        tt.addView(TextView(ctx).apply { text = "${ph.brand} ${ph.model}"; textSize = 20f; typeface = Typeface.DEFAULT_BOLD })
        tt.addView(TextView(ctx).apply { text = "RAM ${ph.ramGb} ГБ физическая • ${ph.storageGb} ГБ"; textSize = 13f })
        tt.addView(TextView(ctx).apply {
            text = "${fmtPrice(ph.price)}  −${ph.discountPct}%"; textSize = 22f
            typeface = Typeface.DEFAULT_BOLD; setTextColor(accent)
        })
        tt.addView(TextView(ctx).apply { text = "Было ${fmtPrice(ph.oldPrice)}"; textSize = 13f })
        tt.addView(TextView(ctx).apply { text = "⚡ ${ph.promo}"; textSize = 13f })
        top.addView(tt)
        col.addView(top)

        val btnRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val favB = Button(ctx, null, android.R.attr.buttonStyleSmall).apply {
            text = if (Prefs.isFav(ctx, ph.id)) "★ В ИЗБРАННОМ" else "☆ В ИЗБРАННОЕ"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                val now = Prefs.toggleFav(ctx, ph.id)
                text = if (now) "★ В ИЗБРАННОМ" else "☆ В ИЗБРАННОЕ"
            }
        }
        val shB = Button(ctx, null, android.R.attr.buttonStyleSmall).apply {
            text = "⤴ Поделиться"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { sharePhone(ctx, ph) }
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
        ph.offline.forEach { col.addView(storeCard(it, accent)) }
        col.addView(h("🛒 Онлайн • доставка в Челны"))
        ph.online.forEach { col.addView(storeCard(it, accent)) }

        val back = Button(ctx).apply { text = "← НАЗАД К СПИСКУ"; setOnClickListener { finish() } }
        col.addView(back)
        scroll.addView(col)
        setContentView(scroll)
    }

    private fun h(t: String) = TextView(this).apply {
        text = t; textSize = 16f; typeface = Typeface.DEFAULT_BOLD; setPadding(0, 20, 0, 8)
    }
    private fun spec(k: String, v: String) = TextView(this).apply {
        text = "• $k: $v"; textSize = 14f; setPadding(0, 2, 0, 2)
    }
    private fun storeCard(o: StoreOffer, accent: Int): LinearLayout {
        val dp = resources.displayMetrics.density
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply { setColor(0xFFF5F5F5.toInt()); cornerRadius = 12 * dp; setStroke(1, accent) }
            setPadding((12 * dp).toInt(), (10 * dp).toInt(), (12 * dp).toInt(), (10 * dp).toInt())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (8 * dp).toInt()
            }
        }
        c.addView(TextView(this).apply { text = "${o.shop} — ${fmtPrice(o.price)}"; typeface = Typeface.DEFAULT_BOLD; textSize = 15f })
        c.addView(TextView(this).apply { text = o.address; textSize = 12f })
        c.addView(TextView(this).apply { text = o.note; textSize = 12f })
        c.addView(Button(this, null, android.R.attr.buttonStyleSmall).apply {
            text = "ОТКРЫТЬ САЙТ"
            setOnClickListener {
                try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(o.url))) }
                catch (_: Exception) { Toast.makeText(context, o.url, Toast.LENGTH_LONG).show() }
            }
        })
        return c
    }

    private fun applyThemeMode() {
        when (Prefs.theme(this)) {
            "light" -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
            else -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}
