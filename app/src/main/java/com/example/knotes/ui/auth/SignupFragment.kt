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
import androidx.navigation.fragment.findNavController
import com.example.knotes.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignupFragment : Fragment() {

    private val signupViewModel: SignupViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val uiState by signupViewModel.uiState.collectAsState()

                SignupScreenCompose(
                    uiState = uiState,
                    onFullNameChanged = signupViewModel::onFullNameChanged,
                    onEmailChanged = signupViewModel::onEmailChanged,
                    onPasswordChanged = signupViewModel::onPasswordChanged,
                    onConfirmPasswordChanged = signupViewModel::onConfirmPasswordChanged,
                    onTogglePasswordVisibility = signupViewModel::togglePasswordVisibility,
                    onToggleConfirmPasswordVisibility = signupViewModel::toggleConfirmPasswordVisibility,
                    onToggleTermsAccepted = signupViewModel::toggleTermsAccepted,
                    onSignupClick = {
                        signupViewModel.signup(
                            onVerificationRequired = {
                                findNavController().navigate(R.id.emailVerificationFragment)
                            }
                        )
                    },
                    onGoogleSignInClick = {
                        Toast.makeText(requireContext(), "Google Sign-In initiated", Toast.LENGTH_SHORT).show()
                    },
                    onNavigateToLogin = {
                        findNavController().navigateUp()
                    },
                    onNavigateBack = {
                        findNavController().navigateUp()
                    }
                )
            }
        }
    }
}
