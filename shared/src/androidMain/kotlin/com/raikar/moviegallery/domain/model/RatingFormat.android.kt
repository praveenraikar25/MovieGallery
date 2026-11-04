package com.raikar.moviegallery.domain.model

actual fun formatRating(rating: Double): String = "%.1f".format(rating)
