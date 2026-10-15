package ru.chelny.smartscan

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Онлайн-каталог: новые смартфоны подтягиваются из сети, встроенный — запасной вариант. */
object RemoteCatalog {
    const val URL_JSON =
        "https://github.com/Sargarus666/SmartSearch/releases/download/v2.1/phones.json"

    /** Дата обновления каталога на сервере (показываем в шапке). */
    var updated: String = ""
        private set

    /** Последний загруженный онлайн-список (видят все вкладки). */
    var live: List<Phone>? = null
        private set

    fun load(cb: (result: List<Phone>?, online: Boolean) -> Unit) {
        Thread {
            var out: List<Phone>? = null
            try {
                val c = (URL(URL_JSON).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000; readTimeout = 8000
                    setRequestProperty("User-Agent", "ChelnySmartScan/1.0")
                }
                if (c.responseCode == 200) {
                    val body = c.inputStream.bufferedReader().readText()
                    out = parse(body)
                }
                c.disconnect()
            } catch (_: Exception) { out = null }
            val res = out
            if (res != null) live = res
            Handler(Looper.getMainLooper()).post { cb(res, res != null) }
        }.start()
    }

    fun parse(body: String): List<Phone> {
        val root = JSONObject(body)
        updated = root.optString("updated", "")
        val arr = root.getJSONArray("phones")
        val list = ArrayList<Phone>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            fun stores(key: String, online: Boolean): List<StoreOffer> {
                val a = o.getJSONArray(key)
                val l = ArrayList<StoreOffer>()
                for (j in 0 until a.length()) {
                    val s = a.getJSONObject(j)
                    l.add(StoreOffer(
                        s.optString("shop"), s.optString("address"),
                        s.optInt("price"), s.optString("note"),
                        s.optString("url"), online
                    ))
                }
                return l
            }
            fun hex(key: String): Int {
                val s = o.optString(key, "#000000")
                return try { android.graphics.Color.parseColor(s) } catch (_: Exception) { 0xFF000000.toInt() }
            }
            val gal = ArrayList<String>()
            try {
                val ga = o.optJSONArray("gallery")
                if (ga != null) for (j in 0 until ga.length()) gal.add(ga.optString(j))
            } catch (_: Exception) {}
            if (gal.isEmpty() && o.optString("photoUrl", "").isNotBlank()) gal.add(o.optString("photoUrl", ""))
            list.add(Phone(
                o.optInt("id"), o.optString("brand"), o.optString("model"),
                o.optInt("ramGb", 8), o.optInt("storageGb", 256),
                o.optInt("price"), o.optInt("oldPrice"),
                o.optString("promo"), o.optDouble("rating", 4.5),
                o.optString("screen"), o.optString("battery"),
                o.optString("camera"), o.optString("cpu"),
                hex("colorStart"), hex("colorEnd"),
                o.optString("photoUrl", ""), o.optString("photoRes", ""),
                o.optString("accent"),
                stores("offline", false), stores("online", true),
                gal
            ))
        }
        // жёсткий фильтр как в ТЗ: только физическая RAM>=8 и цена 10-20к
        return list.filter { it.ramGb >= 8 && it.price in 10000..20000 }
    }
}
