package com.raikar.moviegallery.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raikar.moviegallery.domain.model.AuthError
import com.raikar.moviegallery.domain.model.AuthResult
import com.raikar.moviegallery.domain.usecase.SignInUseCase
import com.raikar.moviegallery.domain.usecase.SignInWithGoogleUseCase
import com.raikar.moviegallery.domain.usecase.SignUpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MIN_PASSWORD_LENGTH = 6

@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        private val signIn: SignInUseCase,
        private val signUp: SignUpUseCase,
        private val signInWithGoogle: SignInWithGoogleUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(LoginUiState())
        val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

        fun onEmailChange(value: String) {
            _uiState.value = _uiState.value.copy(email = value, errorMessage = null)
        }

        fun onPasswordChange(value: String) {
            _uiState.value = _uiState.value.copy(password = value, errorMessage = null)
        }

        fun onToggleMode() {
            val state = _uiState.value
            if (state.isLoading) return
            val next = if (state.mode == LoginMode.SignIn) LoginMode.CreateAccount else LoginMode.SignIn
            _uiState.value = state.copy(mode = next, errorMessage = null)
        }

        fun onSubmit() {
            val state = _uiState.value
            if (state.isLoading) return
            val email = state.email.trim()
            val validationError =
                when {
                    email.isEmpty() || state.password.isBlank() -> "Email and password are required"
                    !isValidEmail(email) -> "Enter a valid email address"
                    state.mode == LoginMode.CreateAccount && state.password.length < MIN_PASSWORD_LENGTH ->
                        "Password must be at least 6 characters"
                    else -> null
                }
            if (validationError != null) {
                _uiState.value = state.copy(errorMessage = validationError)
                return
            }
            _uiState.value = state.copy(isLoading = true, errorMessage = null)
            viewModelScope.launch {
                val result =
                    if (state.mode == LoginMode.SignIn) {
                        signIn(email, state.password)
                    } else {
                        signUp(email, state.password)
                    }
                finish(result)
            }
        }

        fun onGoogleToken(idToken: String) {
            val state = _uiState.value
            if (state.isLoading) return
            _uiState.value = state.copy(isLoading = true, errorMessage = null)
            viewModelScope.launch { finish(signInWithGoogle(idToken), googleFailureMessage = GOOGLE_FAILED) }
        }

        /** The user dismissed the Google account chooser: stay put, show nothing. */
        fun onGoogleCancelled() = Unit

        fun onGoogleFailed() {
            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = GOOGLE_FAILED)
        }

        private fun finish(
            result: AuthResult<Unit>,
            googleFailureMessage: String? = null,
        ) {
            _uiState.value =
                when (result) {
                    is AuthResult.Success -> _uiState.value.copy(isLoading = false, isAuthenticated = true)
                    is AuthResult.Failure ->
                        _uiState.value.copy(
                            isLoading = false,
                            errorMessage = googleFailureMessage ?: messageFor(result.error),
                        )
                }
        }

        private fun messageFor(error: AuthError): String =
            when (error) {
                AuthError.InvalidCredentials -> "Invalid email or password"
                AuthError.EmailInUse -> "An account with this email already exists"
                AuthError.WeakPassword -> "Password must be at least 6 characters"
                AuthError.Network -> "No internet connection. Check your network and try again."
                AuthError.TooManyRequests -> "Too many attempts. Try again later."
                is AuthError.Unknown -> "Something went wrong. Please try again."
            }

        private companion object {
            const val GOOGLE_FAILED = "Google sign-in failed. Please try again."
        }
    }
