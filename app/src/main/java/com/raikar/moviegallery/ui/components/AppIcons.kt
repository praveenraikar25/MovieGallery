package com.raikar.moviegallery.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Bookmark icons are missing from material-icons-core (only present in the much
 * larger -extended artifact), so they're hand-drawn here to match the design's
 * ribbon glyph: `M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-4-7 4V5z` (24x24 viewBox).
 */
object AppIcons {
    val BookmarkFilled: ImageVector by lazy {
        ImageVector
            .Builder(
                name = "BookmarkFilled",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = SolidColor(Color.Black), pathFillType = PathFillType.NonZero) {
                    moveTo(5f, 5f)
                    arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 2f, dy1 = -2f)
                    horizontalLineToRelative(10f)
                    arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 2f, dy1 = 2f)
                    verticalLineToRelative(16f)
                    lineToRelative(-7f, -4f)
                    lineToRelative(-7f, 4f)
                    close()
                }
            }.build()
    }

    val BookmarkOutline: ImageVector by lazy {
        ImageVector
            .Builder(
                name = "BookmarkOutline",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.8f,
                    pathFillType = PathFillType.NonZero,
                ) {
                    moveTo(5f, 5f)
                    arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 2f, dy1 = -2f)
                    horizontalLineToRelative(10f)
                    arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 2f, dy1 = 2f)
                    verticalLineToRelative(16f)
                    lineToRelative(-7f, -4f)
                    lineToRelative(-7f, 4f)
                    close()
                }
            }.build()
    }
}
