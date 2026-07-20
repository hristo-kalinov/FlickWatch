package com.example.flickwatch

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle

class MainActivity : Activity() {

    // Member variable: read-only constant (like 'const int' in C++)
    private val notificationPermissionCode = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // API 33 (Android 13 / Wear OS 4) introduced runtime notification permissions.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                // Already granted, start the service
                startFlickService()
            } else {
                // Request the permission
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    notificationPermissionCode
                )
            }
        } else {
            // Older versions grant notification permission automatically
            startFlickService()
        }
    }

    // This callback is triggered when the user taps "Allow" or "Deny" on the watch screen
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == notificationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // User allowed it, start the service!
                startFlickService()
            } else {
                // User denied it. In a production app, you'd explain why you need it,
                // but for our side project, we'll just close the app.
                finish()
            }
        }
    }

    // Helper function to launch our foreground service
    private fun startFlickService() {
        val intent = Intent(this, FlickService::class.java)
        startForegroundService(intent)
        finish() // Closes the black UI screen immediately, letting the service run in the background
    }
}