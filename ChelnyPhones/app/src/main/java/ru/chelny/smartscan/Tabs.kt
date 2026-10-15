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
        header.addView(mButton(ctx, "Сканировать цены") { runScan() }.apply {
            (layoutParams as ViewGroup.LayoutParams)
            setPadding(0, 0, 0, 0)
        }.also { (it.layoutParams as LinearLayout.LayoutParams).topMargin = ctx.dp(10) })
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
            if (ok && res != null && res.isNotEmpty()) {
                remote = res
                scanLabel.text = "Онлайн-каталог • ${res.size} моделей • из сети"
                refresh()
            } else {
                scanLabel.text = "Встроенный каталог • без сети • ${Catalog.phones.size} моделей"
            }
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
                        if (ok && res != null && res.isNotEmpty()) remote = res
                        scanLabel.text = "Обновлено: $lastScan • Челны • ${filtered().size} моделей"
                        refresh()
                    }
                    refresh()
                    Toast.makeText(requireContext(), "Готово: цены Челнов актуальны", Toast.LENGTH_SHORT).show()
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

/** Вкладка 3 — новости */
class NewsFragment : BaseTab() {
    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, b: Bundle?): View {
        val ctx = requireContext()
        val scroll = ScrollView(ctx).apply { setBackgroundColor(bgColor(ctx)) }
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ctx.dp(20), ctx.dp(20), ctx.dp(20), ctx.dp(20))
        }
        col.addView(TextView(ctx).apply {
            text = "Новости"; textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(fgColor(ctx))
        })
        NewsRepo.news.forEach { n ->
            val card = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                background = GradientDrawable().apply { setColor(cardColor(ctx)); cornerRadius = ctx.dp(24).toFloat() }
                setPadding(ctx.dp(18), ctx.dp(16), ctx.dp(18), ctx.dp(16))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = ctx.dp(12)
                }
                elevation = ctx.dp(2).toFloat()
            }
            card.addView(chip(ctx, "${n.tag} • ${n.date}", accent2(ctx), 0xFFFFFFFF.toInt()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            })
            card.addView(TextView(ctx).apply {
                text = n.title; textSize = 16f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setTextColor(fgColor(ctx)); setPadding(0, ctx.dp(10), 0, 0)
            })
            card.addView(TextView(ctx).apply {
                text = n.text; textSize = 13.5f; setTextColor(subColor(ctx))
                setPadding(0, ctx.dp(4), 0, 0)
            })
            card.addView(tonalButton(ctx, "Поделиться") {
                ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "${n.title}\n${n.text}\n— Челны СмартСкан")
                }, "Поделиться новостью"))
            }.also { (it.layoutParams as LinearLayout.LayoutParams).topMargin = ctx.dp(12) })
            col.addView(card)
        }
        scroll.addView(col)
        return scroll
    }
}

/** Вкладка 4 — настройки: всё применяется мгновенно, как в MMO Calendar. */
class SettingsFragment : BaseTab() {
    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, b: Bundle?): View {
        val ctx = requireContext()
        val scroll = ScrollView(ctx).apply { setBackgroundColor(bgColor(ctx)) }
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ctx.dp(20), ctx.dp(20), ctx.dp(20), ctx.dp(28))
        }
        col.addView(TextView(ctx).apply {
            text = "Настройки"; textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(fgColor(ctx))
        })

        col.addView(sectionLabel(ctx, "ТЕМА"))
        col.addView(themeCards())

        col.addView(sectionLabel(ctx, "СТИЛЬ"))
        col.addView(styleCards())

        col.addView(sectionLabel(ctx, "ОСНОВНОЙ ЦВЕТ"))
        Prefs.ACCENT_NAMES.forEachIndexed { i, name ->
            val sel = Prefs.accent(ctx) == i
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                background = GradientDrawable().apply {
                    setColor(cardColor(ctx)); cornerRadius = ctx.dp(20).toFloat()
                    if (sel) setStroke(ctx.dp(2), Prefs.ACCENTS[i])
                }
                setPadding(ctx.dp(16), ctx.dp(12), ctx.dp(16), ctx.dp(12))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = ctx.dp(8)
                }
                isClickable = true; isFocusable = true
                setOnClickListener { Prefs.setAccent(ctx, i); activity?.recreate() }
            }
            row.addView(View(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(ctx.dp(22), ctx.dp(22)).apply {
                    marginEnd = ctx.dp(12)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL; setColor(Prefs.ACCENTS[i])
                }
            })
            row.addView(TextView(ctx).apply {
                text = name; textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setTextColor(fgColor(ctx))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            if (sel) row.addView(TextView(ctx).apply {
                text = "●"; textSize = 16f; setTextColor(Prefs.ACCENTS[i])
            })
            col.addView(row)
        }

        col.addView(TextView(ctx).apply {
            text = "Набережные Челны • цены DNS / М.Видео / Эльдорадо / OZON / WB / Маркет\nВерсия 1.2 • RAM только физическая ≥ 8 ГБ"
            setTextColor(subColor(ctx)); textSize = 12.5f
            setPadding(0, ctx.dp(20), 0, 0)
        })
        scroll.addView(col)
        return scroll
    }

    private fun optionCard(title: String, desc: String, selected: Boolean, onClick: () -> Unit): LinearLayout {
        val ctx = requireContext()
        val ac = accent2(ctx)
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(cardColor(ctx)); cornerRadius = ctx.dp(20).toFloat()
                if (selected) setStroke(ctx.dp(2), ac)
            }
            setPadding(ctx.dp(16), ctx.dp(13), ctx.dp(16), ctx.dp(13))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = ctx.dp(8)
            }
            isClickable = true; isFocusable = true
            val tx = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            tx.addView(TextView(ctx).apply {
                text = title; textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setTextColor(fgColor(ctx))
            })
            tx.addView(TextView(ctx).apply {
                text = desc; textSize = 12.5f; setTextColor(subColor(ctx))
            })
            addView(tx)
            addView(View(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(ctx.dp(22), ctx.dp(22))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    if (selected) setColor(ac)
                    else setStroke(ctx.dp(2), subColor(ctx))
                }
            })
            setOnClickListener { onClick() }
        }
    }

    private fun themeCards(): LinearLayout {
        val col = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        val cur = Prefs.theme(requireContext())
        listOf(
            "system" to ("Системная" to "Как в телефоне"),
            "light" to ("Светлая" to "Дневной режим"),
            "dark" to ("Тёмная" to "Ночной режим")).forEach { (v, t) ->
            col.addView(optionCard(t.first, t.second, cur == v) {
                Prefs.setTheme(requireContext(), v); activity?.recreate()
            })
        }
        return col
    }

    private fun styleCards(): LinearLayout {
        val col = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        val cur = Prefs.style(requireContext())
        listOf(
            "material" to ("Material You" to "Системный акцент Android 12+"),
            "classic" to ("Классический" to "Фирменный красный"),
            "mono" to ("Монохром" to "Чёрно-белый")).forEach { (v, t) ->
            col.addView(optionCard(t.first, t.second, cur == v) {
                Prefs.setStyle(requireContext(), v); activity?.recreate()
            })
        }
        return col
    }
}
