package com.vannstore.admin

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import kotlin.concurrent.thread

class MainActivity : Activity() {
    // For local testing only. Use HTTPS and secure per-device token provisioning in production.
    private val apiBase = "https://YOUR_API_HOST"
    private val apiToken = "REPLACE_WITH_TEST_TOKEN"
    private val deviceId = "android-test-device-001"
    private lateinit var status: TextView
    private val requestLocation = 1001
    private val cameraRequest = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 40)
        }
        status = TextView(this).apply { text = "VannStore Remote Admin\\nPilih tindakan secara sadar." }
        val locationButton = Button(this).apply { text = "Bagikan lokasi perangkat" }
        val cameraButton = Button(this).apply { text = "Buka kamera untuk mengambil foto" }
        layout.addView(status); layout.addView(locationButton); layout.addView(cameraButton)
        setContentView(layout)

        locationButton.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    requestLocation
                )
            } else shareCurrentLocation()
        }
        cameraButton.setOnClickListener {
            // Explicit user action opens the system camera UI; no covert camera access.
            try {
                startActivityForResult(Intent(MediaStore.ACTION_IMAGE_CAPTURE), cameraRequest)
            } catch (_: Exception) {
                Toast.makeText(this, "Aplikasi kamera tidak tersedia.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestLocation) {
            if (grantResults.any { it == PackageManager.PERMISSION_GRANTED }) shareCurrentLocation()
            else status.text = "Izin lokasi tidak diberikan."
        }
    }

    private fun shareCurrentLocation() {
        val client = LocationServices.getFusedLocationProviderClient(this)
        status.text = "Mengambil lokasi setelah izin diberikan…"
        try {
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                .addOnSuccessListener { location: Location? ->
                    if (location == null) {
                        status.text = "Lokasi belum tersedia. Aktifkan layanan lokasi dan coba lagi."
                        return@addOnSuccessListener
                    }
                    sendLocation(location)
                }
                .addOnFailureListener { status.text = "Gagal mengambil lokasi: ${it.message}" }
        } catch (_: SecurityException) {
            status.text = "Izin lokasi belum tersedia."
        }
    }

    private fun sendLocation(location: Location) {
        status.text = "Mengirim lokasi ke server…"
        thread {
            try {
                val payload = JSONObject().apply {
                    put("device_id", deviceId)
                    put("device_name", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                    put("latitude", location.latitude)
                    put("longitude", location.longitude)
                    put("accuracy", location.accuracy.toDouble())
                }
                val conn = (URL("$apiBase/api/location").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10000
                    readTimeout = 10000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $apiToken")
                }
                conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                runOnUiThread {
                    status.text = if (code in 200..299) "Lokasi berhasil dibagikan." else "Server menolak permintaan (HTTP $code)."
                }
                conn.disconnect()
            } catch (e: Exception) {
                runOnUiThread { status.text = "Gagal mengirim lokasi: ${e.message}" }
            }
        }
    }

    @Deprecated("Deprecated in Android, retained for compatibility with the starter")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == cameraRequest && resultCode == RESULT_OK) {
            status.text = "Foto diambil melalui kamera sistem. Upload foto belum diaktifkan pada starter."
        }
    }
}
