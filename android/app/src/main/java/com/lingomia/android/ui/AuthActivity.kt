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
import com.google.firebase.auth.GoogleAuthProvider
import com.lingomia.android.R
import com.lingomia.android.databinding.ActivityAuthBinding
import com.lingomia.android.FirstLanguageActivity

class AuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        private const val TAG = "AuthActivity"
        private const val RC_SIGN_IN = 9001
    }

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            Log.d(TAG, "Google Sign In successful, idToken: ${account.idToken?.take(10)}...")
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Log.e(TAG, "Google sign in failed", e)
            Log.e(TAG, "Error code: ${e.statusCode}")
            showError("Google sign in failed: ${e.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

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
        binding.signInButton.setOnClickListener { signIn() }
        binding.signUpButton.setOnClickListener { signUp() }
        binding.googleSignInButton.setOnClickListener { googleSignIn() }
        binding.forgotPasswordText.setOnClickListener { forgotPassword() }
    }

    private fun checkCurrentUser() {
        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.isEmailVerified) {
            startLauncherActivity()
        }
    }

    private fun signIn() {
        val email = binding.emailInput.text.toString()
        val password = binding.passwordInput.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields")
            return
        }

        showLoading(true)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user?.isEmailVerified == true) {
                        startLauncherActivity()
                    } else {
                        showError("Please verify your email first")
                        sendEmailVerification()
                    }
                } else {
                    showError("Authentication failed: ${task.exception?.message}")
                }
            }
    }

    private fun signUp() {
        val email = binding.emailInput.text.toString()
        val password = binding.passwordInput.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields")
            return
        }

        showLoading(true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    sendEmailVerification()
                    showError("Please check your email for verification")
                } else {
                    showError("Sign up failed: ${task.exception?.message}")
                }
            }
    }

    private fun googleSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        showLoading(true)
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    startLauncherActivity()
                } else {
                    showError("Google authentication failed: ${task.exception?.message}")
                }
            }
    }

    private fun forgotPassword() {
        val email = binding.emailInput.text.toString()
        if (email.isEmpty()) {
            showError("Please enter your email")
            return
        }

        showLoading(true)
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    showError("Password reset email sent")
                } else {
                    showError("Failed to send reset email: ${task.exception?.message}")
                }
            }
    }

    private fun sendEmailVerification() {
        auth.currentUser?.sendEmailVerification()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showError("Verification email sent")
                } else {
                    showError("Failed to send verification email")
                }
            }
    }

    private fun startLauncherActivity() {
        val intent = Intent(this, FirstLanguageActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.signInButton.isEnabled = !show
        binding.signUpButton.isEnabled = !show
        binding.googleSignInButton.isEnabled = !show
        binding.forgotPasswordText.isEnabled = !show
    }
} 