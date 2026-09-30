package com.example.knotes.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.knotes.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private val loginViewModel: LoginViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    private var showForgotPasswordDialog by mutableStateOf(false)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val uiState by loginViewModel.uiState.collectAsState()

                LoginScreenCompose(
                    uiState = uiState,
                    onEmailChanged = loginViewModel::onEmailChanged,
                    onPasswordChanged = loginViewModel::onPasswordChanged,
                    onTogglePasswordVisibility = loginViewModel::togglePasswordVisibility,
                    onToggleRememberMe = loginViewModel::toggleRememberMe,
                    onCaptchaAnswerChanged = loginViewModel::onCaptchaAnswerChanged,
                    onRefreshCaptcha = loginViewModel::refreshCaptcha,
                    onLoginClick = {
                        loginViewModel.login(
                            onSuccess = {
                                navigateToHome()
                            },
                            onVerificationRequired = {
                                findNavController().navigate(R.id.emailVerificationFragment)
                            }
                        )
                    },
                    onGoogleSignInClick = {
                        Toast.makeText(requireContext(), "Google Sign-In initiated", Toast.LENGTH_SHORT).show()
                    },
                    onForgotPasswordClick = {
                        showForgotPasswordDialog = true
                    },
                    onNavigateToSignup = {
                        findNavController().navigate(R.id.signupFragment)
                    },
                    onNavigateBack = {
                        findNavController().navigateUp()
                    }
                )

                if (showForgotPasswordDialog) {
                    ForgotPasswordDialogCompose(
                        initialEmail = uiState.email,
                        onDismiss = { showForgotPasswordDialog = false },
                        onSendClick = { email, onResult ->
                            loginViewModel.sendPasswordReset(email, onResult)
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeAuthState()
    }

    private fun observeAuthState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.authState.collect { state ->
                    when (state) {
                        is com.example.knotes.domain.repository.AuthState.LoggedIn -> {
                            navigateToHome()
                        }
                        is com.example.knotes.domain.repository.AuthState.EmailVerificationRequired -> {
                            findNavController().navigate(R.id.emailVerificationFragment)
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun navigateToHome() {
        findNavController().navigate(R.id.homeFragment)
    }
}
