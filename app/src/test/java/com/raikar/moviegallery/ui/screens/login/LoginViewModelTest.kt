package com.raikar.moviegallery.ui.screens.login

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginViewModelTest {
    private fun viewModel() = LoginViewModel()

    @Test
    fun `initial state is empty and unauthenticated`() {
        val viewModel = viewModel()

        val state = viewModel.uiState.value
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertNull(state.errorMessage)
        assertFalse(state.isAuthenticated)
    }

    @Test
    fun `onEmailChange updates email and clears error`() {
        val viewModel = viewModel()
        viewModel.onLoginClick() // sets an error first
        assertTrue(viewModel.uiState.value.errorMessage != null)

        viewModel.onEmailChange("admin")

        val state = viewModel.uiState.value
        assertEquals("admin", state.email)
        assertNull(state.errorMessage)
    }

    @Test
    fun `onPasswordChange updates password and clears error`() {
        val viewModel = viewModel()
        viewModel.onLoginClick() // sets an error first
        assertTrue(viewModel.uiState.value.errorMessage != null)

        viewModel.onPasswordChange("admin")

        val state = viewModel.uiState.value
        assertEquals("admin", state.password)
        assertNull(state.errorMessage)
    }

    @Test
    fun `login with valid credentials authenticates the user`() {
        val viewModel = viewModel()
        viewModel.onEmailChange("admin")
        viewModel.onPasswordChange("admin")

        viewModel.onLoginClick()

        val state = viewModel.uiState.value
        assertTrue(state.isAuthenticated)
        assertNull(state.errorMessage)
    }

    @Test
    fun `login with blank email and password shows required error`() {
        val viewModel = viewModel()

        viewModel.onLoginClick()

        val state = viewModel.uiState.value
        assertFalse(state.isAuthenticated)
        assertEquals("Email and password are required", state.errorMessage)
    }

    @Test
    fun `login with blank password shows required error`() {
        val viewModel = viewModel()
        viewModel.onEmailChange("admin")

        viewModel.onLoginClick()

        val state = viewModel.uiState.value
        assertFalse(state.isAuthenticated)
        assertEquals("Email and password are required", state.errorMessage)
    }

    @Test
    fun `login with blank email shows required error`() {
        val viewModel = viewModel()
        viewModel.onPasswordChange("admin")

        viewModel.onLoginClick()

        val state = viewModel.uiState.value
        assertFalse(state.isAuthenticated)
        assertEquals("Email and password are required", state.errorMessage)
    }

    @Test
    fun `login with incorrect credentials shows invalid error`() {
        val viewModel = viewModel()
        viewModel.onEmailChange("wrong")
        viewModel.onPasswordChange("wrong")

        viewModel.onLoginClick()

        val state = viewModel.uiState.value
        assertFalse(state.isAuthenticated)
        assertEquals("Invalid email or password", state.errorMessage)
    }

    @Test
    fun `login with correct email but wrong password shows invalid error`() {
        val viewModel = viewModel()
        viewModel.onEmailChange("admin")
        viewModel.onPasswordChange("wrong")

        viewModel.onLoginClick()

        val state = viewModel.uiState.value
        assertFalse(state.isAuthenticated)
        assertEquals("Invalid email or password", state.errorMessage)
    }
}
