package com.minimal.rulerlauncher

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.*

object PrayerHelper {

    data class NextPrayer(val name: String, val time: String)

    /**
     * Menghitung jadwal shalat berikutnya berdasarkan koordinat GPS
     * menggunakan parameter standar Kementerian Agama RI (Fajr 20°, Isha 18°, Ikhtiyat 2 menit).
     */
    fun getNextPrayer(lat: Double, lng: Double): NextPrayer {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val tzOffset = TimeZone.getDefault().getOffset(cal.timeInMillis) / 3600000.0

        val times = calculatePrayerTimes(year, month, day, lat, lng, tzOffset)
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val schedule = listOf(
            "Subuh" to times[0],
            "Dzuhur" to times[1],
            "Ashar" to times[2],
            "Maghrib" to times[3],
            "Isya" to times[4]
        )

        for ((name, mins) in schedule) {
            if (mins > currentMinutes) {
                return NextPrayer(name, formatMinutes(mins))
            }
        }

        // Jika sudah lewat Isya, tampilkan Subuh esok hari
        return NextPrayer("Subuh", formatMinutes(times[0]))
    }

    private fun calculatePrayerTimes(
        year: Int, month: Int, day: Int,
        lat: Double, lng: Double, tz: Double
    ): List<Int> {
        val jd = julianDate(year, month, day) - lng / (15.0 * 24.0)
        val d = jd - 2451545.0

        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(Math.toRadians(g)) + 0.020 * sin(Math.toRadians(2 * g)))

        val e = 23.439 - 0.00000036 * d
        val ra = Math.toDegrees(atan2(cos(Math.toRadians(e)) * sin(Math.toRadians(l)), cos(Math.toRadians(l)))) / 15.0
        val eqt = q / 15.0 - fixHour(ra)
        val decl = Math.toDegrees(asin(sin(Math.toRadians(e)) * sin(Math.toRadians(l))))

        val transit = 12.0 + tz - lng / 15.0 - eqt

        // Sudut Kemenag RI: Subuh -20°, Maghrib -0.833°, Isya -18°
        val fajr = transit - hourAngle(lat, decl, -20.0) + (2.0 / 60.0)
        val dhuhr = transit + (2.0 / 60.0)
        val asrAngle = -Math.toDegrees(atan(1.0 / (1.0 + tan(Math.toRadians(abs(lat - decl))))))
        val asr = transit + hourAngle(lat, decl, asrAngle) + (2.0 / 60.0)
        val maghrib = transit + hourAngle(lat, decl, -0.833) + (2.0 / 60.0)
        val isha = transit + hourAngle(lat, decl, -18.0) + (2.0 / 60.0)

        return listOf(fajr, dhuhr, asr, maghrib, isha).map { (it * 60).roundToInt() }
    }

    private fun hourAngle(lat: Double, decl: Double, angle: Double): Double {
        val num = sin(Math.toRadians(angle)) - sin(Math.toRadians(lat)) * sin(Math.toRadians(decl))
        val den = cos(Math.toRadians(lat)) * cos(Math.toRadians(decl))
        val valCos = (num / den).coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(valCos)) / 15.0
    }

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun fixAngle(a: Double): Double = a - 360.0 * floor(a / 360.0)
    private fun fixHour(a: Double): Double = a - 24.0 * floor(a / 24.0)

    private fun formatMinutes(totalMins: Int): String {
        val norm = ((totalMins % 1440) + 1440) % 1440
        val h = norm / 60
        val m = norm % 60
        return String.format(Locale.ROOT, "%02d:%02d", h, m)
    }
}
