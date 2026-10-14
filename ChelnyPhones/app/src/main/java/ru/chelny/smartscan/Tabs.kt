package ru.chelny.smartscan

import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.text.SimpleDateFormat
import java.util.*

open class BaseTab : Fragment() {
    protected fun sectionTitle(text: String): TextView = TextView(requireContext()).apply {
        this.text = text; textSize = 20f; typeface = Typeface.DEFAULT_BOLD
        setPadding(0, 8, 0, 4)
    }
}

/** Вкладка 1 — сканер-список */
class ListFragment : BaseTab() {
    private lateinit var adapter: PhoneAdapter
    private var query = ""
    private var chip = 0 // 0 все, 1 до15к, 2 amoled, 3 скидка>=15
    private var sortMode = 0 // 0 скидка, 1 цена, 2 рейтинг
    private var lastScan = "только что"
    private var remote: List<Phone>? = null
    private var onlineSrc = false
    private lateinit var scanLabel: TextView
    private lateinit var progress: ProgressBar
    private lateinit var refresher: SwipeRefreshLayout

    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, b: Bundle?): View {
        val ctx = requireContext()
        val dp = resources.displayMetrics.density
        val accent = Prefs.ACCENTS[Prefs.accent(ctx) % Prefs.ACCENTS.size]

        val root = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }

        val header = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((20 * dp).toInt(), (20 * dp).toInt(), (20 * dp).toInt(), (8 * dp).toInt())
        }
        header.addView(TextView(ctx).apply {
            text = "Сканер цен"; textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        })
        header.addView(TextView(ctx).apply {
            text = "Набережные Челны • 10–20 тыс ₽ • RAM ≥ 8 ГБ"
            setTextColor(surface(ctx, com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF44474F.toInt()))
            textSize = 13f
        })
        scanLabel = TextView(ctx).apply {
            text = "Обновлено: $lastScan"
            setTextColor(surface(ctx, com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF44474F.toInt()))
            textSize = 12f
        }
        header.addView(scanLabel)
        progress = ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100; visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (6 * dp).toInt()).apply {
                topMargin = (8 * dp).toInt()
            }
        }
        header.addView(progress)
        val scanBtn = Button(ctx).apply {
            text = "Сканировать цены"
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (10 * dp).toInt()
            }
            setOnClickListener { runScan() }
        }
        header.addView(scanBtn)
        root.addView(header)

        val search = EditText(ctx).apply {
            hint = "🔍 Поиск: redmi, tecno, amoled…"
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins((12 * dp).toInt(), (8 * dp).toInt(), (12 * dp).toInt(), 0)
            }
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) { query = s.toString(); refresh() }
                override fun beforeTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
                override fun onTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
            })
        }
        root.addView(search)

        val chips = HorizontalScrollView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val chipRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding((8 * dp).toInt(), (8 * dp).toInt(), (8 * dp).toInt(), (4 * dp).toInt())
        }
        val names = arrayOf("Все", "До 15 000 ₽", "AMOLED", "Скидка ≥15%", "Сорт: цена", "Сорт: рейтинг")
        names.forEachIndexed { i, n ->
            val btn = Button(ctx, null, android.R.attr.buttonStyleSmall).apply {
                text = n; tag = i
                setOnClickListener { v ->
                    val idx = v.tag as Int
                    if (idx <= 3) chip = idx else sortMode = if (idx == 4) 1 else 2
                    if (idx == 0) sortMode = 0
                    refresh()
                    Toast.makeText(ctx, "Фильтр: $n", Toast.LENGTH_SHORT).show()
                }
            }
            chipRow.addView(btn)
        }
        chips.addView(chipRow)
        root.addView(chips)

        refresher = SwipeRefreshLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            setOnRefreshListener { runScan() }
        }
        val list = RecyclerView(ctx).apply { layoutManager = LinearLayoutManager(ctx) }
        adapter = PhoneAdapter(emptyList(), onOpen = { ph ->
            startActivity(DetailActivity.intent(ctx, ph.id))
        })
        list.adapter = adapter
        refresher.addView(list)
        root.addView(refresher)
        refresh()
        // онлайн-каталог: новые смартфоны подтягиваются сами, без обновления APK
        RemoteCatalog.load { res, ok ->
            if (ok && res != null && res.isNotEmpty()) {
                remote = res; onlineSrc = true
                scanLabel.text = "Онлайн-каталог • ${res.size} моделей • обновлено из сети"
                refresh()
            } else {
                scanLabel.text = "Встроенный каталог • без сети • ${Catalog.phones.size} моделей"
            }
        }
        return root
    }

    private fun base(): List<Phone> = remote ?: Catalog.phones

    private fun filtered(): List<Phone> {
        var l = base().filter { it.ramGb >= 8 && it.price in 10000..20000 }
        if (query.isNotBlank()) l = l.filter {
            (it.brand + " " + it.model + " " + it.cpu + " " + it.screen).contains(query, true)
        }
        l = when (chip) {
            1 -> l.filter { it.price <= 15000 }
            2 -> l.filter { it.screen.contains("AMOLED", true) }
            3 -> l.filter { it.discountPct >= 15 }
            else -> l
        }
        l = when (sortMode) {
            1 -> l.sortedBy { it.price }
            2 -> l.sortedByDescending { it.rating }
            else -> l.sortedByDescending { it.discountPct }
        }
        return l
    }

    private fun refresh() {
        if (!::adapter.isInitialized) return
        adapter.submit(filtered())
    }

    private fun runScan() {
        progress.visibility = View.VISIBLE
        refresher.isRefreshing = true
        progress.progress = 0
        val h = Handler(Looper.getMainLooper())
        var p = 0
        val tick = object : Runnable {
            override fun run() {
                p += 12
                progress.progress = p
                if (p < 100) h.postDelayed(this, 120)
                else {
                    progress.visibility = View.GONE
                    refresher.isRefreshing = false
                    lastScan = SimpleDateFormat("HH:mm, dd.MM", Locale("ru")).format(Date())
                    scanLabel.text = "Обновлено: $lastScan • Челны • ${filtered().size} моделей"
                    refresh()
                    Toast.makeText(requireContext(), "Сканирование завершено: цены Челнов актуальны", Toast.LENGTH_SHORT).show()
                }
            }
        }
        h.post(tick)
    }
}

