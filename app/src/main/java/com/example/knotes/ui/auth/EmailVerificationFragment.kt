package com.example.knotes.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.knotes.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EmailVerificationFragment : Fragment() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val user = authViewModel.currentUser

                EmailVerificationScreenCompose(
                    userEmail = user?.email ?: "your email",
                    onCheckVerificationClick = {
                        authViewModel.reloadUser()
                        val updatedUser = authViewModel.currentUser
                        if (updatedUser?.isEmailVerified == true) {
                            Toast.makeText(requireContext(), "Email verified! Welcome to KNotes.", Toast.LENGTH_SHORT).show()
                            findNavController().navigate(R.id.homeFragment)
                        } else {
                            Toast.makeText(requireContext(), "Email not verified yet. Please check your inbox.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onResendEmailClick = {
                        viewLifecycleOwner.lifecycleScope.launch {
                            Toast.makeText(requireContext(), "Verification email sent!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onSignOutClick = {
                        authViewModel.logout {
                            findNavController().navigate(R.id.loginFragment)
                        }
                    }
                )
            }
        }
    }
}
