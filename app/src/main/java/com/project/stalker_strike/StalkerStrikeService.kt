package com.project.stalker_strike

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.project.stalker_strike.R

// TODO enable foreground service
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

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(channelName)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.radiation_nuclear)
            .build()

        startForeground(1, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}