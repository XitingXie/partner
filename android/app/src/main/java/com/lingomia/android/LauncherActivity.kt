package com.lingomia.android

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.lingomia.android.ui.AuthActivity
import com.lingomia.android.ui.MainActivity
import com.lingomia.android.network.ApiConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LauncherActivity : AppCompatActivity() {
    private val PREFS_NAME = "AppPrefs"
    private val apiService = ApiConfig.apiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val currentUser = FirebaseAuth.getInstance().currentUser
        
        if (currentUser != null && currentUser.isEmailVerified) {
            // User is signed in and verified, check if language is set
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = apiService.checkUserExists(currentUser.uid)
                    withContext(Dispatchers.Main) {
                        if (response.exists && !response.first_language.isNullOrEmpty()) {
                            // User exists and has language set, save to preferences
                            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().apply {
                                putString("selectedLanguage", response.first_language)
                                putBoolean("isUserRegistered", true)
                                putLong("languageSelectedTime", System.currentTimeMillis())
                                apply()
                            }
                            // Go directly to MainActivity
                            startActivity(Intent(this@LauncherActivity, MainActivity::class.java))
                        } else {
                            // User needs to set language
                            startActivity(Intent(this@LauncherActivity, FirstLanguageActivity::class.java))
                        }
                        finish()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        // On error, default to auth flow
                        startActivity(Intent(this@LauncherActivity, AuthActivity::class.java))
                        finish()
                    }
                }
            }
        } else {
            // No user is signed in or email not verified, go to auth flow
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        }
    }
}