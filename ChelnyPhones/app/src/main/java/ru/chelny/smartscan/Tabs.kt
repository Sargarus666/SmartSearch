package ru.chelny.smartscan

import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.text.SimpleDateFormat
import java.util.*

open class BaseTab : Fragment() {
    override fun onViewCreated(v: View, b: Bundle?) {
        super.onViewCreated(v, b)
        v.setBackgroundColor(bgColor(requireContext()))
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
    private lateinit var scanLabel: TextView
    private lateinit var progress: ProgressBar
    private lateinit var refresher: SwipeRefreshLayout
    private lateinit var chipRow: LinearLayout

    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, b: Bundle?): View {
        val ctx = requireContext()
        val dp = ctx.dp(1)
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgColor(ctx))
        }

        val header = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ctx.dp(20), ctx.dp(20), ctx.dp(20), ctx.dp(4))
        }
        header.addView(TextView(ctx).apply {
            text = "Сканер цен"; textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(fgColor(ctx))
        })
        header.addView(TextView(ctx).apply {
            text = "Набережные Челны • 10–20 тыс ₽ • RAM ≥ 8 ГБ"
            setTextColor(subColor(ctx)); textSize = 13f
        })
        scanLabel = TextView(ctx).apply {
            text = "Обновлено: $lastScan"; setTextColor(subColor(ctx)); textSize = 12f
            setPadding(0, ctx.dp(2), 0, 0)
        }
        header.addView(scanLabel)
        progress = ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100; visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(6)).apply {
                topMargin = ctx.dp(8)
            }
            progressTintList = android.content.res.ColorStateList.valueOf(accent2(ctx))
        }
        header.addView(progress)
        header.addView(mButton(ctx, "Сканировать цены") { runScan() }.also {
            (it.layoutParams as LinearLayout.LayoutParams).topMargin = ctx.dp(10)
        })
        root.addView(header)

        root.addView(searchField(ctx, "Поиск: redmi, tecno, amoled…") { query = it; refresh() }.apply {
            (layoutParams as LinearLayout.LayoutParams).setMargins(
                ctx.dp(16), ctx.dp(10), ctx.dp(16), 0)
        })

        val chips = HorizontalScrollView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            isHorizontalScrollBarEnabled = false
        }
        chipRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(ctx.dp(16), ctx.dp(10), ctx.dp(16), ctx.dp(4))
        }
        chips.addView(chipRow)
        root.addView(chips)
        buildChips()

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
        RemoteCatalog.load { res, ok ->
            try {
                if (!isAdded) return@load
                if (ok && res != null && res.isNotEmpty()) {
                    remote = res
                    val upd = RemoteCatalog.updated.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""
                    scanLabel.text = "Челны • онлайн-каталог • ${res.size} моделей$upd"
                    refresh()
                } else {
                    scanLabel.text = "Встроенный каталог • без сети • ${Catalog.phones.size} моделей"
                }
            } catch (_: Exception) {}
        }
        return root
    }

    private fun buildChips() {
        chipRow.removeAllViews()
        val filters = arrayOf("Все", "До 15 000 ₽", "AMOLED", "Скидка ≥15%")
        filters.forEachIndexed { i, n ->
            chipRow.addView(chipView(requireContext(), n, chip == i) {
                chip = i; buildChips(); refresh()
            })
        }
        val sorts = arrayOf("Цена ↓" to 1, "Рейтинг ★" to 2)
        sorts.forEach { (n, v) ->
            chipRow.addView(chipView(requireContext(), n, sortMode == v) {
                sortMode = v; buildChips(); refresh()
            })
        }
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
                if (p < 100) h.postDelayed(this, 110)
                else {
                    progress.visibility = View.GONE
                    refresher.isRefreshing = false
                    lastScan = SimpleDateFormat("HH:mm, dd.MM", Locale("ru")).format(Date())
                    scanLabel.text = "Обновлено: $lastScan • Челны • ${filtered().size} моделей"
                    RemoteCatalog.load { res, ok ->
                        try {
                            if (!isAdded) return@load
                            if (ok && res != null && res.isNotEmpty()) remote = res
                            scanLabel.text = "Обновлено: $lastScan • Челны • ${filtered().size} моделей"
                            refresh()
                        } catch (_: Exception) {}
                    }
                    refresh()
                    try { Toast.makeText(requireContext(), "Готово: цены Челнов актуальны", Toast.LENGTH_SHORT).show() } catch (_: Exception) {}
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
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgColor(ctx))
            setPadding(ctx.dp(20), ctx.dp(20), ctx.dp(20), ctx.dp(12))
        }
        root.addView(TextView(ctx).apply {
            text = "Избранное"; textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(fgColor(ctx))
        })
        empty = TextView(ctx).apply {
            text = "Пока пусто. Нажми ★ на карточке."
            setTextColor(subColor(ctx)); textSize = 14f
            setPadding(0, ctx.dp(8), 0, 0)
        }
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

