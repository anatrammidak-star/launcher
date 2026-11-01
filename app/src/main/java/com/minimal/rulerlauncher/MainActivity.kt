package com.minimal.rulerlauncher

import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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

        checkDefaultLauncher()
    }

    override fun onResume() {
        super.onResume()
        // Muat ulang daftar jika ada aplikasi yang baru diinstal/dihapus
        if (allApps.isEmpty()) {
            loadInstalledApps()
        }
    }

    private fun checkDefaultLauncher() {
        if (!isCurrentLauncherDefault()) {
            promptSetDefaultLauncher()
        }
    }

    private fun isCurrentLauncherDefault(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val defaultLauncherPackage = resolveInfo?.activityInfo?.packageName
        return defaultLauncherPackage == packageName
    }

    private fun promptSetDefaultLauncher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (!roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                    startActivity(intent)
                    return
                }
            }
        }
        
        // Cadangan untuk HP Xiaomi/MIUI atau Android versi sebelumnya
        try {
            val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                startActivity(intent)
            } catch (_: Exception) {}
        }
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
            val icon = ri.loadIcon(pm)
            allApps.add(AppModel(label, pkg, icon))
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
                val icon = packageManager.getApplicationIcon(pkgName)
                imageView.setImageDrawable(icon)
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
