package com.example.cardimmer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    private lateinit var prefs: PreferencesManager
    private var dimOverlayManager: DimOverlayManager? = null
    private var floatingButtonManager: FloatingButtonManager? = null
    private var muteButtonManager: MuteButtonManager? = null

    companion object {
        const val ACTION_STOP = "com.example.cardimmer.ACTION_STOP"
        const val ACTION_UPDATE_SETTINGS = "com.example.cardimmer.ACTION_UPDATE_SETTINGS"
        private const val NOTIFICATION_CHANNEL_ID = "dimmer_service_channel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesManager(this)
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        dimOverlayManager = DimOverlayManager(this, prefs)
        floatingButtonManager = FloatingButtonManager(this, prefs, dimOverlayManager!!)
        muteButtonManager = MuteButtonManager(this, prefs)

        dimOverlayManager?.show()
        floatingButtonManager?.show()
        if (prefs.isMuteEnabled) {
            muteButtonManager?.show()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            prefs.isEnabled = false
            stopSelf()
            return START_NOT_STICKY
        } else if (intent?.action == ACTION_UPDATE_SETTINGS) {
            dimOverlayManager?.updateDimLevel(prefs.dimLevel)
            floatingButtonManager?.updateSettings()
            
            if (prefs.isMuteEnabled) {
                muteButtonManager?.show()
                muteButtonManager?.updateSettings()
            } else {
                muteButtonManager?.hide()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingButtonManager?.hide()
        muteButtonManager?.hide()
        dimOverlayManager?.hide()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, OverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.dimmer_active))
            .setSmallIcon(R.drawable.ic_launcher) // In a real app use a proper notification icon
            .setContentIntent(mainPendingIntent)
            .addAction(0, getString(R.string.stop_dimming), stopPendingIntent)
            .setOngoing(true)
            .build()
    }
}
