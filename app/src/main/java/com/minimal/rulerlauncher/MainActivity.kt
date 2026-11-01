package com.minimal.rulerlauncher

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private val allApps = mutableListOf<AppModel>()
    private val displayedApps = mutableListOf<AppModel>()
    private lateinit var appAdapter: AppAdapter
    private lateinit var rvApps: RecyclerView
    private lateinit var prefs: SharedPreferences

    private val favoriteViews = mutableListOf<ImageView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("launcher_favs", Context.MODE_PRIVATE)

        favoriteViews.add(findViewById(R.id.favWhatsApp))
        favoriteViews.add(findViewById(R.id.favMaps))
        favoriteViews.add(findViewById(R.id.favChrome))
        favoriteViews.add(findViewById(R.id.favGallery))

        setupRecyclerView()
        loadInstalledApps()
        setupFavorites()
        setupRuler()
    }

    private fun setupRecyclerView() {
        rvApps = findViewById(R.id.rvApps)
        rvApps.layoutManager = LinearLayoutManager(this)
        appAdapter = AppAdapter(displayedApps) { app ->
            val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
            launchIntent?.let { startActivity(it) }
        }
        rvApps.adapter = appAdapter
    }

    private fun getCustomMinimalIcon(packageName: String, originalIcon: Drawable): Drawable {
        val resName = when {
            packageName.contains("whatsapp", ignoreCase = true) -> "ic_minimal_wa"
            packageName.contains("maps", ignoreCase = true) -> "ic_minimal_maps"
            packageName.contains("chrome", ignoreCase = true) -> "ic_minimal_chrome"
            packageName.contains("gallery", ignoreCase = true) || packageName.contains("photos", ignoreCase = true) -> "ic_minimal_gallery"
            packageName.contains("zoom", ignoreCase = true) -> "ic_minimal_zoom"
            else -> null
        }

        if (resName != null) {
            val resId = resources.getIdentifier(resName, "drawable", this.packageName)
            if (resId != 0) {
                val vectorDrawable = ResourcesCompat.getDrawable(resources, resId, theme)
                if (vectorDrawable != null) return vectorDrawable
            }
        }
        return IconHelper.toMinimalistIcon(originalIcon)
    }

    private fun loadInstalledApps() {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveList = pm.queryIntentActivities(intent, 0)

        allApps.clear()
        for (ri in resolveList) {
            val pkg = ri.activityInfo.packageName
            if (pkg == packageName) continue
            val label = ri.loadLabel(pm).toString()
            val rawIcon = ri.loadIcon(pm)
            val minimalistIcon = getCustomMinimalIcon(pkg, rawIcon)
            allApps.add(AppModel(label, pkg, minimalistIcon))
        }
        allApps.sortBy { it.label.lowercase() }

        filterAppsByLetter('A')
    }

    private fun filterAppsByLetter(letter: Char) {
        displayedApps.clear()
        displayedApps.addAll(allApps.filter {
            it.label.trim().startsWith(letter, ignoreCase = true)
        })
        appAdapter.notifyDataSetChanged()
    }

    private fun setupRuler() {
        val ruler: RulerSliderView = findViewById(R.id.rulerSlider)
        ruler.onLetterSelectedListener = { letter: Char ->
            filterAppsByLetter(letter)
        }
    }

    private fun setupFavorites() {
        for (slotIndex in 0 until 4) {
            val iv = favoriteViews[slotIndex]
            updateFavoriteSlotIcon(slotIndex, iv)

            iv.setOnClickListener {
                val pkgName = prefs.getString("fav_slot_$slotIndex", null)
                if (pkgName != null) {
                    val launchIntent = packageManager.getLaunchIntentForPackage(pkgName)
                    if (launchIntent != null) {
                        startActivity(launchIntent)
                    } else {
                        showAppPickerDialog(slotIndex)
                    }
                } else {
                    showAppPickerDialog(slotIndex)
                }
            }

            iv.setOnLongClickListener {
                showAppPickerDialog(slotIndex)
                true
            }
        }
    }

    private fun updateFavoriteSlotIcon(slotIndex: Int, imageView: ImageView) {
        val pkgName = prefs.getString("fav_slot_$slotIndex", null)
        if (pkgName != null) {
            try {
                val rawIcon = packageManager.getApplicationIcon(pkgName)
                val minimalIcon = getCustomMinimalIcon(pkgName, rawIcon)
                imageView.setImageDrawable(minimalIcon)
                imageView.alpha = 1.0f
                return
            } catch (_: PackageManager.NameNotFoundException) {}
        }
        imageView.setImageResource(android.R.drawable.ic_input_add)
        imageView.alpha = 0.4f
    }

    private fun showAppPickerDialog(slotIndex: Int) {
        val appNames = allApps.map { it.label }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Pilih Aplikasi Favorit ${slotIndex + 1}")
            .setItems(appNames) { _, which ->
                val selectedApp = allApps[which]
                prefs.edit().putString("fav_slot_$slotIndex", selectedApp.packageName).apply()
                updateFavoriteSlotIcon(slotIndex, favoriteViews[slotIndex])
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {}

    inner class AppAdapter(
        private val list: List<AppModel>,
        private val onClick: (AppModel) -> Unit
    ) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val icon: ImageView = v.findViewById(R.id.imgAppIcon)
            val name: TextView = v.findViewById(R.id.tvAppName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = list[position]
            holder.name.text = app.label
            holder.icon.setImageDrawable(app.icon)
            holder.itemView.setOnClickListener { onClick(app) }
        }

        override fun getItemCount(): Int = list.size
    }
}
