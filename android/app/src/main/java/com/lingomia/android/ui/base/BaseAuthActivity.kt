package com.lingomia.android.ui.base

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.lingomia.android.auth.AuthManager
import com.lingomia.android.ui.AuthActivity

abstract class BaseAuthActivity : AppCompatActivity() {
    protected lateinit var authManager: AuthManager
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authManager = AuthManager.getInstance(this)

        // Initialize auth state listener
        authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            if (user == null || !user.isEmailVerified) {
                // User is not signed in or email not verified, redirect to auth activity
                startAuthActivity()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Add auth state listener
        authManager.addAuthStateListener(authStateListener)
        
        // Check auth state immediately
        if (!authManager.isUserSignedIn) {
            startAuthActivity()
        }
    }

    override fun onStop() {
        super.onStop()
        // Remove auth state listener
        authManager.removeAuthStateListener(authStateListener)
    }

    private fun startAuthActivity() {
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    protected fun signOut() {
        authManager.signOut()
        startAuthActivity()
    }
} 