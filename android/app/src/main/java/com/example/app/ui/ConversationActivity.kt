package com.example.app.ui

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import androidx.lifecycle.lifecycleScope
import com.example.app.databinding.ActivityConversationBinding
import com.example.app.network.ApiService
import com.example.app.network.ApiConfig
import com.example.app.network.CreateSessionRequest
import com.example.app.ui.adapters.ConversationPagerAdapter
import com.example.app.ui.fragments.ChatFragment
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class ConversationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConversationBinding
    private val apiService: ApiService = ApiConfig.apiService
    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sceneId = intent.getStringExtra("SCENE_ID") ?: run {
            Log.e("ConversationActivity", "No scene ID provided")
            Toast.makeText(this, "Error: No scene ID provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Set fixed user ID
        val userId = "1"  // Changed from "user123" to "1"

        lifecycleScope.launch {
            try {
                // Create session first
                val session = apiService.createSession(
                    CreateSessionRequest(
                        person_id = userId,
                        scene_id = sceneId
                    )
                )
                
                Log.d("ConversationActivity", "Session created: ${session.id}")

                // Then initialize chat with session ID
                setupViewPager()
                setupTabLayout()
                
                viewPager.post {
                    val chatFragment = supportFragmentManager.findFragmentByTag("f${viewPager.currentItem}") as? ChatFragment
                    if (chatFragment != null) {
                        Log.d("ConversationActivity", "Found ChatFragment, initializing session")
                        chatFragment.onSessionCreated(session.id, sceneId, userId)
                    } else {
                        Log.e("ConversationActivity", "ChatFragment not found")
                        Toast.makeText(this@ConversationActivity, "Error: Could not initialize chat", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("ConversationActivity", "Error creating session", e)
                Toast.makeText(this@ConversationActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun setupViewPager() {
        Log.d("ConversationActivity", "Setting up ViewPager")
        viewPager = binding.viewPager
        val adapter = ConversationPagerAdapter(this)
        viewPager.adapter = adapter
        Log.d("ConversationActivity", "ViewPager setup complete")
    }

    private fun setupTabLayout() {
        Log.d("ConversationActivity", "Setting up TabLayout")
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Chat"
                1 -> "Learning"
                else -> null
            }
        }.attach()
        Log.d("ConversationActivity", "TabLayout setup complete")
    }
} 