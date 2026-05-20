package co.edu.unipiloto.fuelcontrol.services

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.app.ActivityCompat
import co.edu.unipiloto.fuelcontrol.NotificationHelper
import com.google.android.gms.maps.model.LatLng
import kotlin.math.*

class SmartRouteService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val userLat = intent?.getDoubleExtra("user_lat", Double.NaN) ?: run { stopSelf(); return START_NOT_STICKY }
        val userLng = intent.getDoubleExtra("user_lng", Double.NaN)

        if (userLat.isNaN() || userLng.isNaN()) {
            stopSelf()
            return START_NOT_STICKY
        }

        val nombres = intent.getStringArrayExtra("station_names") ?: run { stopSelf(); return START_NOT_STICKY }
        val lats    = intent.getDoubleArrayExtra("station_lats")  ?: run { stopSelf(); return START_NOT_STICKY }
        val lngs    = intent.getDoubleArrayExtra("station_lngs")  ?: run { stopSelf(); return START_NOT_STICKY }

        Thread {
            val userLatLng = LatLng(userLat, userLng)

            var minDist = Double.MAX_VALUE
            var nombreMasCercana = ""

            for (i in nombres.indices) {
                val stationLatLng = LatLng(lats[i], lngs[i])
                val dist = distanciaKm(userLatLng, stationLatLng)
                if (dist < minDist) {
                    minDist = dist
                    nombreMasCercana = nombres[i]
                }
            }

            val distTexto = if (minDist < 1.0)
                "${(minDist * 1000).toInt()} m"
            else
                "${"%.1f".format(minDist)} km"

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                stopSelf();
            }
            NotificationHelper.sendRouteSuggestion(
                this,
                nombreMasCercana,
                distTexto,
                "Precio más competitivo"
            )

            stopSelf()

        }.start()

        return START_NOT_STICKY
    }

    private fun distanciaKm(a: LatLng, b: LatLng): Double {
        val R = 6371.0
        val dLat = Math.toRadians(b.latitude  - a.latitude)
        val dLng = Math.toRadians(b.longitude - a.longitude)
        val sinLat = sin(dLat / 2)
        val sinLng = sin(dLng / 2)
        val c = 2 * asin(
            sqrt(
                sinLat * sinLat +
                        cos(Math.toRadians(a.latitude)) *
                        cos(Math.toRadians(b.latitude)) *
                        sinLng * sinLng
            )
        )
        return R * c
    }
}