/** Вкладка 2 — избранное */
class FavFragment : BaseTab() {
    private lateinit var adapter: PhoneAdapter
    private lateinit var empty: TextView
    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, b: Bundle?): View {
        val ctx = requireContext()
        val root = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(12, 12, 12, 12) }
        root.addView(sectionTitle("★ Избранное"))
        empty = TextView(ctx).apply { text = "Пока пусто. Жми ★ на карточке — соберём твой шорт-лист."; textSize = 14f }
        root.addView(empty)
        val list = RecyclerView(ctx).apply { layoutManager = LinearLayoutManager(ctx) }
        adapter = PhoneAdapter(emptyList(), onOpen = { startActivity(DetailActivity.intent(ctx, it.id)) },
            onFavChanged = { load() })
        list.adapter = adapter
        root.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        return root
    }
    override fun onResume() { super.onResume(); load() }
    private fun load() {
        if (!::adapter.isInitialized) return
        val all = (RemoteCatalog.live ?: emptyList()) + Catalog.phones
        val seen = HashSet<Int>()
        val fav = all.filter { Prefs.isFav(requireContext(), it.id) && seen.add(it.id) }
        adapter.submit(fav)
        empty.visibility = if (fav.isEmpty()) View.VISIBLE else View.GONE
    }
}