/** Вкладка 3 — новости: квадрат слева, свайп вниз обновляет из сети */
class NewsFragment : BaseTab() {
    private lateinit var refresher: SwipeRefreshLayout
    private lateinit var col: LinearLayout
    private var items: List<NewsItem> = NewsRepo.news

    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, b: Bundle?): View {
        val ctx = requireContext()
        refresher = SwipeRefreshLayout(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setOnRefreshListener { reload() }
        }
        val scroll = ScrollView(ctx).apply { setBackgroundColor(bgColor(ctx)) }
        col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ctx.dp(20), ctx.dp(20), ctx.dp(20), ctx.dp(20))
        }
        scroll.addView(col)
        refresher.addView(scroll)
        render()
        reload()
        return refresher
    }

    private fun reload() {
        if (!::refresher.isInitialized) return
        refresher.isRefreshing = true
        RemoteNews.load { res, ok ->
            try {
                if (!isAdded) return@load
                refresher.isRefreshing = false
                if (ok && res != null && res.isNotEmpty()) {
                    items = res
                    render()
                    Toast.makeText(ctx(), "Новости обновлены из сети", Toast.LENGTH_SHORT).show()
                } else if (items === NewsRepo.news) {
                    render()
                }
            } catch (_: Exception) {
                try { refresher.isRefreshing = false } catch (_: Exception) {}
            }
        }
    }

    private fun ctx() = requireContext()

    private fun render() {
        if (!::col.isInitialized) return
        val ctx = requireContext()
        col.removeAllViews()
        col.addView(TextView(ctx).apply {
            text = "Новости"; textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(fgColor(ctx))
        })
        if (RemoteNews.updated.isNotBlank()) col.addView(TextView(ctx).apply {
            text = "Обновлено из сети: ${RemoteNews.updated}"
            textSize = 12f; setTextColor(subColor(ctx))
            setPadding(0, ctx.dp(2), 0, 0)
        })
        items.forEach { n -> col.addView(newsCard(n)) }
    }

    /** Горизонтальная карточка: квадрат 96dp слева, текст справа */
    private fun newsCard(n: NewsItem): LinearLayout {
        val ctx = requireContext()
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply { setColor(cardColor(ctx)); cornerRadius = ctx.dp(24).toFloat() }
            setPadding(ctx.dp(14), ctx.dp(14), ctx.dp(14), ctx.dp(14))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = ctx.dp(12)
            }
            elevation = ctx.dp(2).toFloat()
            isClickable = true; isFocusable = true
        }
        // квадратное превью слева — целиком, без обрезки по бокам
        val thumb = ImageView(ctx).apply {
            val s = ctx.dp(96)
            layoutParams = LinearLayout.LayoutParams(s, s).apply { marginEnd = ctx.dp(14) }
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = GradientDrawable().apply {
                setColor(0xFFE8EAF6.toInt()); cornerRadius = ctx.dp(16).toFloat()
            }
            clipToOutline = true
        }
        try {
            val resId = ctx.resources.getIdentifier(n.imageRes, "drawable", ctx.packageName)
            if (resId != 0) thumb.setImageResource(resId)
        } catch (_: Exception) {}
        card.addView(thumb)

        val tx = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        tx.addView(chip(ctx, "${n.tag} • ${n.date}", accent2(ctx), 0xFFFFFFFF.toInt()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        })
        tx.addView(TextView(ctx).apply {
            text = n.title; textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(fgColor(ctx)); setPadding(0, ctx.dp(6), 0, 0)
            maxLines = 3
        })
        tx.addView(TextView(ctx).apply {
            text = "Читать источник →"; textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(accent2(ctx)); setPadding(0, ctx.dp(6), 0, 0)
        })
        card.addView(tx)
        fun openUrl() {
            try {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(n.url)))
            } catch (_: Exception) {
                Toast.makeText(ctx, n.url, Toast.LENGTH_LONG).show()
            }
        }
        card.setOnClickListener { openUrl() }
        return card
    }
}

