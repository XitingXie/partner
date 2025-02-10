package com.lingomia.android.network

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.aallam.openai.api.audio.SpeechRequest
import com.aallam.openai.api.audio.Voice
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.time.Duration.Companion.seconds
import com.lingomia.android.network.ApiConfig
import com.lingomia.android.BuildConfig

class OpenAIService(private val context: Context) {
    private var openAI: OpenAI? = null
    private val cacheDir = context.cacheDir
    private var mediaPlayer: MediaPlayer? = null

    private suspend fun getOpenAIClient(): OpenAI {
        if (openAI == null) {
            // In release builds, get the API key from your backend
            val apiKey = if (BuildConfig.DEBUG) {
                val key = BuildConfig.OPENAI_API_KEY
                Log.d(TAG, "Debug build - API key present: ${!key.isBlank()}")
                Log.d(TAG, "API key first 4 chars: ${if (key.length >= 4) key.substring(0, 4) else "key too short"}")
                if (key.isBlank()) {
                    Log.e(TAG, "OpenAI API key is blank in debug build")
                    throw IllegalStateException("OpenAI API key not found. Please set OPENAI_API_KEY in .env file")
                }
                key
            } else {
                // Get API key from your backend
                val response = ApiConfig.apiService.getOpenAIKey()
                Log.d(TAG, "Release build - API key from backend present: ${!response.apiKey.isBlank()}")
                if (response.apiKey.isBlank()) {
                    Log.e(TAG, "OpenAI API key is blank from backend")
                    throw IllegalStateException("OpenAI API key not found from backend")
                }
                response.apiKey
            }
            
            Log.d(TAG, "Initializing OpenAI client with key length: ${apiKey.length}")
            openAI = OpenAI(
                token = apiKey,
                timeout = Timeout(socket = 60.seconds)
            )
        }
        return openAI!!
    }

    private suspend fun getApiKeyFromBackend(): String {
        return try {
            // Make an authenticated request to your backend to get the API key
            val response = ApiConfig.apiService.getOpenAIKey()
            response.apiKey
        } catch (e: Exception) {
            Log.e(TAG, "Error getting API key from backend", e)
            throw e
        }
    }

    suspend fun textToSpeech(text: String, voice: Voice = Voice.Alloy) {
        try {
            withContext(Dispatchers.IO) {
                val client = getOpenAIClient()
                val request = SpeechRequest(
                    model = ModelId("tts-1"),
                    input = text,
                    voice = voice
                )

                val audioData = client.speech(request)
                
                // Save audio to a temporary file
                val tempFile = File(cacheDir, "speech_${System.currentTimeMillis()}.mp3")
                FileOutputStream(tempFile).use { output ->
                    output.write(audioData)
                }

                // Play the audio
                withContext(Dispatchers.Main) {
                    playAudio(tempFile)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in text to speech", e)
            throw e
        }
    }

    private fun playAudio(audioFile: File) {
        try {
            // Release previous MediaPlayer if it exists
            mediaPlayer?.release()
            
            // Create and configure new MediaPlayer
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFile.path)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio", e)
            throw e
        }
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        openAI = null
    }

    companion object {
        private const val TAG = "OpenAIService"
    }
} 