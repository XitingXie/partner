package com.lingomia.android.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
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
import com.lingomia.android.R
import com.lingomia.android.databinding.ActivityAuthBinding
import com.lingomia.android.FirstLanguageActivity
import com.lingomia.android.network.ApiConfig
import com.lingomia.android.network.ApiService
import com.lingomia.android.data.models.UserRequest
import com.lingomia.android.data.models.UserResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import com.lingomia.android.auth.AuthManager

class AuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val apiService: ApiService = ApiConfig.apiService
    private lateinit var authManager: AuthManager
    private val TAG = "AuthActivity"

    companion object {
        private const val RC_SIGN_IN = 9001
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
        
        // Log the web client ID being used
        Log.d(TAG, "Using web client ID: ${getString(R.string.default_web_client_id)}")

        setupClickListeners()
        checkCurrentUser()
    }

    private fun setupClickListeners() {
        binding.signInButton.setOnClickListener {
            val email = binding.emailInput.text.toString()
            val password = binding.passwordInput.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showLoading(true)
            lifecycleScope.launch {
                val result = authManager.signInWithEmail(email, password)
                result.onSuccess {
                    startMainActivity()
                }.onFailure { exception ->
                    showError(exception.message ?: "Sign in failed")
                }
                showLoading(false)
            }
        }

        binding.signUpButton.setOnClickListener {
            val email = binding.emailInput.text.toString()
            val password = binding.passwordInput.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showLoading(true)
            lifecycleScope.launch {
                val result = authManager.signUpWithEmail(email, password)
                result.onSuccess {
                    Toast.makeText(
                        this@AuthActivity,
                        "Verification email sent. Please verify your email to sign in.",
                        Toast.LENGTH_LONG
                    ).show()
                }.onFailure { exception ->
                    showError(exception.message ?: "Sign up failed")
                }
                showLoading(false)
            }
        }

        binding.forgotPasswordButton.setOnClickListener {
            val email = binding.emailInput.text.toString()
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showLoading(true)
            lifecycleScope.launch {
                val result = authManager.sendPasswordResetEmail(email)
                result.onSuccess {
                    Toast.makeText(
                        this@AuthActivity,
                        "Password reset email sent",
                        Toast.LENGTH_LONG
                    ).show()
                }.onFailure { exception ->
                    showError(exception.message ?: "Failed to send reset email")
                }
                showLoading(false)
            }
        }

        binding.googleSignInButton.setOnClickListener { googleSignIn() }
    }

    private fun checkCurrentUser() {
        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.isEmailVerified) {
            startLauncherActivity()
        }
    }

    private fun googleSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String, userInfo: GoogleUserInfo) {
        showLoading(true)
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                showLoading(false)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.signInButton.isEnabled = !show
        binding.signUpButton.isEnabled = !show
        binding.googleSignInButton.isEnabled = !show
        binding.forgotPasswordButton.isEnabled = !show
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun startLauncherActivity() {
        val intent = Intent(this, FirstLanguageActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun checkUserInDatabase(user: FirebaseUser, googleUserInfo: GoogleUserInfo? = null) {
        val userId = user.uid

        // Launch a coroutine to call the API
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check if the user exists
                val response: UserResponse = apiService.checkUserExists(userId)
                withContext(Dispatchers.Main) {
                    if (response.exists) {
                        // Save user ID and language preferences
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().apply {
                            putString("userId", response.uid ?: throw IllegalStateException("Backend did not return a user ID"))
                            if (!response.first_language.isNullOrEmpty()) {
                                putString("selectedLanguage", response.first_language)
                                putBoolean("isUserRegistered", true)
                                putLong("languageSelectedTime", System.currentTimeMillis())
                                apply()
                            }
                        }
                        
                        // If user exists and has first_language, skip language selection
                        if (!response.first_language.isNullOrEmpty()) {
                            startMainActivity()
                        } else {
                            startLauncherActivity()
                        }
                    } else {
                        insertNewUserRecord(user, googleUserInfo)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Error checking user: ${e.message}")
                }
            }
        }
    }

    private fun insertNewUserRecord(user: FirebaseUser, googleUserInfo: GoogleUserInfo? = null) {
        val userData = UserRequest(
            uid = user.uid,
            email = user.email ?: "",
            displayName = googleUserInfo?.displayName,
            givenName = googleUserInfo?.givenName,
            familyName = googleUserInfo?.familyName,
            photoUrl = googleUserInfo?.photoUrl
        )

        // Launch a coroutine to call the API
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Insert the new user record
                val response: UserResponse = apiService.insertUser(userData)
                withContext(Dispatchers.Main) {
                    if (response.exists) {
                        // Save user ID to SharedPreferences
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().apply {
                            putString("userId", response.uid ?: throw IllegalStateException("Backend did not return a user ID"))
                            apply()
                        }
                        startLauncherActivity()
                    } else {
                        showError("Failed to insert new user record: ${response.message}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Error inserting user: ${e.message}")
                }
            }
        }
    }
} 