package com.lingomia.android

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.lingomia.android.ui.MainActivity
import android.util.Log
import com.lingomia.android.R
import android.widget.ArrayAdapter
import android.widget.ListView

import android.animation.ObjectAnimator
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Button
import com.google.firebase.auth.FirebaseAuth
import com.lingomia.android.network.ApiConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.widget.Toast

class FirstLanguageActivity : AppCompatActivity() {
    private val PREFS_NAME = "AppPrefs"
    private lateinit var textView: TextView
    private lateinit var startButton: Button
    private val apiService = ApiConfig.apiService

    private data class Language(val display: String, val buttonText: String, val code: String)
    
    private val translations = listOf(
        Language("I speak English", "Start", "en"),
        Language("Hablo español", "Comenzar", "es"),
        Language("我讲中文", "开始", "zh"),
        Language("Eu falo português", "Iniciar", "pt"),
        Language("Ich spreche Deutsch", "Starten", "de"),
        Language("Je parle français", "Commencer", "fr"),
        Language("أنا أتحدث العربية", "ابدأ", "ar"),
        Language("私は日本語を話します", "開始", "ja"),
        Language("저는 한국어를 합니다", "시작", "ko")
    )
    private var currentIndex = 0
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("FirstLanguageActivity", "onCreate called")

        // Check if user is logged in
        if (!isUserLoggedIn()) {
            Log.d("FirstLanguageActivity", "No user logged in, redirecting to AuthActivity")
            startActivity(Intent(this, com.lingomia.android.ui.AuthActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_first_language)

        textView = findViewById(R.id.first_language)
        startButton = findViewById(R.id.startButton)

        // Button click listener to navigate to SecondActivity
        startButton.setOnClickListener {
            val selectedLanguage = translations[currentIndex].code
            
            saveLanguagePreference(selectedLanguage)
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("SELECTED_LANGUAGE", selectedLanguage)
            Log.d("FirstLanguageActivity", "Starting MainActivity with language: $selectedLanguage")
            startActivity(intent)
            finish()
        }

        // Start the automatic text switching with animation
        startTextAnimation()
    }

    private fun isUserLoggedIn(): Boolean {
        val currentUser = FirebaseAuth.getInstance().currentUser
        return currentUser != null && currentUser.isEmailVerified
    }

    private fun saveLanguagePreference(language: String) {
        // Save to SharedPreferences
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().apply {
            putString("selectedLanguage", language)
            putBoolean("isUserRegistered", true)
            putLong("languageSelectedTime", System.currentTimeMillis())
            apply()
        }
        
        // Save to backend
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d("FirstLanguageActivity", "Checking if user exists in backend")
                    val checkResponse = apiService.checkUserExists(currentUser.uid)
                    
                    if (!checkResponse.exists) {
                        Log.d("FirstLanguageActivity", "User doesn't exist, creating new user")
                        // Create new user
                        val userRequest = com.lingomia.android.data.models.UserRequest(
                            uid = currentUser.uid,
                            email = currentUser.email ?: "",
                            displayName = currentUser.displayName,
                            photoUrl = currentUser.photoUrl?.toString()
                        )
                        val createResponse = apiService.insertUser(userRequest)
                        if (!createResponse.exists) {
                            throw Exception("Failed to create user: ${createResponse.message}")
                        }
                    }
                    
                    Log.d("FirstLanguageActivity", "Updating language preference in backend: $language")
                    Log.d("FirstLanguageActivity", "User ID: ${currentUser.uid}")
                    
                    val response = apiService.updateFirstLanguage(
                        userId = currentUser.uid,
                        request = mapOf("first_language" to language)
                    )
                    
                    if (response.exists) {
                        Log.d("FirstLanguageActivity", "Successfully updated language in backend")
                    } else {
                        Log.e("FirstLanguageActivity", "Failed to update language: ${response.message}")
                        runOnUiThread {
                            Toast.makeText(
                                this@FirstLanguageActivity,
                                "Failed to save language preference to server",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("FirstLanguageActivity", "Error updating language preference", e)
                    Log.e("FirstLanguageActivity", "Error message: ${e.message}")
                    Log.e("FirstLanguageActivity", "Stack trace: ${e.stackTraceToString()}")
                    runOnUiThread {
                        Toast.makeText(
                            this@FirstLanguageActivity,
                            "Failed to save language preference to server: ${e.localizedMessage}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        } else {
            Log.e("FirstLanguageActivity", "No user signed in")
            runOnUiThread {
                Toast.makeText(
                    this@FirstLanguageActivity,
                    "Error: No user signed in",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        
        Log.d("FirstLanguageActivity", "Saved language preference: $language")
    }

    private fun startTextAnimation() {
        val textSwitcher = object : Runnable {
            override fun run() {
                // Fade out animation
                val fadeOut = ObjectAnimator.ofFloat(textView, "alpha", 1f, 0f)
                fadeOut.duration = 500
                fadeOut.start()

                fadeOut.addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        // Change text and button text
                        currentIndex = (currentIndex + 1) % translations.size
                        textView.text = translations[currentIndex].display
                        startButton.text = translations[currentIndex].buttonText

                        // Fade in animation
                        val fadeIn = ObjectAnimator.ofFloat(textView, "alpha", 0f, 1f)
                        fadeIn.duration = 500
                        fadeIn.start()
                    }
                })

                // Schedule the next update
                handler.postDelayed(this, 2000) // Update every 2 seconds
            }
        }

        // Start the cycle
        handler.post(textSwitcher)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null) // Stop updates to prevent memory leaks
    }
}