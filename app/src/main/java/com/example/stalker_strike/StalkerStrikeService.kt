package com.example.stalker_strike

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat

class StalkerStrikeService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val contentText = "Рівень радіації в нормі"

        val channelId = "StalkerStrikeChannel"
        val channelName = "StalkerStrike"
        val notificationChannel =
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)

        val notification = NotificationCompat.Builder(this, "StalkerStrikeChannel")
            .setContentTitle("StalkerStrike")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.radiation_nuclear)
            .build()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MyApp::MyForegroundService"
        )

        wakeLock?.acquire(10 * 60 * 1000L /*10 minutes*/)

        startForeground(1, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}