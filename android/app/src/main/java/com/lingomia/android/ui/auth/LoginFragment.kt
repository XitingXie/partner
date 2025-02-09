package com.lingomia.android.ui.auth

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.lingomia.android.R
import com.lingomia.android.auth.AuthManager
import com.lingomia.android.databinding.FragmentLoginBinding
import com.lingomia.android.ui.AuthActivity
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var authManager: AuthManager
    private var callback: LoginCallback? = null

    interface LoginCallback {
        fun onLoginSuccess()
        fun onLoginError(message: String)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as? LoginCallback
            ?: throw RuntimeException("$context must implement LoginCallback")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        authManager = AuthManager.getInstance(requireContext())

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString()
            val password = binding.passwordInput.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showLoading(true)
            lifecycleScope.launch {
                val result = authManager.signInWithEmail(email, password)
                result.onSuccess {
                    callback?.onLoginSuccess()
                }.onFailure { exception ->
                    callback?.onLoginError(exception.message ?: "Sign in failed")
                }
                showLoading(false)
            }
        }

        binding.signUpButton.setOnClickListener {
            (activity as? AuthActivity)?.navigateToSignup()
        }

        binding.forgotPasswordButton.setOnClickListener {
            val email = binding.emailInput.text.toString()
            if (email.isEmpty()) {
                Toast.makeText(context, "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showLoading(true)
            lifecycleScope.launch {
                val result = authManager.sendPasswordResetEmail(email)
                result.onSuccess {
                    Toast.makeText(
                        context,
                        "Password reset email sent",
                        Toast.LENGTH_LONG
                    ).show()
                }.onFailure { exception ->
                    callback?.onLoginError(exception.message ?: "Failed to send reset email")
                }
                showLoading(false)
            }
        }

        binding.googleButton.setOnClickListener {
            (activity as? AuthActivity)?.googleSignIn()
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.loginButton.isEnabled = !show
        binding.googleButton.isEnabled = !show
        binding.forgotPasswordButton.isEnabled = !show
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 
