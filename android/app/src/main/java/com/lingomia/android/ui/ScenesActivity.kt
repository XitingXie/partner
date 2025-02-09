package com.lingomia.android.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.lingomia.android.databinding.ActivityScenesBinding
import com.lingomia.android.network.ApiService
import com.lingomia.android.network.ApiConfig
import com.lingomia.android.ui.adapters.ScenesAdapter
import com.lingomia.android.data.models.Scene
import com.lingomia.android.ui.base.BaseAuthActivity
import kotlinx.coroutines.launch

class ScenesActivity : BaseAuthActivity() {
    private lateinit var binding: ActivityScenesBinding
    private val apiService: ApiService = ApiConfig.apiService
    private lateinit var scenesAdapter: ScenesAdapter
    private var topicId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScenesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up action bar
        supportActionBar?.apply {
            title = "Choose a Scene"
        }

        topicId = intent.getStringExtra("TOPIC_ID")
        if (topicId == null) {
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
                val scenes = apiService.getScenes(topicId!!)
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
                    "Error loading scenes: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}