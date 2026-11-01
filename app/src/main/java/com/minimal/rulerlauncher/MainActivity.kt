package com.minimal.rulerlauncher

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private val allApps = mutableListOf<AppModel>()
    private val displayedApps = mutableListOf<AppModel>()
    private lateinit var appAdapter: AppAdapter
    private lateinit var rvApps: RecyclerView
    private lateinit var rulerView: RulerSliderView
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

        // Buka pada huruf Z secara default
        rulerView.setSelectedLetter('Z')
        filterAppsByLetter('Z')
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
            
            // Konversi dinamis ke outline putih
            val outlineIcon = IconHelper.toMinimalistOutline(rawIcon)
            allApps.add(AppModel(label, pkg, outlineIcon))
        }
        allApps.sortBy { it.label.lowercase() }
    }

    private fun filterAppsByLetter(letter: Char) {
        displayedApps.clear()
        displayedApps.addAll(allApps.filter {
            it.label.trim().startsWith(letter, ignoreCase = true)
        })
        appAdapter.notifyDataSetChanged()
    }

    private fun setupRuler() {
        rulerView = findViewById(R.id.rulerSlider)
        rulerView.onLetterSelectedListener = { letter: Char ->
            filterAppsByLetter(letter)
        }
    }

    private fun setupFavorites() {
        for (slotIndex in 0 until 4) {
            val iv = favoriteViews[slotIndex]
            updateFavoriteSlotView(slotIndex, iv)

            // Klik biasa: Buka aplikasi
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

            // Tahan lama: Ubah pilihan aplikasi favorit
            iv.setOnLongClickListener {
                showAppPickerDialog(slotIndex)
                true
            }
        }
    }

    private fun updateFavoriteSlotView(slotIndex: Int, iv: ImageView) {
        val pkgName = prefs.getString("fav_slot_$slotIndex", null)
        if (pkgName != null) {
            try {
                val rawIcon = packageManager.getApplicationIcon(pkgName)
                iv.setImageDrawable(IconHelper.toMinimalistOutline(rawIcon))
                iv.alpha = 1.0f
                return
            } catch (_: PackageManager.NameNotFoundException) {}
        }
        
        // Ikon placeholder default jika slot belum diisi aplikasi
        iv.setImageResource(android.R.drawable.ic_input_add)
        iv.alpha = 0.3f
    }

    private fun showAppPickerDialog(slotIndex: Int) {
        val appNames = allApps.map { it.label }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Pilih Aplikasi Favorit ${slotIndex + 1}")
            .setItems(appNames) { _, which ->
                val selectedApp = allApps[which]
                prefs.edit().putString("fav_slot_$slotIndex", selectedApp.packageName).apply()
                updateFavoriteSlotView(slotIndex, favoriteViews[slotIndex])
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
