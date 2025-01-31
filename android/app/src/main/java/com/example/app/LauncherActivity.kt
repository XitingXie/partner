package com.example.app

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.app.ui.MainActivity
import android.util.Log
import com.example.app.FirstLanguageActivity

class LauncherActivity : AppCompatActivity() {
    private val PREFS_NAME = "AppPrefs"
    private val KEY_FIRST_LAUNCH = "firstLaunch"
    private val KEY_APP_VERSION = "appVersion"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("LauncherActivity", "onCreate called")
        setContentView(R.layout.activity_launcher)
        
        // val prefs: SharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        // val isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        // val savedVersion = prefs.getInt(KEY_APP_VERSION, 0)
        // val currentVersion = BuildConfig.VERSION_CODE

//        val intent = when {
////            isFirstLaunch || savedVersion < currentVersion -> {
////                prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
////                prefs.edit().putInt(KEY_APP_VERSION, currentVersion).apply()
////                Intent(this, OnboardingActivity::class.java)
////            }
////            !isUserRegistered() -> {
////                Intent(this, FirstLanguageActivity::class.java)
////            }
////            else -> {
////                Intent(this, MainActivity::class.java)
////            }
//        }
        val intent = Intent(this, FirstLanguageActivity::class.java)
        Log.d("LauncherActivity", "Starting FirstLanguageActivity")
        startActivity(intent)
        finish()
    }

    private fun isUserRegistered(): Boolean {
        val prefs: SharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        return prefs.getBoolean("isUserRegistered", false)
    }
}