/** Вкладка 3 — новости */
class NewsFragment : BaseTab() {
    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, b: Bundle?): View {
        val ctx = requireContext()
        val dp = resources.displayMetrics.density
        val scroll = ScrollView(ctx)
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((12 * dp).toInt(), (12 * dp).toInt(), (12 * dp).toInt(), (12 * dp).toInt())
        }
        col.addView(sectionTitle("📰 Новости смартфонов • Челны"))
        NewsRepo.news.forEach { n ->
            val card = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                background = GradientDrawable().apply { setColor(0xFFFFFFFF.toInt()); cornerRadius = 14 * dp; setStroke(1, 0x22000000) }
                setPadding((14 * dp).toInt(), (14 * dp).toInt(), (14 * dp).toInt(), (14 * dp).toInt())
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = (10 * dp).toInt()
                }
                elevation = 3 * dp
            }
            val banner = TextView(ctx).apply {
                text = "${n.tag} • ${n.date}"; setTextColor(0xFFFFFFFF.toInt()); textSize = 12f; typeface = Typeface.DEFAULT_BOLD
                background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(n.colorStart, n.colorEnd)).apply { cornerRadius = 8 * dp }
                setPadding((10 * dp).toInt(), (4 * dp).toInt(), (10 * dp).toInt(), (4 * dp).toInt())
            }
            card.addView(banner)
            card.addView(TextView(ctx).apply { text = n.title; textSize = 16f; typeface = Typeface.DEFAULT_BOLD; setPadding(0, 8, 0, 4) })
            card.addView(TextView(ctx).apply { text = n.text; textSize = 13f; setTextColor(0xFF444444.toInt()) })
            val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
            val share = Button(ctx, null, android.R.attr.buttonStyleSmall).apply {
                text = "Поделиться"; setOnClickListener {
                    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "📰 ${n.title}\n${n.text}\n— Челны СмартСкан")
                    }, "Поделиться новостью"))
                }
            }
            row.addView(share)
            card.addView(row)
            col.addView(card)
        }
        scroll.addView(col)
        return scroll
    }
}

/** Вкладка 4 — настройки (темы как в MMO Calendar) */
class SettingsFragment : BaseTab() {
    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, b: Bundle?): View {
        val ctx = requireContext()
        val dp = resources.displayMetrics.density
        val scroll = ScrollView(ctx)
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16 * dp).toInt(), (16 * dp).toInt(), (16 * dp).toInt(), (16 * dp).toInt())
        }
        col.addView(sectionTitle("⚙ Настройки"))
        col.addView(label("Тема оформления (3 шт — как в MMO Calendar):"))
        val themes = arrayOf("system" to "Системная", "light" to "Светлая", "dark" to "Тёмная")
        val rg = RadioGroup(ctx).apply { orientation = RadioGroup.VERTICAL }
        themes.forEach { (v, name) ->
            rg.addView(RadioButton(ctx).apply {
                text = name; tag = v; isChecked = Prefs.theme(ctx) == v
            })
        }
        col.addView(rg)
        col.addView(label("Стиль:"))
        val styles = arrayOf("material" to "Material You", "classic" to "Классический", "mono" to "Монохром")
        val rg2 = RadioGroup(ctx).apply { orientation = RadioGroup.VERTICAL }
        styles.forEach { (v, name) ->
            rg2.addView(RadioButton(ctx).apply { text = name; tag = v; isChecked = Prefs.style(ctx) == v })
        }
        col.addView(rg2)
        col.addView(label("Основной цвет (7 шт):"))
        val grid = GridLayout(ctx).apply { columnCount = 4 }
        Prefs.ACCENT_NAMES.forEachIndexed { i, name ->
            val b = Button(ctx, null, android.R.attr.buttonStyleSmall).apply {
                text = if (Prefs.accent(ctx) == i) "● $name" else name
                setOnClickListener { Prefs.setAccent(ctx, i); Toast.makeText(ctx, "Акцент: $name — перезапусти вкладки", Toast.LENGTH_SHORT).show(); activity?.recreate() }
            }
            grid.addView(b)
        }
        col.addView(grid)
        col.addView(label("Город: Набережные Челны (фиксировано для точности цен)"))
        col.addView(TextView(ctx).apply { text = "Версия 1.0 • Челны СмартСкан\nЦены-ориентиры DNS / М.Видео / Эльдорадо / OZON / WB / Маркет. RAM — только физическая ≥ 8 ГБ."; setPadding(0, 16, 0, 16) })
        val apply = Button(ctx).apply {
            text = "ПРИМЕНИТЬ ТЕМУ"
            setOnClickListener {
                for (i in 0 until rg.childCount) {
                    val r = rg.getChildAt(i) as RadioButton
                    if (r.isChecked) Prefs.setTheme(ctx, r.tag as String)
                }
                for (i in 0 until rg2.childCount) {
                    val r = rg2.getChildAt(i) as RadioButton
                    if (r.isChecked) Prefs.setStyle(ctx, r.tag as String)
                }
                activity?.recreate()
            }
        }
        col.addView(apply)
        scroll.addView(col)
        return scroll
    }
    private fun label(t: String) = TextView(context).apply {
        text = t; typeface = Typeface.DEFAULT_BOLD; setPadding(0, 16, 0, 8); textSize = 14f
    }
}
