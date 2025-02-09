package com.lingomia.android.ui

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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.lingomia.android.R
import com.lingomia.android.databinding.ActivityConversationBinding
import com.lingomia.android.databinding.BottomSheetSceneInfoBinding
import com.lingomia.android.network.ApiService
import com.lingomia.android.network.ApiConfig
import com.lingomia.android.network.TutorResponse
import com.lingomia.android.network.PartnerResponse
import com.lingomia.android.network.ChatRequest
import com.lingomia.android.data.models.CreateSessionRequest
import com.lingomia.android.data.models.SceneLevel
import com.lingomia.android.ui.base.BaseAuthActivity
import kotlinx.coroutines.launch
import org.json.JSONObject
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.widget.TextView
import android.speech.tts.TextToSpeech
import kotlin.system.exitProcess
import java.util.Locale

class ConversationActivity : BaseAuthActivity() {
    private lateinit var binding: ActivityConversationBinding
    private lateinit var apiService: ApiService
    private lateinit var chatAdapter: ChatAdapter
    private var bottomSheetDialog: BottomSheetDialog? = null

    private var sceneId: String? = null
    private var topicId: String? = null
    private var sessionId: String? = null
    private var sceneLevel: SceneLevel? = null
    private val PREFS_NAME = "AppPrefs"
    private lateinit var userId: String
    private val userLevel = "B1"  // Ensure lowercase to match backend
    private var showFeedback = false
    private var userFirstLanguage: String? = null

    private lateinit var textToSpeech: TextToSpeech

