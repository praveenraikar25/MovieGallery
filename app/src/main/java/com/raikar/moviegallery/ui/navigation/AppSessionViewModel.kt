package com.raikar.moviegallery.ui.navigation

import androidx.lifecycle.ViewModel
import com.raikar.moviegallery.domain.usecase.IsSignedInUseCase
import com.raikar.moviegallery.domain.usecase.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * App-level session state for the nav host: where to start, and how to end the session.
 * [isSignedIn] is read once at creation — the session is restored synchronously from
 * local storage — so the start destination is known without a loading state.
 */
@HiltViewModel
class AppSessionViewModel
    @Inject
    constructor(
        isSignedIn: IsSignedInUseCase,
        private val signOutUseCase: SignOutUseCase,
    ) : ViewModel() {
        val startRoute: String = if (isSignedIn()) Routes.MAIN else Routes.LOGIN

        fun signOut() = signOutUseCase()
    }
