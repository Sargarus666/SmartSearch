package ru.chelny.smartscan

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import coil.load

/** Полноэкранная галерея: листается свайпом, показывает счётчик. */
class GalleryActivity : AppCompatActivity() {

    companion object {
        private const val K_URLS = "urls"
        private const val K_POS = "pos"
        private const val K_TITLE = "title"
        fun intent(ctx: Context, urls: List<String>, pos: Int, title: String) =
            Intent(ctx, GalleryActivity::class.java)
                .putStringArrayListExtra(K_URLS, ArrayList(urls))
                .putExtra(K_POS, pos)
                .putExtra(K_TITLE, title)
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val urls = intent.getStringArrayListExtra(K_URLS) ?: arrayListOf()
        var pos = intent.getIntExtra(K_POS, 0)
        val title = intent.getStringExtra(K_TITLE) ?: ""
        if (urls.isEmpty()) { finish(); return }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF000000.toInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(8))
        }
        val back = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            background = null
            contentDescription = "Закрыть"
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
            setColorFilter(0xFFFFFFFF.toInt())
            setOnClickListener { finish() }
        }
        val counter = TextView(this).apply {
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        fun paint() { counter.text = "${pos + 1} / ${urls.size} • $title" }
        paint()
        top.addView(back); top.addView(counter)
        root.addView(top)

        val list = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@GalleryActivity, LinearLayoutManager.HORIZONTAL, false)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        PagerSnapHelper().attachToRecyclerView(list)
        list.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            inner class VH(val iv: ImageView) : RecyclerView.ViewHolder(iv)
            override fun onCreateViewHolder(p: ViewGroup, v: Int): RecyclerView.ViewHolder {
                val iv = ImageView(p.context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setBackgroundColor(0xFF000000.toInt())
                }
                return VH(iv)
            }
            override fun getItemCount() = urls.size
            override fun onBindViewHolder(h: RecyclerView.ViewHolder, p: Int) {
                val iv = (h as VH).iv
                try {
                    iv.load(urls[p]) {
                        crossfade(true)
                        error(android.R.drawable.ic_menu_camera)
                    }
                } catch (_: Exception) {}
            }
        }
        list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(rv: RecyclerView, s: Int) {
                if (s == RecyclerView.SCROLL_STATE_IDLE) {
                    val lm = rv.layoutManager as LinearLayoutManager
                    pos = lm.findFirstVisibleItemPosition().takeIf { it >= 0 } ?: pos
                    paint()
                }
            }
        })
        root.addView(list)
        root.addView(TextView(this).apply {
            text = "Листайте свайпом влево-вправо"
            setTextColor(0xFFBBBBBB.toInt()); textSize = 12f; gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(20))
        })
        setContentView(root)
        try {
            list.scrollToPosition(pos)
        } catch (_: Exception) {}
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
