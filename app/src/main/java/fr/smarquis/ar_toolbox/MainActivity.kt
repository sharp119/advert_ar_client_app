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
            // Navigate to AR Scene (current main functionality)
            val intent = Intent(this, SceneActivity::class.java)
            intent.putExtra("mode", "anchor")
            startActivity(intent)
            finish()
        }

        binding.btnViewer.setOnClickListener {
            // For now, also navigate to SceneActivity but with viewer mode
            // In future, this could navigate to a different activity
            val intent = Intent(this, SceneActivity::class.java)
            intent.putExtra("mode", "viewer")
            startActivity(intent)
            finish()
        }
    }
}