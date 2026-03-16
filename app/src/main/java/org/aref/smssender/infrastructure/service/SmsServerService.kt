package org.aref.smssender.infrastructure.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import org.aref.smssender.BuildConfig
import org.aref.smssender.MainActivity
import org.aref.smssender.R
import org.aref.smssender.application.service.SendSmsService
import org.aref.smssender.infrastructure.adapter.input.http.HttpServerAdapter
import org.aref.smssender.infrastructure.adapter.output.sms.AndroidSmsSenderAdapter
import org.aref.smssender.infrastructure.config.AppConfig
import org.aref.smssender.infrastructure.log.SmsLog

class SmsServerService : Service() {
    companion object {
        @Volatile
        var isRunning: Boolean = false
            private set
    }

    private var httpServer: HttpServerAdapter? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == BuildConfig.ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val appConfig = AppConfig(this)
        val port = appConfig.port

        val smsSenderAdapter = AndroidSmsSenderAdapter(this, appConfig.defaultSimSlot)
        val sendSmsService = SendSmsService(smsSenderAdapter)
        val server = HttpServerAdapter(sendSmsService, appConfig)

        try {
            server.start()
            httpServer = server
            isRunning = true
            SmsLog.info("Servidor iniciado en puerto $port")
            SmsLog.info("SIM configurada: ${if (appConfig.defaultSimSlot == 0) "Default" else "SIM ${appConfig.defaultSimSlot}"}")

            val notification = createNotification(port)
            startForeground(BuildConfig.NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            SmsLog.error("Error al iniciar servidor: ${e.message}")
            e.printStackTrace()
            isRunning = false
            stopSelf()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        SmsLog.info("Servidor detenido")
        httpServer?.stop()
        httpServer = null
        isRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            BuildConfig.CHANNEL_ID,
            "SMS Server",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Servidor HTTP para envío de SMS activo"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(port: Int): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, SmsServerService::class.java).apply { action = BuildConfig.ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, BuildConfig.CHANNEL_ID)
            .setContentTitle("SMS Server Activo")
            .setContentText("Escuchando en puerto $port")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openIntent)
            .addAction(0, "Detener", stopIntent)
            .setOngoing(true)
            .build()
    }
}

