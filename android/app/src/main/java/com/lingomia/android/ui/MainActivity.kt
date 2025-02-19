package com.lingomia.android.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.lingomia.android.databinding.ActivityMainBinding
import com.lingomia.android.network.ApiService
import com.lingomia.android.network.ApiConfig
import com.lingomia.android.ui.adapters.TopicsAdapter
import com.lingomia.android.ui.base.BaseAuthActivity
import kotlinx.coroutines.launch
import com.lingomia.android.R

class MainActivity : BaseAuthActivity() {
    private lateinit var binding: ActivityMainBinding
    private val apiService: ApiService = ApiConfig.apiService
    private lateinit var topicsAdapter: TopicsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()  // Hide the default action bar
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("MainActivity", "onCreate called")
        
        setupBottomNavigation()
        setupRecyclerView()
        loadTopics()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // We're already on the home screen (MainActivity)
                    true
                }
                R.id.navigation_exercise -> {
                    // TODO: Launch Exercise Activity/Fragment
                    Toast.makeText(this, "Exercise coming soon", Toast.LENGTH_SHORT).show()
                    false
                }
                R.id.navigation_profile -> {
                    // TODO: Launch Profile Activity/Fragment
                    Toast.makeText(this, "Profile coming soon", Toast.LENGTH_SHORT).show()
                    false
                }
                R.id.navigation_notifications -> {
                    // TODO: Launch Notifications Activity/Fragment
                    Toast.makeText(this, "Notifications coming soon", Toast.LENGTH_SHORT).show()
                    false
                }
                else -> false
            }
        }
        
        // Set home as selected by default since this is the home screen
        binding.bottomNavigation.selectedItemId = R.id.navigation_home
    }

    private fun setupRecyclerView() {
        Log.d("MainActivity", "Setting up RecyclerView")
        topicsAdapter = TopicsAdapter { topicId: String ->
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
        
        // Get current user ID
        val currentUser = authManager.currentUser
        if (currentUser == null) {
            Log.e("MainActivity", "No user found, redirecting to auth")
            startAuthActivity()
            return
        }

        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "Making API call to get topics for user: ${currentUser.uid}")
                val topics = apiService.getTopics(currentUser.uid)
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
                        if (e.code() == 401) {
                            // Unauthorized - token might be expired
                            startAuthActivity()
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Server error: ${e.code()}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
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