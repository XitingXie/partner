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
import com.lingomia.android.network.OpenAIService
import com.aallam.openai.api.audio.Voice
import android.media.MediaPlayer
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.speech.RecognizerIntent
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class ConversationActivity : BaseAuthActivity() {
    private lateinit var binding: ActivityConversationBinding
    private lateinit var apiService: ApiService
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var openAIService: OpenAIService
    private var bottomSheetDialog: BottomSheetDialog? = null

    private var sceneId: String? = null
    private var topicId: String? = null
    private var sessionId: String? = null
    private var sceneLevel: SceneLevel? = null
    private val PREFS_NAME = "AppPrefs"
    private lateinit var userId: String
    private var userLevel: String = "B1"  // Default level
    private var userFirstLanguage: String? = null

    private var mediaPlayer: MediaPlayer? = null

    private val TAG = "ConversationActivity"
    private val PERMISSION_REQUEST_CODE = 123

    private var isRecording = false
    private lateinit var speechRecognizer: SpeechRecognizer
    private val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(android.R.color.white)
        binding = ActivityConversationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize OpenAI service
        openAIService = OpenAIService(this)

        // Initialize userId from auth manager
        val currentUser = authManager.currentUser
        if (currentUser == null) {
            Log.e(TAG, "No user found, redirecting to auth")
            startAuthActivity()
            return
        }
        userId = currentUser.uid
        Log.d(TAG, "Initialized userId: $userId")

        // Initialize user preferences from SharedPreferences
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        userFirstLanguage = prefs.getString("selectedLanguage", null)
        userLevel = prefs.getString("userLevel", "B1") ?: "B1"  // Default to B1 if not set
        Log.d(TAG, "User's first language: $userFirstLanguage")
        Log.d(TAG, "User's level: $userLevel")

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
        
        // Play opening remarks audio after setup
        playOpeningRemarks()

        // Check for microphone permission and initialize speech recognizer
        if (checkPermission()) {
            initializeSpeechRecognizer()
        } else {
            requestPermission()
        }
        setupMicButton()
    }

    

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        binding.chatRecyclerView.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(this@ConversationActivity)
        }
    }

    private fun setupUI() {
        // binding.messageInput.setOnEditorActionListener { _, actionId, event ->
        //     if (actionId == EditorInfo.IME_ACTION_SEND ||
        //         (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
        //     ) {
        //         sendMessage()
        //         true
        //     } else {
        //         false
        //     }
        // }

        // binding.sendButton.setOnClickListener {
        //     sendMessage()
        // }
        
        // binding.messageInput.isEnabled = false
    }

    private fun createSession() {
        lifecycleScope.launch {
            try {
                val request = CreateSessionRequest(
                    topicId = topicId!!,
                    sceneId = sceneId!!,
                    userId = userId
                )
                val response = apiService.createSession(request)
                sessionId = response.id

                runOnUiThread {

                    scrollToBottom()
                }
            } catch (e: Exception) {
                showErrorAndExit("Error creating chat session: ${e.message}")
            }
        }
    }

    private fun speak(text: String) {
        lifecycleScope.launch {
            try {
                val voice = when {
                    text.startsWith("Tutor:") -> Voice.Alloy
                    else -> Voice.Nova
                }
                openAIService.textToSpeech(text, voice)
                runOnUiThread {
                    binding.micButton.isEnabled = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in text to speech", e)
                runOnUiThread {
                    Toast.makeText(this@ConversationActivity, 
                        "Error playing audio: ${e.localizedMessage}", 
                        Toast.LENGTH_SHORT).show()
                    binding.micButton.isEnabled = true
                }
            }
        }
    }

    private fun playAudio(response: ResponseBody) {
        // Check content type first
        val contentType = response.contentType()
        if (contentType?.toString()?.contains("audio") != true) {
            Log.d(TAG, "Non-audio content type received: $contentType")
            return
        }

        // Check content length
        if (response.contentLength() == 0L) {
            Log.d(TAG, "No audio content received")
            return
        }

        var tempFile: File? = null
        try {
            // Log content type and length for debugging
            Log.d(TAG, "Audio content type: ${response.contentType()}")
            Log.d(TAG, "Audio content length: ${response.contentLength()}")

            // Create a temporary file to store the audio
            tempFile = File.createTempFile("audio_", ".mp3", applicationContext.cacheDir)
            
            // Write the response body to the temp file
            val audioBytes = response.bytes()
            Log.d(TAG, "Audio bytes size: ${audioBytes.size}")
            
            // Check if response is a JSON message
            if (contentType.toString().contains("application/json")) {
                Log.d(TAG, "Received JSON response instead of audio")
                return
            }
            
            if (audioBytes.size < 100) { // Likely invalid audio file if too small
                Log.d(TAG, "Audio file too small, likely invalid")
                return
            }

            FileOutputStream(tempFile).use { outputStream ->
                outputStream.write(audioBytes)
            }

            // Release any existing MediaPlayer
            mediaPlayer?.release()
            mediaPlayer = null

            // Create and prepare new MediaPlayer
            mediaPlayer = MediaPlayer().apply {
                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    mp.reset()
                    true
                }
                
                try {
                    setDataSource(tempFile.path)
                    prepare()
                    start()
                    
                    // Delete temp file when done playing
                    setOnCompletionListener {
                        tempFile?.delete()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "MediaPlayer preparation failed", e)
                    Log.e(TAG, "File path: ${tempFile.path}")
                    Log.e(TAG, "File exists: ${tempFile.exists()}")
                    Log.e(TAG, "File size: ${tempFile.length()}")
                    reset()
                    tempFile?.delete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio: ${e.message}")
            Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
            mediaPlayer?.release()
            mediaPlayer = null
            tempFile?.delete()
            
            // Only show toast for actual playback errors, not invalid files
            if (response.contentLength() > 100L && !contentType.toString().contains("application/json")) {
                Toast.makeText(this, "Error playing audio: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun playOpeningRemarks() {
        lifecycleScope.launch {
            try {
                val openingRemarks = apiService.getOpeningRemarksAudio(sceneId!!, userLevel)
                Log.d(TAG, "Opening remarks audio received, content length: ${openingRemarks.contentLength()}")
                
                // Check content type
                val contentType = openingRemarks.contentType()
                if (contentType?.toString()?.contains("application/json") == true) {
                    Log.d(TAG, "Received JSON response - no audio available")
                    return@launch
                }
                
                if (contentType?.toString()?.contains("audio") != true) {
                    Log.e(TAG, "Invalid content type received: $contentType")
                    return@launch
                }
                
                if (openingRemarks.contentLength() > 100L) {
                    runOnUiThread {
                        playAudio(openingRemarks)
                    }
                } else {
                    Log.d(TAG, "Opening remarks audio file too small or empty")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting opening remarks: ${e.message}")
                Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
                // Don't show toast for missing audio files
                if (e.message?.contains("404") != true) {
                    Toast.makeText(this@ConversationActivity, 
                        "Error getting opening remarks: ${e.localizedMessage}", 
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        mediaPlayer?.release()
        mediaPlayer = null
        openAIService.release()
    }

    private fun initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                runOnUiThread {
                    binding.micButton.setImageResource(R.drawable.ic_mic_active)
                }
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                runOnUiThread {
                    binding.micButton.setImageResource(R.drawable.ic_mic)
                    isRecording = false
                }
            }

            override fun onError(error: Int) {
                Log.e(TAG, "Speech recognition error: $error")
                runOnUiThread {
                    binding.micButton.setImageResource(R.drawable.ic_mic)
                    binding.micButton.isEnabled = true
                    isRecording = false
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> 
                            Toast.makeText(this@ConversationActivity, "No speech detected", Toast.LENGTH_SHORT).show()
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 
                            Toast.makeText(this@ConversationActivity, "No speech detected", Toast.LENGTH_SHORT).show()
                        else -> 
                            Toast.makeText(this@ConversationActivity, "Error occurred in speech recognition", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0]
                    Log.d(TAG, "Speech recognized: $spokenText")
                    sendMessage(spokenText)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun setupMicButton() {
        binding.micButton.setOnClickListener {
            if (!checkPermission()) {
                requestPermission()
                return@setOnClickListener
            }
            
            if (!isRecording) {
                startRecording()
            } else {
                stopRecording()
            }
        }
    }

    private fun startRecording() {
        if (!checkPermission()) {
            requestPermission()
            return
        }
        
        try {
            speechRecognizer.startListening(recognizerIntent)
            isRecording = true
            binding.micButton.setImageResource(R.drawable.ic_mic_active)
            binding.micButton.isEnabled = true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition", e)
            Toast.makeText(this, "Error starting speech recognition", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        speechRecognizer.stopListening()
        isRecording = false
        binding.micButton.setImageResource(R.drawable.ic_mic)
    }

    private fun sendMessage(message: String) {
        if (message.isEmpty() || sessionId == null) return

        Log.d(TAG, "Sending message: $message")
        
        chatAdapter.addMessage(message, isUser = true)
        scrollToBottom()
        
        // Disable mic button while processing
        binding.micButton.isEnabled = false

        lifecycleScope.launch {
            try {
                // First, check with tutor
                val tutorRequest = ChatRequest(
                    sessionId = sessionId!!,
                    sceneId = sceneId!!,
                    userId = userId,
                    userInput = message,
                    first_language = userFirstLanguage ?: "en"
                )
                
                Log.d(TAG, "Sending tutor request with userId: $userId")
                val tutorResponse = apiService.chatWithTutor(tutorRequest)
                Log.d(TAG, "Tutor response: $tutorResponse")

                if (tutorResponse.needsCorrection) {
                    runOnUiThread {
                        speak(tutorResponse.tutorMessage)
                    }
                } else {
                    // If no corrections needed, proceed with partner chat
                    Log.d(TAG, "No correction needed, proceeding with partner chat")
                    val partnerRequest = ChatRequest(
                        sessionId = sessionId!!,
                        sceneId = sceneId!!,
                        userId = userId,
                        userInput = message
                    )
                    val partnerResponse = apiService.chatWithPartner(partnerRequest)
                    Log.d(TAG, "Partner response: $partnerResponse")

                    runOnUiThread {
                        speak(partnerResponse.message)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message", e)
                runOnUiThread {
                    Toast.makeText(this@ConversationActivity, "Network Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    binding.micButton.isEnabled = true
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

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                    initializeSpeechRecognizer()
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                    // Disable the mic button if permission is denied
                    binding.micButton.isEnabled = false
                }
                return
            }
        }
    }
}