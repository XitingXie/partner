package com.lingomia.android.ui.auth

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.lingomia.android.auth.AuthManager
import com.lingomia.android.databinding.FragmentSignupBinding
import com.lingomia.android.ui.AuthActivity
import kotlinx.coroutines.launch

class SignupFragment : Fragment() {
    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!
    private lateinit var authManager: AuthManager
    private var callback: SignupCallback? = null

    interface SignupCallback {
        fun onSignupSuccess()
        fun onSignupError(message: String)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as? SignupCallback
            ?: throw RuntimeException("$context must implement SignupCallback")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authManager = AuthManager.getInstance(requireContext())
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            (activity as? AuthActivity)?.navigateToLogin()
        }

        binding.createButton.setOnClickListener {
            val name = binding.nameInput.text.toString()
            val email = binding.emailInput.text.toString()
            val phone = binding.phoneInput.text.toString()
            val password = binding.passwordInput.text.toString()
            val confirmPassword = binding.confirmPasswordInput.text.toString()

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showLoading(true)
            lifecycleScope.launch {
                val result = authManager.signUpWithEmail(email, password)
                result.onSuccess {
                    Toast.makeText(
                        context,
                        "Verification email sent. Please verify your email to sign in.",
                        Toast.LENGTH_LONG
                    ).show()
                    callback?.onSignupSuccess()
                }.onFailure { exception ->
                    callback?.onSignupError(exception.message ?: "Sign up failed")
                }
                showLoading(false)
            }
        }

        binding.loginButton.setOnClickListener {
            (activity as? AuthActivity)?.navigateToLogin()
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.createButton.isEnabled = !show
        binding.backButton.isEnabled = !show
        binding.loginButton.isEnabled = !show
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 
