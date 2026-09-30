package com.example.knotes.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.knotes.data.repository.SyncRepository
import com.example.knotes.domain.repository.AuthRepository
import com.example.knotes.util.AuthValidator
import com.example.knotes.util.PasswordValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignupUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val termsAccepted: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val passwordValidation: PasswordValidationResult = AuthValidator.validatePassword("")
)

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignupUiState())
    val uiState: StateFlow<SignupUiState> = _uiState.asStateFlow()

    fun onFullNameChanged(name: String) {
        _uiState.update { it.copy(fullName = name, errorMessage = null) }
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPasswordChanged(password: String) {
        val validation = AuthValidator.validatePassword(password)
        _uiState.update {
            it.copy(
                password = password,
                passwordValidation = validation,
                errorMessage = null
            )
        }
    }

    fun onConfirmPasswordChanged(password: String) {
        _uiState.update { it.copy(confirmPassword = password, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    fun toggleTermsAccepted() {
        _uiState.update { it.copy(termsAccepted = !it.termsAccepted) }
    }

    fun signup(onVerificationRequired: () -> Unit) {
        val state = _uiState.value
        if (state.isLoading) return

        val (nameValid, nameError) = AuthValidator.validateFullName(state.fullName)
        if (!nameValid) {
            _uiState.update { it.copy(errorMessage = nameError) }
            return
        }

        val (emailValid, emailError) = AuthValidator.validateEmail(state.email)
        if (!emailValid) {
            _uiState.update { it.copy(errorMessage = emailError) }
            return
        }

        if (!state.passwordValidation.isValid) {
            _uiState.update { it.copy(errorMessage = state.passwordValidation.errorMessage ?: "Password requirements not met") }
            return
        }

        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Passwords do not match") }
            return
        }

        if (!state.termsAccepted) {
            _uiState.update { it.copy(errorMessage = "Please accept the Terms & Privacy Policy to continue") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = authRepository.signup(state.fullName, state.email, state.password)
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false) }
                syncRepository.startSync(manual = true)
                onVerificationRequired()
            }.onFailure { exception ->
                val message = AuthValidator.mapFirebaseException(exception)
                _uiState.update { it.copy(isLoading = false, errorMessage = message) }
            }
        }
    }

    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(idToken)
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false) }
                syncRepository.startSync(manual = true)
                onSuccess()
            }.onFailure { exception ->
                val message = AuthValidator.mapFirebaseException(exception)
                _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                onError(message)
            }
        }
    }
}
