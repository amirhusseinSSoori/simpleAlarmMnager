package com.amirhusseinsoori.simplealarmmnager

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.amirhusseinsoori.simplealarmmnager.alarm.AlarmItem
import com.amirhusseinsoori.simplealarmmnager.alarm.AndroidAlarmScheduler
import com.amirhusseinsoori.simplealarmmnager.ui.theme.SimpleAlarmMnagerTheme
import java.time.LocalDateTime

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_BATTERY_OPTIMIZATION = 1001
    }
    
    private lateinit var scheduler: AndroidAlarmScheduler
    private var alarmItem: AlarmItem? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        scheduler = AndroidAlarmScheduler(this)
        
        // Request permissions
        requestPermissions()
        
        // Check battery optimization
        checkBatteryOptimization()
        
        setContent {
            var secondsText by remember { mutableStateOf("") }
            var message by remember { mutableStateOf("") }
            
            SimpleAlarmMnagerTheme {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    OutlinedTextField(
                        value = secondsText,
                        onValueChange = { secondsText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(text = "Trigger alarm in seconds") }
                    )
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(text = "Message") }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(onClick = {
                            try {
                                alarmItem = AlarmItem(
                                    time = LocalDateTime.now().plusSeconds(secondsText.toLong()),
                                    message = message
                                )
                                alarmItem?.let(scheduler::schedule)
                                secondsText = ""
                                message = ""
                                Toast.makeText(this@MainActivity, "Alarm scheduled!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Log.e(TAG, "Error scheduling alarm: ${e.message}")
                                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }) {
                            Text(text = "Schedule")
                        }
                        Button(onClick = {
                            try {
                                alarmItem?.let(scheduler::cancel)
                                alarmItem = null
                                Toast.makeText(this@MainActivity, "Alarm cancelled!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Log.e(TAG, "Error cancelling alarm: ${e.message}")
                                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }) {
                            Text(text = "Cancel")
                        }
                    }
                    Button(
                        onClick = { checkBatteryOptimization() },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text("Check Battery Optimization")
                    }
                    Button(
                        onClick = { requestNotificationPermission() },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Request Notification Permission")
                    }
                }
            }
        }
    }
    
    private fun requestPermissions() {
        val permissions = mutableListOf<String>().apply {
            add(Manifest.permission.WAKE_LOCK)
            add(Manifest.permission.POST_NOTIFICATIONS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.SCHEDULE_EXACT_ALARM)
            }
        }
        
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 1000)
        }
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val requestPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    Toast.makeText(this, "Notification permission granted!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Notification permission denied. Alarms may not work properly.", Toast.LENGTH_LONG).show()
                }
            }
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    
    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                try {
                    startActivityForResult(intent, REQUEST_CODE_BATTERY_OPTIMIZATION)
                } catch (e: Exception) {
                    Log.e(TAG, "Could not open battery optimization settings: ${e.message}")
                    Toast.makeText(this, "Please disable battery optimization for this app manually", Toast.LENGTH_LONG).show()
                }
            } else {
                Log.d(TAG, "Battery optimization is already disabled")
            }
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_BATTERY_OPTIMIZATION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val powerManager = getSystemService(POWER_SERVICE) as PowerManager
                if (powerManager.isIgnoringBatteryOptimizations(packageName)) {
                    Toast.makeText(this, "Battery optimization disabled!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Please disable battery optimization for alarms to work properly", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}


