package com.raikar.moviegallery.ui.screens.profile

import com.raikar.moviegallery.domain.model.CurrentUser

data class ProfileUiState(
    /** Null when the account has no display name; the name row is then hidden. */
    val name: String? = null,
    val email: String = "",
    val initial: String = CurrentUser.FALLBACK_INITIAL,
)
