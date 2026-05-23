package co.edu.unipiloto.fuelcontrol.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import java.util.Timer
import java.util.TimerTask

class MapTimerService : Service() {

    private val binder = LocalBinder()
    private var timer: Timer? = null
    private var segundos: Int = 0

    inner class LocalBinder : Binder() {
        fun getService(): MapTimerService = this@MapTimerService
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                segundos++
            }
        }, 1000L, 1000L)
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        timer = null
    }

    fun getSegundos(): Int = segundos
}