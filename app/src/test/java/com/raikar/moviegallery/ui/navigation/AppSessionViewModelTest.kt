package com.raikar.moviegallery.ui.navigation

import com.raikar.moviegallery.domain.usecase.IsSignedInUseCase
import com.raikar.moviegallery.domain.usecase.SignOutUseCase
import com.raikar.moviegallery.testutil.FakeAuthRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AppSessionViewModelTest {
    private fun viewModel(repository: FakeAuthRepository) =
        AppSessionViewModel(IsSignedInUseCase(repository), SignOutUseCase(repository))

    @Test
    fun `starts on login when no user is signed in`() {
        assertEquals(Routes.LOGIN, viewModel(FakeAuthRepository(isSignedIn = false)).startRoute)
    }

    @Test
    fun `starts on main when a user is already signed in`() {
        assertEquals(Routes.MAIN, viewModel(FakeAuthRepository(isSignedIn = true)).startRoute)
    }

    @Test
    fun `sign out ends the session so the next launch starts on login`() {
        val repository = FakeAuthRepository(isSignedIn = true)

        viewModel(repository).signOut()

        assertEquals(1, repository.signOutCalls)
        assertFalse(repository.isSignedIn)
        assertEquals(Routes.LOGIN, viewModel(repository).startRoute)
    }
}
