package com.raikar.moviegallery.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val MovieShapes =
    Shapes(
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(10.dp),
        large = RoundedCornerShape(12.dp),
    )

val PillShape = RoundedCornerShape(percent = 50)
val AvatarShape = CircleShape
