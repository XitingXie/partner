package com.example.app.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app.R
import com.example.app.databinding.ActivityConversationBinding
import com.example.app.network.ApiConfig
import com.example.app.network.ApiService
import com.example.app.network.TutorResponse
import com.example.app.network.PartnerResponse
import com.example.app.network.ChatRequest
import com.example.app.data.models.CreateSessionRequest
import com.example.app.data.models.SceneLevel
import kotlinx.coroutines.launch
import org.json.JSONObject

class ConversationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConversationBinding
    private lateinit var apiService: ApiService
    private lateinit var chatAdapter: ChatAdapter

    private var sceneId: Int? = null
    private var topicId: Int? = null
    private var sessionId: String? = null
    private var sceneLevel: SceneLevel? = null
    private val userId = 1
    private val userLevel = "B1"  // Ensure lowercase to match backend
    private var showFeedback = false

    companion object {
        private const val TAG = "ConversationActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(android.R.color.white)
        binding = ActivityConversationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up action bar
        supportActionBar?.apply {
            title = "Conversation"  // We can set a default title or get it from the scene later
        }

        sceneId = intent.getIntExtra("SCENE_ID", -1)
        topicId = intent.getIntExtra("TOPIC_ID", -1)

        if (sceneId == -1 || topicId == -1) {
            showErrorAndExit("Missing required information")
            return
        }

        setupRecyclerView()
        setupUI()
        apiService = ApiConfig.getApiService(this)
        createSession()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        binding.chatRecyclerView.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(this@ConversationActivity)
        }
    }

    private fun setupUI() {
        binding.messageInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                sendMessage()
                true
            } else {
                false
            }
        }

        binding.sendButton.setOnClickListener {
            sendMessage()
        }
        
        binding.tutorButton.setOnClickListener {
            showFeedback = !showFeedback
            binding.tutorButton.alpha = if (showFeedback) 1.0f else 0.5f
            chatAdapter.setShowFeedback(showFeedback)
        }
        
        binding.messageInput.isEnabled = false
    }

    private fun createSession() {
        lifecycleScope.launch {
            try {
                val request = CreateSessionRequest(userId, sceneId!!, topicId!!)
                val response = apiService.createSession(request)
                sessionId = response.id

                runOnUiThread {
                    binding.messageInput.isEnabled = true
                    // Add welcome message to chat
                    chatAdapter.addMessage("Welcome! How can I help you today?", isUser = false)
                    scrollToBottom()
                }
            } catch (e: Exception) {
                showErrorAndExit("Error creating chat session: ${e.message}")
            }
        }
    }

    private fun sendMessage() {
        val message = binding.messageInput.text.toString().trim()
        if (message.isEmpty() || sessionId == null) return

        Log.d(TAG, "Sending message: $message")
        
        chatAdapter.addMessage(message, isUser = true)
        scrollToBottom()
        
        binding.messageInput.text.clear()

        lifecycleScope.launch {
            try {
                // First, check with tutor
                val request = ChatRequest(
                    sessionId = sessionId!!,
                    sceneId = sceneId!!,
                    userId = userId,
                    userInput = message
                )
                
                val tutorResponse = apiService.chatWithTutor(request)
                Log.d(TAG, "Tutor response: $tutorResponse")
                Log.d(TAG, "Needs correction: ${tutorResponse.needsCorrection}")
                Log.d(TAG, "Feedback: ${tutorResponse.feedback}")
                Log.d(TAG, "Tutor message: ${tutorResponse.tutorMessage}")

                if (tutorResponse.needsCorrection) {
                    // Show feedback and let user try again
                    runOnUiThread {
                        val lastUserMessage = chatAdapter.getLastUserMessage()
                        Log.d(TAG, "Last user message: $lastUserMessage")
                        if (lastUserMessage != null) {
                            chatAdapter.updateMessageFeedback(lastUserMessage, tutorResponse.tutorMessage)
                            Log.d(TAG, "Updated message feedback")
                            scrollToBottom()
                        } else {
                            Log.e(TAG, "No last user message found")
                        }
                    }
                    return@launch
                }

                // If no corrections needed, proceed with partner chat
                val partnerResponse = apiService.chatWithPartner(request)  // Reuse the same request
                Log.d(TAG, "Partner response: $partnerResponse")

                runOnUiThread {
                    chatAdapter.addMessage(partnerResponse.message, isUser = false)
                    scrollToBottom()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message", e)
                Log.e(TAG, "Exception: ${e.message}")
                Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
                runOnUiThread {
                    Toast.makeText(this@ConversationActivity, "Network Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun scrollToBottom() {
        binding.chatRecyclerView.post {
            binding.chatRecyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
        }
    }

    private fun showErrorAndExit(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_conversation, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(TAG, "onOptionsItemSelected called with itemId: ${item.itemId}")
        return when (item.itemId) {
            R.id.action_key_phrases -> {
                showSceneLevelData()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadSceneLevel() {
        Log.d(TAG, "loadSceneLevel called")
        lifecycleScope.launch {
            try {
                sceneLevel = apiService.getSceneLevel(sceneId!!, userLevel)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading scene level data", e)
                Toast.makeText(this@ConversationActivity, 
                    "Error loading scene data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSceneLevelData() {
        if (sceneLevel == null) {
            loadSceneLevel()
            return
        }
        Log.d(TAG, "showSceneLevelData called")
        val data = """
            Key Phrases:
            ${sceneLevel!!.keyPhrases ?: "None"}
            
            Vocabulary:
            ${sceneLevel!!.vocabulary ?: "None"}
            
            Grammar Points:
            ${sceneLevel!!.grammarPoints ?: "None"}
            
            Example Dialogs:
            ${sceneLevel!!.exampleDialogs ?: "None"}
        """.trimIndent()

        // For now, just show in a Toast
        Toast.makeText(this, data, Toast.LENGTH_LONG).show()
    }
}