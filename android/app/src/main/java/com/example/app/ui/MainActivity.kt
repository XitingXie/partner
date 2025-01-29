package com.example.app.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app.databinding.ActivityMainBinding
import com.example.app.network.ApiService
import com.example.app.network.ApiConfig
import com.example.app.ui.adapters.TopicsAdapter
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val apiService: ApiService = ApiConfig.apiService
    private lateinit var topicsAdapter: TopicsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadTopics()
    }

    private fun setupRecyclerView() {
        Log.d("MainActivity", "Setting up RecyclerView")
        topicsAdapter = TopicsAdapter { topicId: Int ->
            Log.d("MainActivity", "Topic clicked: $topicId")
            val intent = Intent(this, ScenesActivity::class.java)
            intent.putExtra("TOPIC_ID", topicId)
            startActivity(intent)
        }

        binding.topicsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = topicsAdapter
            Log.d("MainActivity", "RecyclerView setup complete")
        }
    }

    private fun loadTopics() {
        Log.d("MainActivity", "Starting to load topics...")
        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "Making API call to get topics")
                val topics = apiService.getTopics()
                Log.d("MainActivity", "Raw topics response: $topics")
                
                if (topics.isEmpty()) {
                    Log.w("MainActivity", "Received empty topics list")
                    Toast.makeText(
                        this@MainActivity,
                        "No topics available",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Log.d("MainActivity", "Submitting ${topics.size} topics to adapter")
                    topics.forEach { topic ->
                        Log.d("MainActivity", "Topic: id=${topic.id}, name=${topic.name}, description=${topic.description}")
                    }
                    topicsAdapter.submitList(topics)
                    Log.d("MainActivity", "Topics submitted to adapter")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error loading topics", e)
                when (e) {
                    is retrofit2.HttpException -> {
                        val errorBody = e.response()?.errorBody()?.string()
                        Log.e("MainActivity", "HTTP ${e.code()}: $errorBody")
                        Toast.makeText(
                            this@MainActivity,
                            "Server error: ${e.code()}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    is java.net.UnknownHostException -> {
                        Log.e("MainActivity", "Network error: ${e.message}")
                        Toast.makeText(
                            this@MainActivity,
                            "Network error: Cannot reach server",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    is com.google.gson.JsonSyntaxException -> {
                        Log.e("MainActivity", "JSON parsing error: ${e.message}")
                        Toast.makeText(
                            this@MainActivity,
                            "Error parsing server response",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    else -> {
                        Log.e("MainActivity", "Unknown error: ${e.message}")
                        Toast.makeText(
                            this@MainActivity,
                            "Error: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                e.printStackTrace()
            }
        }
    }
} 