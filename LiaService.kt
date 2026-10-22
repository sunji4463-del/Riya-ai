package com.lia.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.lia.assistant.ui.VoiceState
import com.lia.assistant.ui.VoiceViewModel

class LiaService : Service() {
    private var wake: PowerManager.WakeLock? = null
    private val voice: VoiceViewModel get() = (application as LiaApplication).voiceVm

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val ch = NotificationChannel(CHANNEL, "Lia voice", NotificationManager.IMPORTANCE_LOW)
        ch.description = "Shown while Lia listens in the background"
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                voice.stop()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_MUTE -> voice.toggleMute()
        }

        try {
            val n = buildNotification()
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            } else {
                startForeground(NOTIF_ID, n)
            }
        } catch (e: Exception) {
            voice.info = "Background service চালু করা যায়নি: ${e.message}"
            stopSelf()
            return START_NOT_STICKY
        }

        if (wake == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wake = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "lia:voice").apply {
                setReferenceCounted(false)
                acquire()
            }
        }
        voice.onChange = { refresh() }
        return START_NOT_STICKY
    }

    private fun refresh() {
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val v = voice
        val title = when {
            v.muted -> "Lia - muted"
            v.state == VoiceState.LISTENING -> "Lia is listening..."
            v.state == VoiceState.THINKING -> "Lia is thinking..."
            v.state == VoiceState.SPEAKING -> "Lia is speaking..."
            else -> "Lia is ready"
        }
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            flags
        )
        val mute = PendingIntent.getService(
            this, 1, Intent(this, LiaService::class.java).setAction(ACTION_MUTE), flags
        )
        val stop = PendingIntent.getService(
            this, 2, Intent(this, LiaService::class.java).setAction(ACTION_STOP), flags
        )

        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(title)
            .setContentText("Voice is active in background. Tap to open.")
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(0, if (v.muted) "Unmute" else "Mute", mute)
            .addAction(0, "Stop", stop)
            .build()
    }

    override fun onDestroy() {
        voice.onChange = null
        wake?.let { if (it.isHeld) it.release() }
        wake = null
        if (voice.active) voice.stop()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL = "lia_voice"
        const val NOTIF_ID = 42
        const val ACTION_MUTE = "com.lia.assistant.MUTE"
        const val ACTION_STOP = "com.lia.assistant.STOP"

        fun start(ctx: Context) {
            ContextCompat.startForegroundService(ctx, Intent(ctx, LiaService::class.java))
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, LiaService::class.java))
        }
    }
}
