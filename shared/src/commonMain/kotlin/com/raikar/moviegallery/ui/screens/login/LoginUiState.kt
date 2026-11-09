package com.raikar.moviegallery.ui.screens.login

enum class LoginMode { SignIn, CreateAccount }

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val mode: LoginMode = LoginMode.SignIn,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false,
)
