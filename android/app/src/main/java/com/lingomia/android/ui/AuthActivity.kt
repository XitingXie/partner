package com.lingomia.android.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.lingomia.android.LauncherActivity
import com.lingomia.android.R
import com.lingomia.android.auth.AuthManager
import com.lingomia.android.databinding.ActivityAuthBinding
import com.lingomia.android.ui.auth.AuthPagerAdapter
import com.lingomia.android.ui.auth.LoginFragment
import com.lingomia.android.ui.auth.SignupFragment

class AuthActivity : AppCompatActivity(), LoginFragment.LoginCallback, SignupFragment.SignupCallback {
    private lateinit var binding: ActivityAuthBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var authManager: AuthManager
    private lateinit var authPagerAdapter: AuthPagerAdapter
    private val TAG = "AuthActivity"

    companion object {
        private const val PREFS_NAME = "AppPrefs"
    }

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            Log.d(TAG, "Google Sign In successful, idToken: ${account.idToken?.take(10)}...")
            // Get additional user info
            val displayName = account.displayName
            val givenName = account.givenName
            val familyName = account.familyName
            val email = account.email
            val photoUrl = account.photoUrl?.toString()
            
            Log.d(TAG, "User info - Name: $displayName, Email: $email, Photo: $photoUrl")
            
            firebaseAuthWithGoogle(account.idToken!!, GoogleUserInfo(
                displayName = displayName ?: "",
                givenName = givenName ?: "",
                familyName = familyName ?: "",
                email = email ?: "",
                photoUrl = photoUrl ?: ""
            ))
        } catch (e: ApiException) {
            Log.e(TAG, "Google sign in failed", e)
            Log.e(TAG, "Error code: ${e.statusCode}")
            showError("Google sign in failed: ${e.message}")
        }
    }

    data class GoogleUserInfo(
        val displayName: String,
        val givenName: String,
        val familyName: String,
        val email: String,
        val photoUrl: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        authManager = AuthManager.getInstance(this)

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        
        setupViewPager()
        checkCurrentUser()
    }

    private fun setupViewPager() {
        authPagerAdapter = AuthPagerAdapter(this)
        binding.authViewPager.apply {
            adapter = authPagerAdapter
            isUserInputEnabled = false  // Disable swiping between pages
        }
    }

    fun navigateToSignup() {
        binding.authViewPager.currentItem = 1
    }

    fun navigateToLogin() {
        binding.authViewPager.currentItem = 0
    }

    fun googleSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private fun checkCurrentUser() {
        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.isEmailVerified) {
            startLauncherActivity()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String, userInfo: GoogleUserInfo) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        checkUserInDatabase(user, userInfo)
                    }
                } else {
                    showError("Google authentication failed: ${task.exception?.message}")
                }
            }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun startLauncherActivity() {
        val intent = Intent(this, LauncherActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun checkUserInDatabase(user: FirebaseUser, googleUserInfo: GoogleUserInfo? = null) {
        // Implementation of user database check
        startLauncherActivity()
    }

    // LoginCallback implementations
    override fun onLoginSuccess() {
        startLauncherActivity()
    }

    override fun onLoginError(message: String) {
        showError(message)
    }

    // SignupCallback implementations
    override fun onSignupSuccess() {
        navigateToLogin()
    }

    override fun onSignupError(message: String) {
        showError(message)
    }
} 