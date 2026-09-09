package com.raikar.moviegallery.ui.screens.login

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

private const val VALID_EMAIL = "admin"
private const val VALID_PASSWORD = "admin"

@HiltViewModel
class LoginViewModel
    @Inject
    constructor() : ViewModel() {
        private val _uiState = MutableStateFlow(LoginUiState())
        val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

        fun onEmailChange(value: String) {
            _uiState.value = _uiState.value.copy(email = value, errorMessage = null)
        }

        fun onPasswordChange(value: String) {
            _uiState.value = _uiState.value.copy(password = value, errorMessage = null)
        }

        fun onLoginClick() {
            val state = _uiState.value
            when {
                state.email.isBlank() || state.password.isBlank() -> {
                    _uiState.value = state.copy(errorMessage = "Email and password are required")
                }
                state.email != VALID_EMAIL || state.password != VALID_PASSWORD -> {
                    _uiState.value = state.copy(errorMessage = "Invalid email or password")
                }
                else -> {
                    _uiState.value = state.copy(isAuthenticated = true)
                }
            }
        }
    }
