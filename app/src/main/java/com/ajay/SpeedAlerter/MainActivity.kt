package com.ajay.SpeedAlerter

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Looper
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class MainActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private val locationPermissionCode = 1
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var speedLimitKmph: Int = 0
    private lateinit var speedLimitTextView: TextView
    private lateinit var setLimitButton: Button
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var alarmSwitch: Switch
    private lateinit var alertTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        speedLimitTextView = findViewById(R.id.editTextSpeedLimit)
        setLimitButton = findViewById(R.id.buttonSetLimit)
        alarmSwitch = findViewById(R.id.switchAlarm)
        alertTextView = findViewById(R.id.textViewAlert)

        mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (!isLocationPermissionGranted()) {
            requestLocationPermission()
        } else {
            startLocationUpdates()
        }

        setLimitButton.setOnClickListener {
            if (!isLocationPermissionGranted()) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    locationPermissionCode
                )
            } else {
                val speedLimitText = speedLimitTextView.text.toString()
                speedLimitKmph = if (speedLimitText.isNotEmpty()) speedLimitText.toInt() else 0
                Toast.makeText(this, "Speed limit set to $speedLimitKmph km/h", Toast.LENGTH_SHORT).show()
            }

        }

        alarmSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isLocationPermissionGranted()) {
                requestLocationPermission()
            } else {
                if (isChecked) {
                    alertTextView.text = "Alarm Enabled"
                } else {
                    alertTextView.text = ""
                    stopAlarm()
                }
            }}

    }

    // Add other necessary methods as per your application logic

    private fun isLocationPermissionGranted(): Boolean {
        return (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.popup_layout)

            val buttonAskAgain = dialog.findViewById<Button>(R.id.buttonAskAgain)
            val buttonCloseApp = dialog.findViewById<Button>(R.id.buttonCloseApp)

            buttonAskAgain.setOnClickListener {
                dialog.dismiss()
                // Ask for location permission again
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    locationPermissionCode
                )
            }

            buttonCloseApp.setOnClickListener {
                dialog.dismiss()
                finish() // Close the activity
            }

            dialog.show()
        } else {
            // Request the permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationPermissionCode
            )
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
            return
        }
        val locationFastestInterval = 1
        val locationMaxWaitTime = 2
        val locationInterval = 3
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, locationInterval.toLong())
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(locationFastestInterval.toLong())
            .setMaxUpdateDelayMillis(locationMaxWaitTime.toLong())
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    val speed = location.speed
                    updateSpeedometer(speed)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
            // Handle the exception gracefully here
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
            } else {
                // Handle the case where the user denies the permission
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateSpeedometer(speed: Float) {
        val speedTextView: TextView = findViewById(R.id.textViewSpeedometer)
        val speedInKmph = (speed * 3.6).toInt()
        val speedInMps = speed.toInt()

        val speedText = "Current Speed: $speedInMps m/s ($speedInKmph km/h)"
        speedTextView.text = speedText
        if (speedInKmph > speedLimitKmph && alarmSwitch.isChecked) {
            playAlarm() // Play the alarm if the speed exceeds the limit and the alarm switch is enabled
        } else {
            stopAlarm()
        }
    }

    private fun playAlarm() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.isLooping = true // Set the alarm to loop
            mediaPlayer.start() // Start playing the alarm sound
        }
    }

    private fun stopAlarm() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.isLooping = false // Disable looping
            mediaPlayer.stop() // Stop playing the alarm sound
            mediaPlayer.prepare() // Prepare the MediaPlayer for the next playback
        }
    }


}