/** Вкладка 4 — настройки как в MMO Calendar: сегменты, круги цветов, «О приложении». Всё применяется мгновенно. */
class SettingsFragment : BaseTab() {
    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, b: Bundle?): View {
        val ctx = requireContext()
        val scroll = ScrollView(ctx).apply { setBackgroundColor(bgColor(ctx)) }
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ctx.dp(16), ctx.dp(20), ctx.dp(16), ctx.dp(28))
        }
        col.addView(groupCard(ctx, "Тема", segmented(ctx,
            listOf("system" to "Система", "light" to "Светлая", "dark" to "Тёмная"),
            Prefs.theme(ctx)) { Prefs.setTheme(ctx, it); activity?.recreate() }))

        col.addView(groupCard(ctx, "Стиль", segmented(ctx,
            listOf("material" to "Material You", "classic" to "Классика", "mono" to "Монохром"),
            Prefs.style(ctx)) { Prefs.setStyle(ctx, it); activity?.recreate() }))

        col.addView(colorGroup(ctx))
        col.addView(aboutGroup(ctx))
        scroll.addView(col)
        return scroll
    }

    /** Карточка группы с фиолетовым заголовком — как в MMO. */
    private fun groupCard(ctx: android.content.Context, title: String, body: View): LinearLayout =
        LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(cardColor(ctx)); cornerRadius = ctx.dp(22).toFloat()
            }
            setPadding(ctx.dp(18), ctx.dp(16), ctx.dp(18), ctx.dp(18))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = ctx.dp(12)
            }
            elevation = ctx.dp(2).toFloat()
            addView(TextView(ctx).apply {
                text = title; textSize = 17f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setTextColor(accent2(ctx))
                setPadding(0, 0, 0, ctx.dp(12))
            })
            addView(body)
        }

    /** Сегмент-переключатель: тёмная капсула, выбранное — фиолетовая пилюля. */
    private fun segmented(
        ctx: android.content.Context,
        items: List<Pair<String, String>>,
        current: String,
        onPick: (String) -> Unit
    ): LinearLayout {
        val ac = accent2(ctx)
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                setColor(if (isDark(ctx)) 0xFF0B0812.toInt() else 0xFFF0EDF5.toInt())
                cornerRadius = ctx.dp(99).toFloat() * 100
            }
            setPadding(ctx.dp(5), ctx.dp(5), ctx.dp(5), ctx.dp(5))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            items.forEach { (v, name) ->
                val sel = v == current
                addView(TextView(ctx).apply {
                    text = name; textSize = 13.5f; gravity = Gravity.CENTER
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    setTextColor(if (sel) 0xFFFFFFFF.toInt() else subColor(ctx))
                    background = GradientDrawable().apply {
                        setColor(if (sel) ac else 0x00000000)
                        cornerRadius = ctx.dp(99).toFloat() * 100
                    }
                    setPadding(0, ctx.dp(11), 0, ctx.dp(11))
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    isClickable = true; isFocusable = true
                    setOnClickListener { onPick(v) }
                })
            }
        }
    }

    /** Ряд кругов цветов с галочкой + подпись — как в MMO. */
    private fun colorGroup(ctx: android.content.Context): LinearLayout {
        val ac = accent2(ctx)
        val cur = Prefs.accent(ctx)
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val scroll = HorizontalScrollView(ctx).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        Prefs.ACCENT_NAMES.forEachIndexed { i, _ ->
            val sel = i == cur
            val cell = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = ctx.dp(12)
                }
                isClickable = true; isFocusable = true
                setOnClickListener { Prefs.setAccent(ctx, i); activity?.recreate() }
            }
            val circle = FrameLayout(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(ctx.dp(44), ctx.dp(44))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Prefs.ACCENTS[i])
                    if (sel) setStroke(ctx.dp(3), 0xFFFFFFFF.toInt())
                }
            }
            if (sel) circle.addView(TextView(ctx).apply {
                text = "✓"; textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setTextColor(0xFFFFFFFF.toInt()); gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            })
            cell.addView(circle)
            row.addView(cell)
        }
        scroll.addView(row)
        val body = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        body.addView(scroll)
        body.addView(TextView(ctx).apply {
            text = Prefs.ACCENT_NAMES[cur % Prefs.ACCENT_NAMES.size]
            textSize = 14f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(fgColor(ctx)); setPadding(0, ctx.dp(10), 0, 0)
        })
        return groupCard(ctx, "Основной цвет", body)
    }

    /** «О приложении»: строки + описание — как в MMO. */
    private fun aboutGroup(ctx: android.content.Context): LinearLayout {
        val body = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        fun infoRow(k: String, v: String) {
            val r = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, ctx.dp(7), 0, ctx.dp(7))
            }
            r.addView(TextView(ctx).apply {
                text = k; textSize = 15f; setTextColor(subColor(ctx))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            r.addView(TextView(ctx).apply {
                text = v; textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setTextColor(fgColor(ctx))
            })
            body.addView(r)
        }
        infoRow("Версия", "2.1")
        infoRow("Город", "Челны")
        infoRow("Каталог", "RAM ≥ 8 ГБ • 10–20 тыс ₽")
        body.addView(TextView(ctx).apply {
            text = "Сканер цен и акций на бюджетные смартфоны в Набережных Челнах. Цены-ориентиры DNS, М.Видео, Эльдорадо, OZON, WB и Маркета."
            textSize = 13.5f; setTextColor(subColor(ctx))
            setPadding(0, ctx.dp(8), 0, 0)
        })
        return groupCard(ctx, "О приложении", body)
    }
}
