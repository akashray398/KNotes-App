package com.example.knotes.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.knotes.data.repository.SyncRepository
import com.example.knotes.domain.repository.AuthRepository
import com.example.knotes.util.AuthValidator
import com.example.knotes.util.CaptchaChallenge
import com.example.knotes.util.CaptchaManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val rememberMe: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isCaptchaRequired: Boolean = false,
    val captchaChallenge: CaptchaChallenge? = null,
    val captchaAnswer: String = "",
    val captchaError: String? = null,
    val isRateLimited: Boolean = false,
    val lockoutSeconds: Int = 0
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val captchaManager: CaptchaManager,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleRememberMe() {
        _uiState.update { it.copy(rememberMe = !it.rememberMe) }
    }

    fun onCaptchaAnswerChanged(answer: String) {
        _uiState.update { it.copy(captchaAnswer = answer, captchaError = null) }
    }

    fun refreshCaptcha() {
        val challenge = captchaManager.generateChallenge()
        _uiState.update { it.copy(captchaChallenge = challenge, captchaAnswer = "", captchaError = null) }
    }

    fun login(onSuccess: () -> Unit, onVerificationRequired: () -> Unit) {
        val state = _uiState.value
        if (state.isLoading) return

        if (captchaManager.isRateLimited()) {
            val seconds = captchaManager.getRemainingLockoutSeconds()
            _uiState.update {
                it.copy(
                    isRateLimited = true,
                    lockoutSeconds = seconds,
                    errorMessage = "Too many failed attempts. Please wait $seconds seconds."
                )
            }
            return
        }

        val (emailValid, emailError) = AuthValidator.validateEmail(state.email)
        if (!emailValid) {
            _uiState.update { it.copy(errorMessage = emailError) }
            return
        }

        if (state.password.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Password is required") }
            return
        }

        // CAPTCHA verification if required
        if (captchaManager.isCaptchaRequired()) {
            val challenge = state.captchaChallenge ?: captchaManager.generateChallenge()
            if (state.captchaChallenge == null) {
                _uiState.update {
                    it.copy(
                        isCaptchaRequired = true,
                        captchaChallenge = challenge,
                        errorMessage = "Security verification required. Please solve the challenge below."
                    )
                }
                return
            }

            if (!captchaManager.verifyChallenge(state.captchaAnswer, challenge)) {
                captchaManager.recordFailedAttempt()
                val newChallenge = captchaManager.generateChallenge()
                _uiState.update {
                    it.copy(
                        captchaChallenge = newChallenge,
                        captchaAnswer = "",
                        captchaError = "Incorrect answer. Please try again."
                    )
                }
                return
            }
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = authRepository.login(state.email, state.password)
            result.onSuccess { user ->
                captchaManager.recordSuccessfulAttempt()
                _uiState.update { it.copy(isLoading = false, isCaptchaRequired = false, captchaChallenge = null) }
                syncRepository.startSync(manual = true)

                if (user.isEmailVerified) {
                    onSuccess()
                } else {
                    onVerificationRequired()
                }
            }.onFailure { exception ->
                captchaManager.recordFailedAttempt()
                val captchaNeeded = captchaManager.isCaptchaRequired()
                val challenge = if (captchaNeeded) captchaManager.generateChallenge() else null
                val message = AuthValidator.mapFirebaseException(exception)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = message,
                        isCaptchaRequired = captchaNeeded,
                        captchaChallenge = challenge,
                        captchaAnswer = ""
                    )
                }
            }
        }
    }

    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(idToken)
            result.onSuccess {
                captchaManager.recordSuccessfulAttempt()
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

    fun sendPasswordReset(email: String, onResult: (String) -> Unit) {
        val (valid, error) = AuthValidator.validateEmail(email)
        if (!valid) {
            onResult(error ?: "Invalid email")
            return
        }

        viewModelScope.launch {
            authRepository.sendPasswordReset(email)
            onResult("If an account exists for $email, password reset instructions have been sent.")
        }
    }
}
