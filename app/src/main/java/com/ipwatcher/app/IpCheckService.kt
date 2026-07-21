package com.ipwatcher.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

class IpCheckService : Service() {

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val CHANNEL_ID = "ip_watcher_channel"
        const val NOTIF_ID = 1
        const val ACTION_STOP = "com.ipwatcher.app.ACTION_STOP"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelfService()
            return START_NOT_STICKY
        }

        startForeground(NOTIF_ID, buildNotification("Đang khởi động…"))
        Prefs.setRunning(this, true)
        startLoop()
        return START_STICKY
    }

    private fun startLoop() {
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                val ip = fetchPublicIp()
                val desired = Prefs.getDesiredPrefix(this@IpCheckService).trim()
                android.util.Log.d("IpWatcher", "Checked IP=$ip desired=$desired")

                val desiredList = desired.split(";", ",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                val isMatch = desiredList.isEmpty() || (ip != null && desiredList.any { ip.startsWith(it) })

                when {
                    ip == null -> {
                        updateNotification("⚠️ Không lấy được IP (có thể do VPN chặn) - thử lại sau ${Prefs.getCheckIntervalSec(this@IpCheckService)}s")
                    }
                    isMatch -> {
                        updateNotification("IP hiện tại: $ip (khớp) ✅")
                    }
                    else -> {
                        val hasAssistant = IpVoiceInteractionService.isCurrentlyActiveAssistant(this@IpCheckService)
                        if (!hasAssistant) {
                            updateNotification("IP: $ip (không khớp) – Chưa đặt app làm Trợ lý mặc định, mở app để cấp quyền.")
                            delay(Prefs.getCheckIntervalSec(this@IpCheckService) * 1000L)
                            continue
                        }
                        updateNotification("IP: $ip (không khớp) → đang bật Airplane Mode…")
                        val sentOn = IpVoiceInteractionService.requestAirplaneToggle(this@IpCheckService, true)
                        android.util.Log.d("IpWatcher", "requestAirplaneToggle(true) returned $sentOn")
                        delay(Prefs.getToggleOnMs(this@IpCheckService))

                        updateNotification("Đang tắt Airplane Mode…")
                        val sentOff = IpVoiceInteractionService.requestAirplaneToggle(this@IpCheckService, false)
                        android.util.Log.d("IpWatcher", "requestAirplaneToggle(false) returned $sentOff")

                        updateNotification("Đã toggle, đang chờ mạng kết nối lại…")
                        delay(Prefs.getReconnectWaitMs(this@IpCheckService))
                        continue
                    }
                }

                delay(Prefs.getCheckIntervalSec(this@IpCheckService) * 1000L)
            }
        }
    }

    private fun fetchPublicIp(): String? {
        val urls = listOf(
            "https://api.ipify.org",
            "https://ifconfig.me/ip",
            "https://icanhazip.com"
        )
        for (u in urls) {
            try {
                val url = URL(u)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.requestMethod = "GET"
                val code = conn.responseCode
                if (code == 200) {
                    val result = conn.inputStream.bufferedReader().readText().trim()
                    android.util.Log.d("IpWatcher", "fetchPublicIp OK via $u -> $result")
                    return result
                } else {
                    android.util.Log.w("IpWatcher", "fetchPublicIp $u trả về mã $code")
                }
            } catch (e: Exception) {
                android.util.Log.e("IpWatcher", "fetchPublicIp $u lỗi: ${e.message}")
            }
        }
        return null
    }

    private fun stopSelfService() {
        job?.cancel()
        Prefs.setRunning(this, false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        job?.cancel()
        Prefs.setRunning(this, false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "IP Watcher", NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val stopIntent = Intent(this, IpCheckService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("IP Watcher đang chạy")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .addAction(0, "Dừng", stopPending)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(text))
    }
}
