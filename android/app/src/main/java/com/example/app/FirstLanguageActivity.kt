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


    private val translations = listOf(
        Pair("I speak English.", "Start"),
        Pair("Hablo español.", "Comenzar"),
        Pair("我讲中文。", "开始"),
        Pair("Eu falo português.", "Iniciar"),
        Pair("Ich spreche Deutsch.", "Starten"),
        Pair("Je parle français.", "Commencer"),
        Pair("أنا أتحدث العربية.", "ابدأ"),
        Pair("私は日本語を話します。", "開始"),
        Pair("저는 한국어를 합니다.", "시작")
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
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // Start the automatic text switching with animation
        startTextAnimation()
    }

    private fun saveLanguagePreference(language: String) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().apply {
            putString("selectedLanguage", language)
            putBoolean("isUserRegistered", true)
            apply()
        }
    }

    private fun startTextAnimation() {
        val textSwitcher = object : Runnable {
            override fun run() {
                // Fade out animation
                val fadeOut = ObjectAnimator.ofFloat(textView, "alpha", 1f, 0f)
                val fadeOutButton = ObjectAnimator.ofFloat(startButton, "alpha", 1f, 0f)
                fadeOut.duration = 500
                fadeOutButton.duration = 500
                fadeOut.start()
                fadeOutButton.start()

                fadeOut.addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        // Change text and button text
                        currentIndex = (currentIndex + 1) % translations.size
                        textView.text = translations[currentIndex].first
                        startButton.text = translations[currentIndex].second

                        // Fade in animation
                        val fadeIn = ObjectAnimator.ofFloat(textView, "alpha", 0f, 1f)
                        val fadeInButton = ObjectAnimator.ofFloat(startButton, "alpha", 0f, 1f)
                        fadeIn.duration = 500
                        fadeInButton.duration = 500
                        fadeIn.start()
                        fadeInButton.start()
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