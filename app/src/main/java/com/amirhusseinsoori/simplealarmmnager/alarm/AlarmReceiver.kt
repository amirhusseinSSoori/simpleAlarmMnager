package com.amirhusseinsoori.simplealarmmnager.alarm


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.amirhusseinsoori.simplealarmmnager.AlarmActivity

class AlarmReceiver : BroadcastReceiver() {
override fun onReceive(context: Context, intent: Intent) {

        val message = intent?.getStringExtra("EXTRA_MESSAGE") ?: return
        println("Alarm triggered: $message")
        val wakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(
                PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "MyApp::AlarmWakeLock"
            )
        }
        wakeLock.acquire(60 * 1000L)

        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        context.startActivity(alarmIntent)
        wakeLock.release()
    }
}