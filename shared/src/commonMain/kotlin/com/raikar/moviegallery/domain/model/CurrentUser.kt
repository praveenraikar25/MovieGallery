package com.raikar.moviegallery.domain.model

/** The signed-in account's identity as shown in the UI. Either field may be missing. */
data class CurrentUser(
    val email: String?,
    val displayName: String?,
) {
    /** The display name, or null when it is absent or blank. */
    val name: String?
        get() = displayName?.trim()?.takeIf { it.isNotEmpty() }

    /** First letter of [name], else of [email], uppercased; [FALLBACK_INITIAL] when neither has one. */
    val initial: String
        get() =
            (name ?: email?.trim())
                ?.firstOrNull()
                ?.uppercase()
                ?: FALLBACK_INITIAL

    companion object {
        const val FALLBACK_INITIAL = "?"
    }
}
