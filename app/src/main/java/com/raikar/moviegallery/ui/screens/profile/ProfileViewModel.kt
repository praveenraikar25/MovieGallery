package com.raikar.moviegallery.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raikar.moviegallery.domain.usecase.ObserveCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel
    @Inject
    constructor(
        observeCurrentUser: ObserveCurrentUserUseCase,
    ) : ViewModel() {
        val uiState: StateFlow<ProfileUiState> =
            observeCurrentUser()
                .map { user ->
                    if (user == null) {
                        ProfileUiState()
                    } else {
                        ProfileUiState(name = user.name, email = user.email.orEmpty(), initial = user.initial)
                    }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())
    }
