package com.amirhusseinsoori.simplealarmmnager.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.time.ZoneId

class AndroidAlarmScheduler(
    private val context: Context
): AlarmScheduler {

    companion object {
        private const val TAG = "AndroidAlarmScheduler"
    }

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(item: AlarmItem) {
        Log.d(TAG, "Scheduling alarm: ${item.message} at ${item.time}")
        
        // Check if exact alarms are allowed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e(TAG, "Exact alarms are not allowed")
                throw SecurityException("Exact alarms are not allowed. Please grant SCHEDULE_EXACT_ALARM permission.")
            }
        }
        
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("EXTRA_MESSAGE", item.message)
        }
        
        try {
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                item.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                item.time.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000,
                pendingIntent
            )
            
            Log.d(TAG, "Alarm scheduled successfully")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to schedule alarm: ${e.message}")
            throw SecurityException("Failed to schedule alarm: ${e.message}")
        }
    }

    override fun cancel(item: AlarmItem) {
        Log.d(TAG, "Cancelling alarm: ${item.message}")
        
        try {
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                item.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Alarm cancelled successfully")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to cancel alarm: ${e.message}")
            throw SecurityException("Failed to cancel alarm: ${e.message}")
        }
    }
}