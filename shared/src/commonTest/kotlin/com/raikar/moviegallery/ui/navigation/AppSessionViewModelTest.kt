package com.raikar.moviegallery.ui.navigation

import com.raikar.moviegallery.domain.usecase.IsSignedInUseCase
import com.raikar.moviegallery.domain.usecase.SignOutUseCase
import com.raikar.moviegallery.testutil.FakeAuthRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppSessionViewModelTest {
    private fun viewModel(repository: FakeAuthRepository) =
        AppSessionViewModel(IsSignedInUseCase(repository), SignOutUseCase(repository))

    @Test
    fun `starts on login when no user is signed in`() {
        assertFalse(viewModel(FakeAuthRepository(isSignedIn = false)).isSignedIn)
    }

    @Test
    fun `starts on main when a user is already signed in`() {
        assertTrue(viewModel(FakeAuthRepository(isSignedIn = true)).isSignedIn)
    }

    @Test
    fun `sign out ends the session so the next launch starts on login`() {
        val repository = FakeAuthRepository(isSignedIn = true)

        viewModel(repository).signOut()

        assertEquals(1, repository.signOutCalls)
        assertFalse(repository.isSignedIn)
        assertFalse(viewModel(repository).isSignedIn)
    }
}
