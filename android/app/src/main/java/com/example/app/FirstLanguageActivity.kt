package com.example.app

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.app.ui.MainActivity
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView

import android.animation.ObjectAnimator
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Button

class FirstLanguageActivity : AppCompatActivity() {
    private val PREFS_NAME = "AppPrefs"
    private lateinit var textView: TextView
    private lateinit var startButton: Button

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

    private fun saveLanguagePreference(language: String) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().apply {
            putString("selectedLanguage", language)
            putBoolean("isUserRegistered", true)
            putLong("languageSelectedTime", System.currentTimeMillis())
            apply()
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