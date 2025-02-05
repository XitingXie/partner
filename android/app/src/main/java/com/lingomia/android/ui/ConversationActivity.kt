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
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.lingomia.android.R
import com.lingomia.android.databinding.ActivityConversationBinding
import com.lingomia.android.databinding.BottomSheetSceneInfoBinding
import com.lingomia.android.network.ApiConfig
import com.lingomia.android.network.ApiService
import com.lingomia.android.network.TutorResponse
import com.lingomia.android.network.PartnerResponse
import com.lingomia.android.network.ChatRequest
import com.lingomia.android.data.models.CreateSessionRequest
import com.lingomia.android.data.models.SceneLevel
import kotlinx.coroutines.launch
import org.json.JSONObject
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.widget.TextView


import android.speech.tts.TextToSpeech
import com.lingomia.android.ui.ConversationActivity.Companion.TAG
import kotlin.system.exitProcess

import java.util.Locale

class ConversationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConversationBinding
    private lateinit var apiService: ApiService
    private lateinit var chatAdapter: ChatAdapter
    private var bottomSheetDialog: BottomSheetDialog? = null

    private var sceneId: Int? = null
    private var topicId: Int? = null
    private var sessionId: String? = null
    private var sceneLevel: SceneLevel? = null
    private val userId = 1
    private val userLevel = "B1"  // Ensure lowercase to match backend
    private var showFeedback = false

    private lateinit var textToSpeech: TextToSpeech

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

        textToSpeech = TextToSpeech(this) { status ->
//            if (status == TextToSpeech.SUCCESS) {
//                // Set language (e.g., US English)
//                val result = textToSpeech.setLanguage(Locale.US)
//                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
//                    // Handle language not supported
//                    speakButton.isEnabled = false
//                } else {
//                    speakButton.isEnabled = true
//                }
//            } else {
//                // Handle initialization failure
//                speakButton.isEnabled = false
//            }
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
                            textToSpeech.setLanguage(Locale.CHINESE)
                            val availableVoices = textToSpeech.voices
                            val cnEnglishVoices = availableVoices.filter { it.locale == Locale.CHINA }
                            if (cnEnglishVoices.isNotEmpty()) {
                                textToSpeech.voice = cnEnglishVoices[0]
                                Log.d("TTS", "Selected Chinese voice: ${cnEnglishVoices[0].name}")
                            }
//                            val maleVoices = availableVoices.filter { it.gender == TextToSpeech.Engine. }
//                            if (femaleVoices.isNotEmpty()) {
//                                textToSpeech.voice = femaleVoices[0]
//                                Log.d("TTS", "Selected female voice: ${femaleVoices[0].name}")
//                            }

                            speak(tutorResponse.tutorMessage)
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