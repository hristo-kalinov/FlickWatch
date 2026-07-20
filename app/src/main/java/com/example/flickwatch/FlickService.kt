package com.example.flickwatch

import android.content.pm.ServiceInfo
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log

class FlickService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var gyroscope: Sensor? = null
    private lateinit var vibrator: Vibrator

    private val channelId = "FlickServiceChannel"
    private val notificationId = 1

    var firstPartOfFlickTimeNanos: Long = 0L

    override fun onCreate() {
        super.onCreate()

        // 1. Create Notification Channel & start Foreground Service (Mandatory!)
        createNotificationChannel()
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("FlickWatch Active")
            .setContentText("Monitoring wrist gestures...")
            .setSmallIcon(android.R.drawable.stat_notify_sync) // Uses a default Android sync icon
            .build()

        startForeground(
            notificationId,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        )

        // 2. Initialize the Sensor Manager and Gyroscope
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        // 3. Initialize the Vibrator Service
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        // 4. Register listener with 1-second batching
        gyroscope?.let { sensor ->
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_GAME, // ~50Hz sampling
                1000000                          // 1 second batch latency in microseconds
            )
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            val x = event.values[0]

            // 1. Detect the initial negative swing
            if (x <= -15) {
                firstPartOfFlickTimeNanos = event.timestamp
            }
            // 2. Detect the positive snap-back within 300ms (300,000,000 nanoseconds)
            else if (x >= 15 && firstPartOfFlickTimeNanos != 0L) {
                val elapsedNanos = event.timestamp - firstPartOfFlickTimeNanos

                if (elapsedNanos < 300000000L) { // 300ms in nanoseconds
                    Log.d("FlickSensor", "Flicked!")

                    // Trigger vibration when a flick is successfully detected
                    triggerVibration(150) // Vibrates for 150 milliseconds

                    firstPartOfFlickTimeNanos = 0L // Reset the state machine
                } else {
                    // Too slow! It took longer than 300ms, so reset and ignore
                    firstPartOfFlickTimeNanos = 0L
                }
            }
        }
    }

    /**
     * Triggers a short vibration.
     * @param durationMillis Duration of the vibration in milliseconds.
     */
    private fun triggerVibration(durationMillis: Long) {
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // API 26 and above use VibrationEffect
                val effect = VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(effect)
            } else {
                // Legacy support for older devices
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMillis)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used, but required to override by the interface
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cleanup: ALWAYS unregister sensor listeners to prevent battery drain leaks
        sensorManager.unregisterListener(this)
    }

    override fun onBind(intent: Intent): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            channelId,
            "Flick Service Channel",
            NotificationManager.IMPORTANCE_LOW // Low priority, no annoying sound
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}