    companion object {
        private const val TAG = "ConversationActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(android.R.color.white)
        binding = ActivityConversationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize userId and userFirstLanguage from SharedPreferences
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        userId = authManager.currentUser?.uid
            ?: throw IllegalStateException("User ID not found. User must sign in first.")
        userFirstLanguage = prefs.getString("selectedLanguage", null)
        Log.d(TAG, "User's first language: $userFirstLanguage")

        // Set up action bar
        supportActionBar?.apply {
            title = "Conversation"  // We can set a default title or get it from the scene later
        }

        sceneId = intent.getStringExtra("SCENE_ID")
        topicId = intent.getStringExtra("TOPIC_ID")

        if (sceneId == null || topicId == null) {
            showErrorAndExit("Missing required information")
            return
        }
        apiService = ApiConfig.apiService
        loadSceneLevel()

        setupRecyclerView()
        setupUI()

        createSession()

        textToSpeech = TextToSpeech(this) { status ->
            if (status != TextToSpeech.SUCCESS) {
                Log.e(TAG, "Text to Speech Failed")
                exitProcess(-1)
            }
        }
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
                val request = CreateSessionRequest(
                    topicId = topicId!!,
                    sceneId = sceneId!!,
                    userId = userId!!  // Safe to use !! here since we check for null in initialization
                )
                val response = apiService.createSession(request)
                sessionId = response.id

                runOnUiThread {
                    binding.messageInput.isEnabled = true
                    scrollToBottom()
                }
            } catch (e: Exception) {
                showErrorAndExit("Error creating chat session: ${e.message}")
            }
        }
    }

    private fun speak(text: String) {
        if (::textToSpeech.isInitialized) {
            // Speak the text
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    override fun onDestroy() {
        // Shutdown TextToSpeech to release resources
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroy()
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
                val tutorRequest = ChatRequest(
                    sessionId = sessionId!!,
                    sceneId = sceneId!!,
                    userId = userId!!,
                    userInput = message,
                    first_language = userFirstLanguage ?: "zh" // Default to Chinese if not set
                )
                
                val tutorResponse = apiService.chatWithTutor(tutorRequest)
                Log.d(TAG, "Tutor response: $tutorResponse")
                Log.d(TAG, "Needs correction: ${tutorResponse.needsCorrection}")
                Log.d(TAG, "Feedback: ${tutorResponse.feedback}")
                Log.d(TAG, "Tutor message: ${tutorResponse.tutorMessage}")

                if (tutorResponse.needsCorrection) {
                    Log.d(TAG, "Tutor indicates correction needed, showing feedback")
                    // Show feedback and let user try again
                    runOnUiThread {
                        val lastUserMessage = chatAdapter.getLastUserMessage()
                        Log.d(TAG, "Last user message: $lastUserMessage")
                        if (lastUserMessage != null) {
                            chatAdapter.updateMessageFeedback(lastUserMessage, tutorResponse.tutorMessage)
                            
                            // Set TTS language based on user's first language
                            when (userFirstLanguage) {
                                "zh" -> textToSpeech.setLanguage(Locale.CHINESE)
                                "es" -> textToSpeech.setLanguage(Locale("es"))
                                "pt" -> textToSpeech.setLanguage(Locale("pt"))
                                "de" -> textToSpeech.setLanguage(Locale.GERMAN)
                                "fr" -> textToSpeech.setLanguage(Locale.FRENCH)
                                "ar" -> textToSpeech.setLanguage(Locale("ar"))
                                "ja" -> textToSpeech.setLanguage(Locale.JAPANESE)
                                "ko" -> textToSpeech.setLanguage(Locale.KOREAN)
                                else -> textToSpeech.setLanguage(Locale.CHINESE) // Default to Chinese
                            }

                            speak(tutorResponse.tutorMessage)
                            Log.d(TAG, "Updated message feedback")
                            scrollToBottom()
                        } else {
                            Log.e(TAG, "No last user message found")
                        }
                    }
                    return@launch
                }

                Log.d(TAG, "No correction needed, proceeding with partner chat")
                // If no corrections needed, proceed with partner chat
                val partnerRequest = ChatRequest(
                    sessionId = sessionId!!,
                    sceneId = sceneId!!,
                    userId = userId!!,
                    userInput = message
                )
                Log.d(TAG, "Sending partner request: $partnerRequest")
                val partnerResponse = apiService.chatWithPartner(partnerRequest)
                Log.d(TAG, "Partner response: $partnerResponse")

                runOnUiThread {
                    textToSpeech.setLanguage(Locale.US)
                    speak(partnerResponse.message)
                    chatAdapter.addMessage(partnerResponse.message, isUser = false)
                    scrollToBottom()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message", e)
                Log.e(TAG, "Exception: ${e.message}")
                Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
                runOnUiThread {
                    // Only show a toast, don't finish the activity
                    Toast.makeText(this@ConversationActivity, "Network Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun scrollToBottom() {
        binding.chatRecyclerView.post {
            val lastPosition = chatAdapter.itemCount - 1
            if (lastPosition >= 0) {
                binding.chatRecyclerView.smoothScrollToPosition(lastPosition)
            }
        }
    }

    private fun showErrorAndExit(message: String) {
        Log.e(TAG, "showErrorAndExit called with message: $message")
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
                    "Error loading scene level data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSceneLevelData() {
        
//        if (sceneLevel == null) {
//            Log.d(TAG, "showSceneLevelData sceneLevel == null")
//            loadSceneLevel()
//            return
//        }
        Log.d(TAG, "showSceneLevelData called")

        if (bottomSheetDialog == null) {
            bottomSheetDialog = BottomSheetDialog(this)
            val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_scene_info, null)
            bottomSheetDialog?.setContentView(bottomSheetView)

            // Set the data
            bottomSheetView.findViewById<TextView>(R.id.tvKeyPhrases).text = sceneLevel!!.keyPhrases ?: "None"
            bottomSheetView.findViewById<TextView>(R.id.tvVocabulary).text = sceneLevel!!.vocabulary ?: "None"
            bottomSheetView.findViewById<TextView>(R.id.tvGrammarPoints).text = sceneLevel!!.grammarPoints ?: "None"
            bottomSheetView.findViewById<TextView>(R.id.tvExampleDialogs).text = sceneLevel!!.exampleDialogs ?: "None"
        }

        bottomSheetDialog?.show()
    }
}