package com.raikar.moviegallery.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.domain.usecase.GetHomeSectionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val getHomeSections: GetHomeSectionsUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(HomeUiState())
        val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

        init {
            load()
        }

        fun retry() = load()

        private fun load() {
            _uiState.value = HomeUiState(isLoading = true)
            viewModelScope.launch {
                _uiState.value =
                    when (val result = getHomeSections()) {
                        is DataResult.Success -> HomeUiState(isLoading = false, sections = result.data)
                        is DataResult.Failure -> HomeUiState(isLoading = false, error = result.error)
                    }
            }
        }
    }
