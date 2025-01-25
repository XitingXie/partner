package com.example.app.ui.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.speech.tts.UtteranceProgressListener
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.app.R
import com.example.app.databinding.FragmentChatBinding
import com.example.app.network.ApiService
import com.example.app.network.ApiConfig
import com.example.app.network.ChatRequest
import kotlinx.coroutines.launch
import java.util.Locale

class ChatFragment : Fragment(), OnInitListener {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private val apiService: ApiService = ApiConfig.apiService
    private var sessionId: String? = null
    private var sceneId: String? = null
    private var userId: String? = null
    private var isListening = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        textToSpeech = TextToSpeech(requireContext(), this)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (checkPermissions()) {
            setupSpeechRecognizer()
        }
        setupMicButton()
    }

    private fun checkPermissions(): Boolean {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSION_REQUEST_CODE
            )
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupSpeechRecognizer()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Microphone permission is required for voice recognition",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            Log.e("ChatFragment", "Speech recognition not available on this device")
            Toast.makeText(context, "Speech recognition not available", Toast.LENGTH_SHORT).show()
            return
        }
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("SpeechRecognition", "Ready for speech, params: $params")
            }

            override fun onBeginningOfSpeech() {
                Log.d("SpeechRecognition", "Speech began")
            }

            override fun onRmsChanged(rmsdB: Float) {
                Log.d("SpeechRecognition", "RMS changed: $rmsdB")
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                Log.d("SpeechRecognition", "Buffer received: ${buffer?.size} bytes")
            }

            override fun onEndOfSpeech() {
                Log.d("SpeechRecognition", "Speech ended")
                stopListening()
            }

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No recognition match"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Unknown error"
                }
                Log.e("SpeechRecognition", "Error: $errorMessage ($error)")
                stopListening()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                Log.d("SpeechRecognition", "Got results: $matches")
                matches?.get(0)?.let { text ->
                    Log.d("SpeechRecognition", "Selected text: $text")
                    sendMessageToBackend(text)
                } ?: Log.e("SpeechRecognition", "No speech results")
                stopListening()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                Log.d("SpeechRecognition", "Partial results: $matches")
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                Log.d("SpeechRecognition", "Event type: $eventType, params: $params")
            }
        })
    }

    private fun setupMicButton() {
        binding.toggleMicButton.apply {
            isEnabled = true
            setOnClickListener {
                if (!checkPermissions()) {
                    return@setOnClickListener
                }

                if (isListening) {
                    stopListening()
                } else {
                    startListening()
                }
            }
        }
    }

    private fun startListening() {
        if (isListening) {
            Log.d("ChatFragment", "Already listening, ignoring start request")
            return
        }

        try {
            if (!::speechRecognizer.isInitialized) {
                Log.e("ChatFragment", "SpeechRecognizer not initialized")
                return
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH)
            }
            Log.d("ChatFragment", "Starting speech recognition")
            speechRecognizer.startListening(intent)
            isListening = true
            binding.toggleMicButton.setImageResource(R.drawable.ic_mic_active)
        } catch (e: Exception) {
            Log.e("ChatFragment", "Error starting speech recognition", e)
            Toast.makeText(context, "Error starting voice recognition", Toast.LENGTH_SHORT).show()
            isListening = false
            binding.toggleMicButton.setImageResource(R.drawable.ic_mic)
        }
    }

    private fun stopListening() {
        if (::speechRecognizer.isInitialized && isListening) {
            Log.d("ChatFragment", "Stopping speech recognition")
            speechRecognizer.stopListening()
            isListening = false
            binding.toggleMicButton.apply {
                setImageResource(R.drawable.ic_mic)
                isEnabled = false
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.ENGLISH)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("ChatFragment", "Language not supported")
                Toast.makeText(context, "Text to speech language not supported", Toast.LENGTH_SHORT).show()
            } else {
                textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d("ChatFragment", "TTS started")
                    }

                    override fun onDone(utteranceId: String?) {
                        Log.d("ChatFragment", "TTS completed, re-enabling mic button")
                        binding.toggleMicButton.post {
                            binding.toggleMicButton.isEnabled = true
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        Log.e("ChatFragment", "TTS error, re-enabling mic button")
                        binding.toggleMicButton.post {
                            binding.toggleMicButton.isEnabled = true
                        }
                    }
                })
            }
        } else {
            Log.e("ChatFragment", "TTS initialization failed")
            Toast.makeText(context, "Failed to initialize text to speech", Toast.LENGTH_SHORT).show()
        }
    }

    fun onSessionCreated(sessionId: String, sceneId: String, userId: String) {
        this.sessionId = sessionId
        this.sceneId = sceneId
        this.userId = userId
        sendMessageToBackend("Hello")
    }

    private fun sendMessageToBackend(message: String) {
        Log.d("ChatFragment", "Attempting to send message: $message")
        sessionId?.let { sid ->
            sceneId?.let { scene ->
                userId?.let { user ->
                    lifecycleScope.launch {
                        try {
                            val request = ChatRequest(
                                session_id = sid,
                                user_id = user,
                                scene_id = scene,
                                user_input = message
                            )
                            Log.d("ChatFragment", "Sending request: $request")
                            val response = apiService.chat(request)
                            Log.d("ChatFragment", "Got response: $response")
                            speakResponse(response.message)
                        } catch (e: Exception) {
                            Log.e("ChatFragment", "Backend error", e)
                            binding.toggleMicButton.post {
                                binding.toggleMicButton.isEnabled = true
                            }
                            Toast.makeText(
                                context,
                                "Error: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    private fun speakResponse(text: String) {
        if (!::textToSpeech.isInitialized) {
            Log.e("ChatFragment", "TTS not initialized")
            binding.toggleMicButton.isEnabled = true
            return
        }

        binding.toggleMicButton.isEnabled = false
        val result = textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utteranceId")
        if (result == TextToSpeech.ERROR) {
            Log.e("ChatFragment", "Error speaking text")
            binding.toggleMicButton.isEnabled = true
        } else {
            Log.d("ChatFragment", "Speaking text: $text")
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 123
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        _binding = null
    }
}