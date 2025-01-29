package com.example.app.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app.databinding.ActivityScenesBinding
import com.example.app.network.ApiService
import com.example.app.network.ApiConfig
import com.example.app.ui.adapters.ScenesAdapter
import com.example.app.data.models.Scene
import kotlinx.coroutines.launch

class ScenesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScenesBinding
    private val apiService: ApiService = ApiConfig.apiService
    private lateinit var scenesAdapter: ScenesAdapter
    private var topicId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityScenesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        topicId = intent.getIntExtra("TOPIC_ID", -1)
        if (topicId == -1) {
            Log.e("ScenesActivity", "No topic ID provided")
            finish()
            return
        }

        setupRecyclerView()
        loadScenes()
    }

    private fun setupRecyclerView() {
        Log.d("ScenesActivity", "Setting up RecyclerView")
        scenesAdapter = ScenesAdapter { sceneId ->
            Log.d("ScenesActivity", "Scene clicked: $sceneId")
            val intent = Intent(this, ConversationActivity::class.java)
            intent.putExtra("SCENE_ID", sceneId)
            intent.putExtra("TOPIC_ID", topicId)
            startActivity(intent)
        }

        binding.scenesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ScenesActivity)
            adapter = scenesAdapter
            Log.d("ScenesActivity", "RecyclerView setup complete")
        }
    }

    private fun loadScenes() {
        Log.d("ScenesActivity", "Starting to load scenes for topic $topicId")
        lifecycleScope.launch {
            try {
                val scenes = apiService.getScenes(topicId)
                Log.d("ScenesActivity", "Received ${scenes.size} scenes")
                
                if (scenes.isEmpty()) {
                    Log.w("ScenesActivity", "No scenes available for topic $topicId")
                    Toast.makeText(
                        this@ScenesActivity,
                        "No scenes available for this topic",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    scenesAdapter.submitList(scenes)
                }
            } catch (e: Exception) {
                Log.e("ScenesActivity", "Error loading scenes", e)
                Toast.makeText(
                    this@ScenesActivity,
                    "Error loading scenes",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}