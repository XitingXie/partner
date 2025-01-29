package com.example.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app.R
import com.example.app.data.models.*
import com.example.app.databinding.ActivityConversationBinding
import com.example.app.network.ApiConfig
import com.example.app.network.ApiService
import com.example.app.ui.adapters.ChatAdapter
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale

class ConversationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConversationBinding
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var apiService: ApiService
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    
    private var sceneId: Int? = null
    private var topicId: Int? = null
    private var sessionId: String? = null
    private val userId = 1 // TODO: Get from user authentication
    private var isKeyboardActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityConversationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sceneId = intent.getIntExtra("SCENE_ID", -1)
        topicId = intent.getIntExtra("TOPIC_ID", -1)
        
        if (sceneId == -1 || topicId == -1) {
            Log.e("ConversationActivity", "Missing required IDs - Scene ID: $sceneId, Topic ID: $topicId")
            Toast.makeText(this, "Error: Missing required information", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        setupRecyclerView()
        initializeSpeechRecognizer()
        initializeTextToSpeech()
        
        // Initialize apiService before creating session
        apiService = ApiConfig.getApiService(this)
        
        createSession()
    }

    private fun setupUI() {
        binding.messageInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                val message = binding.messageInput.text.toString().trim()
                if (message.isNotEmpty()) {
                    sendMessage(message)
                    binding.messageInput.setText("")
                }
                true
            } else {
                false
            }
        }

        binding.keyboardButton.setOnClickListener {
            toggleKeyboard()
        }

        binding.toggleMicButton.setOnClickListener {
            if (isKeyboardActive) {
                sendMessage()
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST_RECORD_AUDIO)
                } else {
                    startListening()
                }
            }
        }

        binding.endChatButton.setOnClickListener {
            finish()
        }

        // Initially disable message input until session is created
        binding.messageInput.isEnabled = false
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        binding.chatRecyclerView.adapter = chatAdapter
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun createSession() {
        Log.d("ConversationActivity", "createSession() called")
        Log.d("ConversationActivity", "ApiService instance: $apiService")
        
        lifecycleScope.launch {
            try {
                val currentSceneId = sceneId ?: run {
                    Log.e("ConversationActivity", "Scene ID is null")
                    Toast.makeText(this@ConversationActivity, "Error: Scene ID is missing", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                val currentTopicId = topicId ?: run {
                    Log.e("ConversationActivity", "Topic ID is null")
                    Toast.makeText(this@ConversationActivity, "Error: Topic ID is missing", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                // Create session first
                val request = CreateSessionRequest(
                    userId = userId,
                    sceneId = currentSceneId,
                    topicId = currentTopicId
                )
                
                Log.d("ConversationActivity", "Creating session with request: $request")
                Log.d("ConversationActivity", "Using ApiService: ${apiService.javaClass.name}")
                
                val response = apiService.createSession(request)
                
                Log.d("ConversationActivity", "Got create session response: $response")
                sessionId = response.id
                Log.d("ConversationActivity", "Session created with ID: $sessionId")
                
                // Enable chat input now that we have a session
                runOnUiThread {
                    binding.messageInput.isEnabled = true
                    binding.toggleMicButton.isEnabled = true
                }
                
                // Show welcome message
                handleChatResponse("Welcome! How can I help you today?")
            } catch (e: Exception) {
                Log.e("ConversationActivity", "Error creating session", e)
                Log.e("ConversationActivity", "Error details: ${e.message}")
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(
                        this@ConversationActivity,
                        "Error creating chat session: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
        }
    }

    private fun initializeSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(
                this,
                "Speech recognition is not available",
                Toast.LENGTH_LONG
            ).show()
            binding.toggleMicButton.isEnabled = false
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                binding.toggleMicButton.setImageResource(R.drawable.ic_mic_active)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    handleSpeechInput(text)
                }
                binding.toggleMicButton.setImageResource(R.drawable.ic_mic)
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                binding.toggleMicButton.setImageResource(R.drawable.ic_mic)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("ConversationActivity", "Language not supported")
                }
            } else {
                Log.e("ConversationActivity", "TextToSpeech initialization failed")
            }
        }

        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                Log.d("ConversationActivity", "Started speaking: $utteranceId")
            }

            override fun onDone(utteranceId: String) {
                Log.d("ConversationActivity", "Finished speaking: $utteranceId")
            }

            override fun onError(utteranceId: String) {
                Log.e("ConversationActivity", "Error speaking: $utteranceId")
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String, errorCode: Int) {
                Log.e("ConversationActivity", "Error speaking: $utteranceId, code: $errorCode")
            }
        })
    }

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSION_REQUEST_CODE
            )
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        try {
            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            Log.e("ConversationActivity", "Error starting speech recognition", e)
            Toast.makeText(
                this,
                "Error starting speech recognition",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun handleSpeechInput(text: String) {
        sendMessage(text)
    }

    private fun sendMessage(message: String = binding.messageInput.text.toString().trim()) {
        lifecycleScope.launch {
            try {
                val currentSessionId = sessionId ?: run {
                    Log.e("ConversationActivity", "❌ Session ID is null, cannot send message")
                    Toast.makeText(this@ConversationActivity, "Error: Session not created yet", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                Log.d("ConversationActivity", "📤 Sending message: '$message'")
                Log.d("ConversationActivity", "📦 Session ID: $currentSessionId")

                val currentSceneId = sceneId ?: run {
                    Log.e("ConversationActivity", "❌ Scene ID is null")
                    Toast.makeText(this@ConversationActivity, "Error: Scene not selected", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Add user message to chat
                val userChatMessage = ChatMessage(
                    id = 0,
                    sessionId = currentSessionId,
                    userId = userId,
                    content = message,
                    timestamp = "",
                    type = "user"
                )
                Log.d("ConversationActivity", "✉️ User Message: $userChatMessage")
                
                chatAdapter.addMessage(userChatMessage)

                // Scroll to bottom
                binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)

                // Send message to API
                val request = ChatRequest(
                    sessionId = currentSessionId,
                    sceneId = currentSceneId,
                    userId = userId,
                    userInput = message
                )
                Log.d("ConversationActivity", "🚀 Chat Request: $request")
                
                val response = try {
                    apiService.chat(request)
                } catch (e: Exception) {
                    Log.e("ConversationActivity", "❌ API Call Error", e)
                    Toast.makeText(
                        this@ConversationActivity,
                        "Network Error: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                Log.d("ConversationActivity", "📥 Chat Response: $response")
                Log.d("ConversationActivity", "📨 Response Message: ${response.message}")
                
                handleChatResponse(response.message)
            } catch (e: Exception) {
                Log.e("ConversationActivity", "❌ Unexpected Error in sendMessage", e)
                Toast.makeText(
                    this@ConversationActivity,
                    "Unexpected Error: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun handleChatResponse(message: String) {
        val currentSessionId = sessionId ?: ""
        
        // Try to extract the conversation and feedback from the message
        val conversationText = try {
            val jsonStartIndex = message.indexOf("{")
            if (jsonStartIndex != -1) {
                message.substring(0, jsonStartIndex).trim()
            } else {
                message
            }
        } catch (e: Exception) {
            message
        }

        // Add bot message to chat
        val botChatMessage = ChatMessage(
            id = 0,
            sessionId = currentSessionId,
            userId = 0, // Bot message
            content = conversationText,
            timestamp = "",
            type = "assistant"
        )
        
        Log.d("ConversationActivity", "📬 Bot Message: $botChatMessage")
        chatAdapter.addMessage(botChatMessage)

        // Scroll to bottom
        binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)

        // Try to parse feedback JSON
        try {
            val jsonStartIndex = message.indexOf("{")
            if (jsonStartIndex != -1) {
                val jsonString = message.substring(jsonStartIndex)
                val jsonObject = JSONObject(jsonString)
                
                // Extract and log feedback details
                val unfamiliarWords = jsonObject.optJSONArray("unfamiliar_words")
                val notSoGoodExpressions = jsonObject.optJSONObject("not_so_good_expressions")
                val grammarErrors = jsonObject.optJSONObject("grammar_errors")
                val bestFitWords = jsonObject.optJSONObject("best_fit_words")
                
                Log.d("ConversationActivity", "📊 Feedback Details:")
                Log.d("ConversationActivity", "Unfamiliar Words: ${unfamiliarWords?.toString() ?: "None"}")
                Log.d("ConversationActivity", "Not-So-Good Expressions: ${notSoGoodExpressions?.toString() ?: "None"}")
                Log.d("ConversationActivity", "Grammar Errors: ${grammarErrors?.toString() ?: "None"}")
                Log.d("ConversationActivity", "Best Fit Words: ${bestFitWords?.toString() ?: "None"}")
            }
        } catch (e: Exception) {
            Log.e("ConversationActivity", "❌ Error parsing feedback JSON", e)
        }

        // Speak the response if TTS is enabled
        if (::textToSpeech.isInitialized) {
            textToSpeech.speak(conversationText, TextToSpeech.QUEUE_FLUSH, null, "response")
        }
    }

    private fun hideKeyboard() {
        binding.messageInput.visibility = View.GONE
        binding.keyboardButton.visibility = View.VISIBLE
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.messageInput.windowToken, 0)
    }

    private fun toggleKeyboard() {
        isKeyboardActive = !isKeyboardActive
        binding.messageInput.visibility = if (isKeyboardActive) View.VISIBLE else View.GONE
        binding.toggleMicButton.setImageResource(
            if (isKeyboardActive) R.drawable.ic_send else R.drawable.ic_mic
        )
        
        if (isKeyboardActive) {
            binding.messageInput.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.messageInput, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun sendMessage() {
        val message = binding.messageInput.text.toString().trim()
        if (message.isNotEmpty()) {
            binding.messageInput.text.clear()
            // Send message to backend
            sendMessage(message)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListening()
            } else {
                Toast.makeText(
                    this,
                    "Microphone permission is required for voice recognition",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    override fun onBackPressed() {
        if (isKeyboardActive) {
            hideKeyboard()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 123
        private const val PERMISSION_REQUEST_RECORD_AUDIO = 124
    }
}