package com.amirhusseinsoori.simplealarmmnager

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.amirhusseinsoori.simplealarmmnager.alarm.AlarmItem
import com.amirhusseinsoori.simplealarmmnager.alarm.AndroidAlarmScheduler
import com.amirhusseinsoori.simplealarmmnager.ui.theme.SimpleAlarmMnagerTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

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
            var selectedDateTime by remember { mutableStateOf(LocalDateTime.now().plusMinutes(1)) }
            var message by remember { mutableStateOf("") }

            SimpleAlarmMnagerTheme {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Set Alarm Date & Time",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // Date Selection
                            TextButton(
                                onClick = { showDatePicker(selectedDateTime) { newDate ->
                                    selectedDateTime = selectedDateTime.withYear(newDate.year)
                                        .withMonth(newDate.monthValue)
                                        .withDayOfMonth(newDate.dayOfMonth)
                                } },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "📅 Date: ${selectedDateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                                    fontSize = 16.sp
                                )
                            }

                            // Time Selection
                            TextButton(
                                onClick = { showTimePicker(selectedDateTime) { newTime ->
                                    selectedDateTime = selectedDateTime.withHour(newTime.hour)
                                        .withMinute(newTime.minute)
                                        .withSecond(0)
                                } },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "🕐 Time: ${selectedDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }

                    // Message Input
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        placeholder = { Text(text = "Alarm Message (optional)") }
                    )

                    // Current selected date/time display
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Text(
                            text = "⏰ Alarm will trigger at:\n${selectedDateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))}",
                            modifier = Modifier.padding(16.dp),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    // Check if selected time is in the future
                                    if (selectedDateTime.isBefore(LocalDateTime.now())) {
                                        Toast.makeText(this@MainActivity, "Please select a future date and time", Toast.LENGTH_LONG).show()
                                        return@Button
                                    }

                                    alarmItem = AlarmItem(
                                        time = selectedDateTime,
                                        message = message.ifEmpty { "Alarm!" }
                                    )
                                    alarmItem?.let(scheduler::schedule)
                                    message = ""
                                    Toast.makeText(this@MainActivity, "Alarm scheduled for ${selectedDateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))}!", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error scheduling alarm: ${e.message}")
                                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Schedule Alarm")
                        }
                        Button(
                            onClick = {
                                try {
                                    alarmItem?.let(scheduler::cancel)
                                    alarmItem = null
                                    Toast.makeText(this@MainActivity, "Alarm cancelled!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error cancelling alarm: ${e.message}")
                                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Cancel Alarm")
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

    private fun showDatePicker(currentDateTime: LocalDateTime, onDateSelected: (LocalDateTime) -> Unit) {
        val calendar = Calendar.getInstance()
        calendar.set(currentDateTime.year, currentDateTime.monthValue - 1, currentDateTime.dayOfMonth)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = LocalDateTime.of(year, month + 1, dayOfMonth, 0, 0)
                onDateSelected(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set minimum date to today
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun showTimePicker(currentDateTime: LocalDateTime, onTimeSelected: (LocalDateTime) -> Unit) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, currentDateTime.hour)
        calendar.set(Calendar.MINUTE, currentDateTime.minute)

        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val selectedTime = LocalDateTime.of(2000, 1, 1, hourOfDay, minute)
                onTimeSelected(selectedTime)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true // 24-hour format
        )
        timePickerDialog.show()
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