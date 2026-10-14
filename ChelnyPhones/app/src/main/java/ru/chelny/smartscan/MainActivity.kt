package ru.chelny.smartscan

import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private var tab = 0

    override fun onCreate(b: Bundle?) {
        when (Prefs.theme(this)) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
        super.onCreate(b)
        if (b != null) tab = b.getInt("tab", 0)

        val root = androidx.appcompat.widget.LinearLayoutCompat(this).apply {
            orientation = androidx.appcompat.widget.LinearLayoutCompat.VERTICAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        val container = FrameLayout(this).apply {
            id = 1001
            layoutParams = androidx.appcompat.widget.LinearLayoutCompat.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val nav = BottomNavigationView(this).apply {
            layoutParams = androidx.appcompat.widget.LinearLayoutCompat.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            menu.add(0, 1, 0, "Список").setIcon(android.R.drawable.ic_menu_sort_by_size)
            menu.add(0, 2, 0, "Избранное").setIcon(android.R.drawable.btn_star_big_on)
            menu.add(0, 3, 0, "Новости").setIcon(android.R.drawable.ic_dialog_info)
            menu.add(0, 4, 0, "Настройки").setIcon(android.R.drawable.ic_menu_preferences)
            selectedItemId = tab + 1
            setOnItemSelectedListener { item ->
                tab = item.itemId - 1
                showTab()
                true
            }
        }
        root.addView(container); root.addView(nav)
        setContentView(root)
        showTab()
    }

    private fun showTab() {
        val f: Fragment = when (tab) {
            1 -> FavFragment()
            2 -> NewsFragment()
            3 -> SettingsFragment()
            else -> ListFragment()
        }
        supportFragmentManager.beginTransaction().replace(1001, f).commit()
    }

    override fun onSaveInstanceState(o: Bundle) {
        super.onSaveInstanceState(o); o.putInt("tab", tab)
    }
}
