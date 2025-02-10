package com.lingomia.android.ui.base

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.lingomia.android.auth.AuthManager
import com.lingomia.android.ui.AuthActivity

abstract class BaseAuthActivity : AppCompatActivity() {
    protected lateinit var authManager: AuthManager
    private var isRedirecting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authManager = AuthManager.getInstance(this)
        
        // Only check initial auth state
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null || !user.isEmailVerified) {
            startAuthActivity()
            return
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
    }

    protected fun startAuthActivity() {
        if (isRedirecting) return
        isRedirecting = true
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    protected fun signOut() {
        if (isRedirecting) return
        isRedirecting = true
        authManager.signOut()
        startAuthActivity()
    }
} 