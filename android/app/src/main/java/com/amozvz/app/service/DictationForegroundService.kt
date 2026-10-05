package com.amozvz.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.amozvz.app.utils.NotificationHelper

class DictationForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            ACTION_START -> {
                val notification = NotificationHelper.buildForegroundNotification(this, "AmozVz is listening...")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    } else {
                        0
                    }
                    ServiceCompat.startForeground(this, NotificationHelper.NOTIFICATION_ID, notification, foregroundType)
                } else {
                    startForeground(NotificationHelper.NOTIFICATION_ID, notification)
                }
            }
            ACTION_STOP -> {
                onStopRequested?.invoke()
                stopSelf()
            }
            ACTION_CANCEL -> {
                onCancelRequested?.invoke()
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    companion object {
        const val ACTION_START = "com.amozvz.app.action.START"
        const val ACTION_STOP = "com.amozvz.app.action.STOP"
        const val ACTION_CANCEL = "com.amozvz.app.action.CANCEL"

        var onStopRequested: (() -> Unit)? = null
        var onCancelRequested: (() -> Unit)? = null

        fun start(context: Context) {
            val intent = Intent(context, DictationForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, DictationForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
