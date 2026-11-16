package com.minimal.rulerlauncher

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val allApps = mutableListOf<AppModel>()
    private val displayedApps = mutableListOf<AppModel>()
    private lateinit var appAdapter: AppAdapter
    private lateinit var rvApps: RecyclerView
    private lateinit var horizontalRuler: HorizontalRulerView
    private lateinit var tvLocationPrayer: TextView
    private lateinit var prefs: SharedPreferences

    private val favoriteViews = mutableListOf<ImageView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("launcher_favs", Context.MODE_PRIVATE)
        tvLocationPrayer = findViewById(R.id.tvLocationPrayer)

        favoriteViews.add(findViewById(R.id.favWhatsApp))
        favoriteViews.add(findViewById(R.id.favMaps))
        favoriteViews.add(findViewById(R.id.favChrome))
        favoriteViews.add(findViewById(R.id.favGallery))

        setupRecyclerView()
        loadInstalledApps()
        setupFavorites()
        setupHorizontalRuler()

        // Default awal pada huruf A
        horizontalRuler.setSelectedLetter('A')
        filterAppsByLetter('A')

        // Ketuk widget lokasi/shalat untuk memaksa pembaruan koordinat GPS
        tvLocationPrayer.setOnClickListener {
            requestLocationAndUpdatePrayer()
        }

        requestLocationAndUpdatePrayer()
    }

    override fun onResume() {
        super.onResume()
        updateLocationAndPrayerWidget()
    }

    private fun requestLocationAndUpdatePrayer() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                101
            )
        } else {
            updateLocationAndPrayerWidget()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            updateLocationAndPrayerWidget()
        }
    }

    private fun updateLocationAndPrayerWidget() {
        // Default koordinat Bogor apabila GPS belum aktif atau izin belum diberikan
        var lat = prefs.getFloat("last_lat", -6.5971f).toDouble()
        var lng = prefs.getFloat("last_lng", 106.8060f).toDouble()
        var cityName = prefs.getString("last_city", "Bogor") ?: "Bogor"

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = lm.getProviders(true)
            var bestLocation: Location? = null
            for (provider in providers) {
                val l = lm.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                    bestLocation = l
                }
            }

            if (bestLocation != null) {
                lat = bestLocation.latitude
                lng = bestLocation.longitude
                prefs.edit()
                    .putFloat("last_lat", lat.toFloat())
                    .putFloat("last_lng", lng.toFloat())
                    .apply()

                resolveCityName(lat, lng) { resolvedCity ->
                    if (resolvedCity.isNotEmpty()) {
                        cityName = resolvedCity
                        prefs.edit().putString("last_city", cityName).apply()
                        val nextPrayer = PrayerHelper.getNextPrayer(lat, lng)
                        tvLocationPrayer.text = "$cityName  •  ${nextPrayer.name} ${nextPrayer.time}"
                    }
                }
            }
        }

        val nextPrayer = PrayerHelper.getNextPrayer(lat, lng)
        tvLocationPrayer.text = "$cityName  •  ${nextPrayer.name} ${nextPrayer.time}"
    }

    @Suppress("DEPRECATION")
    private fun resolveCityName(lat: Double, lng: Double, callback: (String) -> Unit) {
        try {
            val geocoder = Geocoder(this, Locale("id", "ID"))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(lat, lng, 1) { addresses ->
                    val addr = addresses.firstOrNull()
                    val city = addr?.subAdminArea ?: addr?.locality ?: ""
                    val cleanCity = city.replace("Kota ", "").replace("Kabupaten ", "")
                    runOnUiThread { callback(cleanCity) }
                }
            } else {
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                val addr = addresses?.firstOrNull()
                val city = addr?.subAdminArea ?: addr?.locality ?: ""
                val cleanCity = city.replace("Kota ", "").replace("Kabupaten ", "")
                callback(cleanCity)
            }
        } catch (_: Exception) {
            // Abaikan jika perangkat offline, tetap gunakan nama kota terakhir yang tersimpan
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
            val rawIcon = ri.loadIcon(pm)
            val minimalistIcon = IconHelper.createMinimalistIcon(rawIcon, label)
            allApps.add(AppModel(label, pkg, minimalistIcon))
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

    private fun setupHorizontalRuler() {
        horizontalRuler = findViewById(R.id.horizontalRuler)
        horizontalRuler.onLetterSelectedListener = { letter: Char ->
            filterAppsByLetter(letter)
        }
    }

    private fun setupFavorites() {
        for (slotIndex in 0 until 4) {
            val iv = favoriteViews[slotIndex]
            updateFavoriteSlotView(slotIndex, iv)

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

    private fun updateFavoriteSlotView(slotIndex: Int, iv: ImageView) {
        val pkgName = prefs.getString("fav_slot_$slotIndex", null)
        if (pkgName != null) {
            try {
                val rawIcon = packageManager.getApplicationIcon(pkgName)
                val appInfo = packageManager.getApplicationInfo(pkgName, 0)
                val label = packageManager.getApplicationLabel(appInfo).toString()
                iv.setImageDrawable(IconHelper.createMinimalistIcon(rawIcon, label))
                iv.alpha = 1.0f
                return
            } catch (_: PackageManager.NameNotFoundException) {}
        }

        iv.setImageResource(android.R.drawable.ic_input_add)
        iv.alpha = 0.35f
    }

    private fun showAppPickerDialog(slotIndex: Int) {
        val appNames = allApps.map { it.label }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Pilih Aplikasi Favorit ${slotIndex + 1}")
            .setItems(appNames) { _, which ->
                val selectedApp = allApps[which]
                prefs.edit().putString("fav_slot_$slotIndex", selectedApp.packageName).apply()
                favoriteViews[slotIndex].setImageDrawable(selectedApp.icon)
                favoriteViews[slotIndex].alpha = 1.0f
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
