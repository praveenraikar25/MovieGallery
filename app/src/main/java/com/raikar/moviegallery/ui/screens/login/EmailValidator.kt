package com.raikar.moviegallery.ui.screens.login

// Deliberately loose: one "@", no whitespace, and a dot in the domain. The account service
// has the final say; this only catches obvious typos before a network round trip.
private val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

internal fun isValidEmail(email: String): Boolean = EMAIL_PATTERN.matches(email)
