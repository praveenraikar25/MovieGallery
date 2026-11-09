package com.raikar.moviegallery.ui.screens.profile

import com.raikar.moviegallery.domain.model.CurrentUser
import com.raikar.moviegallery.domain.usecase.ObserveCurrentUserUseCase
import com.raikar.moviegallery.testutil.FakeAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    // viewModelScope runs on Dispatchers.Main, which tests replace with a test dispatcher.
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private val auth = FakeAuthRepository(isSignedIn = true)

    /** Collects the state so the WhileSubscribed upstream is active, as it is under a screen. */
    private fun TestScope.viewModel(): ProfileViewModel =
        ProfileViewModel(ObserveCurrentUserUseCase(auth)).also {
            it.uiState.onEach { }.launchIn(backgroundScope)
        }

    @Test
    fun `shows name and email for an account with a display name`() =
        runTest(UnconfinedTestDispatcher()) {
            auth.currentUser.value = CurrentUser("jane@example.com", "Jane Doe")

            val state = viewModel().uiState.value

            assertEquals("Jane Doe", state.name)
            assertEquals("jane@example.com", state.email)
            assertEquals("J", state.initial)
        }

    @Test
    fun `hides the name for an account without a display name`() =
        runTest(UnconfinedTestDispatcher()) {
            auth.currentUser.value = CurrentUser("jane@example.com", null)

            val state = viewModel().uiState.value

            assertNull(state.name)
            assertEquals("jane@example.com", state.email)
            assertEquals("J", state.initial)
        }

    @Test
    fun `treats a blank display name as missing`() =
        runTest(UnconfinedTestDispatcher()) {
            auth.currentUser.value = CurrentUser("sam@example.com", "   ")

            val state = viewModel().uiState.value

            assertNull(state.name)
            assertEquals("S", state.initial)
        }

    @Test
    fun `follows an account switch and never keeps the previous account`() =
        runTest(UnconfinedTestDispatcher()) {
            auth.currentUser.value = CurrentUser("jane@example.com", "Jane Doe")
            val viewModel = viewModel()

            auth.signOut()
            assertEquals(ProfileUiState(), viewModel.uiState.value)

            auth.currentUser.value = CurrentUser("sam@example.com", null)
            val state = viewModel.uiState.value
            assertNull(state.name)
            assertEquals("sam@example.com", state.email)
            assertEquals("S", state.initial)
        }
}
