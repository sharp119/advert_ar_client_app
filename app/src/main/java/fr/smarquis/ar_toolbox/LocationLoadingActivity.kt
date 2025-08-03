package fr.smarquis.ar_toolbox

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import fr.smarquis.ar_toolbox.databinding.ActivityLocationLoadingBinding

class LocationLoadingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLocationLoadingBinding
    private lateinit var locationManager: LocationManager
    private var mode: String = "anchor"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationLoadingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide action bar for clean look
        supportActionBar?.hide()

        // Get mode from intent
        mode = intent.getStringExtra("mode") ?: "anchor"

        // Initialize location manager
        locationManager = LocationManager(this)

        // Start location detection
        startLocationDetection()
    }

    private fun startLocationDetection() {
        binding.statusText.text = "Detecting location..."
        binding.venueText.visibility = View.GONE

        locationManager.detectLocation { location, venueName ->
            runOnUiThread {
                onLocationDetected(location, venueName)
            }
        }
    }

    private fun onLocationDetected(location: Location, venueName: String) {
        // Update UI to show venue detected
        binding.statusText.text = "Location detected!"
        binding.venueText.text = "$venueName detected"
        binding.venueText.visibility = View.VISIBLE

        // Wait a moment to show the venue, then proceed to AR
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            proceedToAR(location, venueName)
        }, 1500) // 1.5 seconds to show venue detection
    }

    private fun proceedToAR(location: Location, venueName: String) {
        val intent = Intent(this, SceneActivity::class.java).apply {
            putExtra("mode", mode)
            putExtra("venue_name", venueName)
            putExtra("latitude", location.latitude)
            putExtra("longitude", location.longitude)
            putExtra("accuracy", location.accuracy)
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager.stopLocationUpdates()
    }
}