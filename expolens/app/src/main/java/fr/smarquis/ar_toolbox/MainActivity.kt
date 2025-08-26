package fr.smarquis.ar_toolbox

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import fr.smarquis.ar_toolbox.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide action bar for clean look
        supportActionBar?.hide()

        setupButtons()
    }

    private fun setupButtons() {
        binding.btnAnchor.setOnClickListener {
            // Navigate to location loading screen for Anchor mode
            val intent = Intent(this, LocationLoadingActivity::class.java)
            intent.putExtra("mode", "anchor")
            startActivity(intent)
            finish()
        }

        binding.btnViewer.setOnClickListener {
            // Viewer mode goes directly to AR (no location detection needed)
            val intent = Intent(this, SceneActivity::class.java)
            intent.putExtra("mode", "viewer")
            startActivity(intent)
            finish()
        }
    }
}