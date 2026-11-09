package com.raikar.moviegallery.ui.navigation

import androidx.lifecycle.ViewModel
import com.raikar.moviegallery.domain.usecase.IsSignedInUseCase
import com.raikar.moviegallery.domain.usecase.SignOutUseCase

/**
 * App-level session state: whether a session exists at launch, and how to end it.
 * [isSignedIn] is read once at creation — the session is restored synchronously from
 * local storage — so the start destination is known without a loading state. Each platform
 * maps it to its own start route (Android: `AppNavHost`).
 *
 * Resolve it outside the `NavHost` (as `AppNavHost`'s default parameter does) so the activity
 * owns it. Resolved inside a destination it would belong to that back-stack entry: sign-out's
 * `popUpTo(0)` would clear it and the next instance could hand the `NavHost` a different start route.
 */
class AppSessionViewModel(
    isSignedInUseCase: IsSignedInUseCase,
    private val signOutUseCase: SignOutUseCase,
) : ViewModel() {
    val isSignedIn: Boolean = isSignedInUseCase()

    fun signOut() = signOutUseCase()
}
