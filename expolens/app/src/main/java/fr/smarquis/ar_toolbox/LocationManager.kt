package fr.smarquis.ar_toolbox

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager as AndroidLocationManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class LocationManager(private val context: Context) {
    
    private var locationManager: AndroidLocationManager? = null
    private var locationListener: LocationListener? = null
    private var onLocationDetected: ((Location, String) -> Unit)? = null
    
    fun detectLocation(callback: (Location, String) -> Unit) {
        onLocationDetected = callback
        
        if (!hasLocationPermissions()) {
            // For now, simulate location detection if permissions not granted
            simulateLocationDetection()
            return
        }
        
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as AndroidLocationManager
        
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val venueName = determineVenue(location)
                callback(location, venueName)
                stopLocationUpdates()
            }
            
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        
        try {
            // Try GPS first, fallback to network
            when {
                locationManager?.isProviderEnabled(AndroidLocationManager.GPS_PROVIDER) == true -> {
                    locationManager?.requestLocationUpdates(
                        AndroidLocationManager.GPS_PROVIDER,
                        0L,
                        0f,
                        locationListener!!
                    )
                }
                locationManager?.isProviderEnabled(AndroidLocationManager.NETWORK_PROVIDER) == true -> {
                    locationManager?.requestLocationUpdates(
                        AndroidLocationManager.NETWORK_PROVIDER,
                        0L,
                        0f,
                        locationListener!!
                    )
                }
                else -> {
                    // No providers available, simulate
                    simulateLocationDetection()
                }
            }
        } catch (e: SecurityException) {
            // Permissions not granted, simulate
            simulateLocationDetection()
        }
    }
    
    private fun simulateLocationDetection() {
        // Simulate venue1 location for demo purposes
        val simulatedLocation = Location("simulated").apply {
            latitude = 37.7749  // San Francisco coordinates as example
            longitude = -122.4194
            accuracy = 10f
        }
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            onLocationDetected?.invoke(simulatedLocation, "venue1")
        }, 2000) // 2-second loading simulation
    }
    
    private fun determineVenue(location: Location): String {
        // Simple venue detection logic - in real app this would use a venue database
        // For demo, always return venue1
        return "venue1"
    }
    
    private fun hasLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun stopLocationUpdates() {
        locationListener?.let { listener ->
            try {
                locationManager?.removeUpdates(listener)
            } catch (e: SecurityException) {
                // Ignore
            }
        }
        locationManager = null
        locationListener = null
    }
}