package com.raikar.moviegallery.ui.screens.login

import com.raikar.moviegallery.domain.model.AuthError
import com.raikar.moviegallery.domain.model.AuthResult
import com.raikar.moviegallery.domain.usecase.SignInUseCase
import com.raikar.moviegallery.domain.usecase.SignInWithGoogleUseCase
import com.raikar.moviegallery.domain.usecase.SignUpUseCase
import com.raikar.moviegallery.testutil.FakeAuthRepository
import com.raikar.moviegallery.testutil.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository()

    private fun viewModel() =
        LoginViewModel(
            signIn = SignInUseCase(repository),
            signUp = SignUpUseCase(repository),
            signInWithGoogle = SignInWithGoogleUseCase(repository),
        )

    private fun LoginViewModel.fill(
        email: String,
        password: String,
    ) {
        onEmailChange(email)
        onPasswordChange(password)
    }

    private fun failWith(error: AuthError) {
        repository.nextResult = AuthResult.Failure(error)
    }

    @Test
    fun `initial state is empty, sign-in mode and unauthenticated`() {
        val state = viewModel().uiState.value

        assertEquals("", state.email)
        assertEquals("", state.password)
        assertEquals(LoginMode.SignIn, state.mode)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertFalse(state.isAuthenticated)
    }

    @Test
    fun `editing a field clears the error`() {
        val viewModel = viewModel()
        viewModel.onSubmit()
        assertTrue(viewModel.uiState.value.errorMessage != null)

        viewModel.onEmailChange("a")
        assertNull(viewModel.uiState.value.errorMessage)

        viewModel.onSubmit()
        viewModel.onPasswordChange("b")
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `blank email or password shows the required error in both modes`() {
        val viewModel = viewModel()
        viewModel.onSubmit()
        assertEquals("Email and password are required", viewModel.uiState.value.errorMessage)

        viewModel.fill("alex@example.com", "")
        viewModel.onSubmit()
        assertEquals("Email and password are required", viewModel.uiState.value.errorMessage)

        viewModel.onToggleMode()
        viewModel.fill("", "secret1")
        viewModel.onSubmit()
        assertEquals("Email and password are required", viewModel.uiState.value.errorMessage)
        assertTrue(repository.signInCalls.isEmpty() && repository.signUpCalls.isEmpty())
    }

    @Test
    fun `malformed email is rejected without calling the service`() {
        val viewModel = viewModel()
        viewModel.fill("not-an-email", "secret1")

        viewModel.onSubmit()

        assertEquals("Enter a valid email address", viewModel.uiState.value.errorMessage)
        assertTrue(repository.signInCalls.isEmpty())
        viewModel.onToggleMode()
        viewModel.onSubmit()
        assertEquals("Enter a valid email address", viewModel.uiState.value.errorMessage)
        assertTrue(repository.signUpCalls.isEmpty())
    }

    @Test
    fun `old placeholder credential is rejected as a malformed email`() {
        val viewModel = viewModel()
        viewModel.fill("admin", "admin")

        viewModel.onSubmit()

        assertEquals("Enter a valid email address", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isAuthenticated)
    }

    @Test
    fun `valid sign-in authenticates the user`() {
        val viewModel = viewModel()
        viewModel.fill("  alex@example.com ", "secret")

        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertTrue(state.isAuthenticated)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(listOf("alex@example.com" to "secret"), repository.signInCalls)
    }

    @Test
    fun `wrong password or unknown account shows the invalid error and stays`() {
        failWith(AuthError.InvalidCredentials)
        val viewModel = viewModel()
        viewModel.fill("alex@example.com", "wrong")

        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertFalse(state.isAuthenticated)
        assertFalse(state.isLoading)
        assertEquals("Invalid email or password", state.errorMessage)
    }

    @Test
    fun `toggle switches mode and clears the error but keeps the fields`() {
        val viewModel = viewModel()
        viewModel.fill("alex@example.com", "")
        viewModel.onSubmit()
        assertTrue(viewModel.uiState.value.errorMessage != null)

        viewModel.onToggleMode()
        assertEquals(LoginMode.CreateAccount, viewModel.uiState.value.mode)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals("alex@example.com", viewModel.uiState.value.email)

        viewModel.onToggleMode()
        assertEquals(LoginMode.SignIn, viewModel.uiState.value.mode)
    }

    @Test
    fun `sign-up with a valid email and password creates the account`() {
        val viewModel = viewModel()
        viewModel.onToggleMode()
        viewModel.fill("new@example.com", "secret1")

        viewModel.onSubmit()

        assertTrue(viewModel.uiState.value.isAuthenticated)
        assertEquals(listOf("new@example.com" to "secret1"), repository.signUpCalls)
        assertTrue(repository.signInCalls.isEmpty())
    }

    @Test
    fun `sign-up with a short password is rejected locally`() {
        val viewModel = viewModel()
        viewModel.onToggleMode()
        viewModel.fill("new@example.com", "12345")

        viewModel.onSubmit()

        assertEquals("Password must be at least 6 characters", viewModel.uiState.value.errorMessage)
        assertTrue(repository.signUpCalls.isEmpty())
    }

    @Test
    fun `sign-in does not apply the minimum password length`() {
        val viewModel = viewModel()
        viewModel.fill("alex@example.com", "12345")

        viewModel.onSubmit()

        assertEquals(1, repository.signInCalls.size)
    }

    @Test
    fun `sign-up with an existing email shows the already-exists error`() {
        failWith(AuthError.EmailInUse)
        val viewModel = viewModel()
        viewModel.onToggleMode()
        viewModel.fill("taken@example.com", "secret1")

        viewModel.onSubmit()

        assertEquals("An account with this email already exists", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isAuthenticated)
    }

    @Test
    fun `server-side weak password shows the minimum length message`() {
        failWith(AuthError.WeakPassword)
        val viewModel = viewModel()
        viewModel.onToggleMode()
        viewModel.fill("new@example.com", "secret1")

        viewModel.onSubmit()

        assertEquals("Password must be at least 6 characters", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `network, rate limit and unknown failures map to specific messages and keep the fields`() {
        val cases =
            listOf(
                AuthError.Network to "No internet connection. Check your network and try again.",
                AuthError.TooManyRequests to "Too many attempts. Try again later.",
                AuthError.Unknown(IllegalStateException()) to "Something went wrong. Please try again.",
            )
        for ((error, message) in cases) {
            failWith(error)
            val viewModel = viewModel()
            viewModel.fill("alex@example.com", "secret")

            viewModel.onSubmit()

            assertEquals(message, viewModel.uiState.value.errorMessage)
            assertEquals("alex@example.com", viewModel.uiState.value.email)
            assertEquals("secret", viewModel.uiState.value.password)
        }
    }

    @Test
    fun `request in flight shows loading and ignores repeat submits and toggles`() {
        val gate = CompletableDeferred<Unit>()
        repository.gate = gate
        val viewModel = viewModel()
        viewModel.fill("alex@example.com", "secret")

        viewModel.onSubmit()
        assertTrue(viewModel.uiState.value.isLoading)
        viewModel.onSubmit()
        viewModel.onToggleMode()
        assertEquals(1, repository.signInCalls.size)
        assertEquals(LoginMode.SignIn, viewModel.uiState.value.mode)

        gate.complete(Unit)

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.isAuthenticated)
    }

    @Test
    fun `failed request stops loading and shows the error`() {
        val gate = CompletableDeferred<Unit>()
        repository.gate = gate
        failWith(AuthError.Network)
        val viewModel = viewModel()
        viewModel.fill("alex@example.com", "secret")

        viewModel.onSubmit()
        assertTrue(viewModel.uiState.value.isLoading)
        gate.complete(Unit)

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.errorMessage != null)
    }

    @Test
    fun `google token signs the user in`() {
        val viewModel = viewModel()

        viewModel.onGoogleToken("id-token")

        assertTrue(viewModel.uiState.value.isAuthenticated)
        assertEquals(listOf("id-token"), repository.googleCalls)
    }

    @Test
    fun `cancelled google chooser changes nothing and shows no error`() {
        val viewModel = viewModel()

        viewModel.onGoogleCancelled()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertFalse(state.isAuthenticated)
        assertFalse(state.isLoading)
    }

    @Test
    fun `google failure shows the google message`() {
        val viewModel = viewModel()
        viewModel.onGoogleFailed()
        assertEquals("Google sign-in failed. Please try again.", viewModel.uiState.value.errorMessage)

        failWith(AuthError.InvalidCredentials)
        val second = viewModel()
        second.onGoogleToken("bad-token")
        assertEquals("Google sign-in failed. Please try again.", second.uiState.value.errorMessage)
        assertFalse(second.uiState.value.isAuthenticated)
    }
}
