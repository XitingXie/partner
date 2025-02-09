package com.lingomia.android

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.lingomia.android.R
import com.lingomia.android.ui.AuthActivity
import com.lingomia.android.ui.MainActivity
import com.lingomia.android.network.ApiConfig
import kotlinx.coroutines.*

class LauncherActivity : AppCompatActivity() {
    private val apiService = ApiConfig.apiService
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var retryCount = 0
    private val maxRetries = 3

    private var progressBar: ProgressBar? = null
    private var errorContainer: LinearLayout? = null
    private var retryButton: Button? = null
    private var errorMessage: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        
        initializeViews()
        
        retryButton?.setOnClickListener {
            retryCount = 0
            checkServerConnection()
        }
        
        val currentUser = FirebaseAuth.getInstance().currentUser
        
        if (currentUser == null || !currentUser.isEmailVerified) {
            startAuthActivity()
            return
        }

        checkServerConnection()
    }

    private fun initializeViews() {
        try {
            progressBar = findViewById(R.id.progressBar)
            errorContainer = findViewById(R.id.errorContainer)
            retryButton = findViewById(R.id.retryButton)
            errorMessage = findViewById(R.id.errorMessage)
        } catch (e: Exception) {
            // Log the error but don't crash
            e.printStackTrace()
        }
    }

    private fun checkServerConnection() {
        // Show loading indicator, hide error
        progressBar?.visibility = View.VISIBLE
        errorContainer?.visibility = View.GONE
        
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        
        coroutineScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.checkUserExists(currentUser.uid)
                }
                
                progressBar?.visibility = View.GONE
                if (response.exists && !response.first_language.isNullOrEmpty()) {
                    startMainActivity()
                } else {
                    startFirstLanguageActivity()
                }
            } catch (e: Exception) {
                retryCount++
                if (retryCount < maxRetries) {
                    // Silent retry after a short delay
                    delay(1000)
                    checkServerConnection()
                } else {
                    // Show error UI after max retries
                    progressBar?.visibility = View.GONE
                    errorContainer?.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun startAuthActivity() {
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun startFirstLanguageActivity() {
        startActivity(Intent(this, FirstLanguageActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}