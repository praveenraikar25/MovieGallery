package com.raikar.moviegallery.ui.screens.login

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false,
)
