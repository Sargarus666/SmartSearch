package ru.chelny.smartscan

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Новости из сети: только топ-источники, обновляются по свайпу. */
object RemoteNews {
    const val URL_JSON =
        "https://github.com/Sargarus666/SmartSearch/releases/download/v2.1/news.json"

    var live: List<NewsItem>? = null
        private set
    var updated: String = ""
        private set

    fun load(cb: (result: List<NewsItem>?, online: Boolean) -> Unit) {
        Thread {
            var out: List<NewsItem>? = null
            try {
                val c = (URL(URL_JSON).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000; readTimeout = 8000
                    setRequestProperty("User-Agent", "ChelnySmartScan/1.0")
                }
                if (c.responseCode == 200) {
                    out = parse(c.inputStream.bufferedReader().readText())
                }
                c.disconnect()
            } catch (_: Exception) { out = null }
            val res = out
            if (res != null) live = res
            Handler(Looper.getMainLooper()).post { cb(res, res != null) }
        }.start()
    }

    fun parse(body: String): List<NewsItem> {
        val root = JSONObject(body)
        updated = root.optString("updated", "")
        val arr = root.getJSONArray("news")
        val list = ArrayList<NewsItem>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                NewsItem(
                    o.optInt("id", i),
                    o.optString("title"),
                    o.optString("text"),
                    o.optString("date"),
                    o.optString("tag"),
                    o.optInt("colorStart", 0xFF7c5cff.toInt()),
                    o.optInt("colorEnd", 0xFF5a3ee0.toInt()),
                    o.optString("imageRes", ""),
                    o.optString("url", "")
                )
            )
        }
        return list.filter { it.title.isNotBlank() }
    }
}
