package com.raikar.moviegallery.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
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

    val SparkleDouble: ImageVector by lazy {
        ImageVector
            .Builder(
                name = "SparkleDouble",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = SolidColor(Color.White), pathFillType = PathFillType.NonZero) {
                    moveTo(12f, 3f)
                    lineToRelative(1.8f, 4.6f)
                    lineTo(18f, 9f)
                    lineToRelative(-4.2f, 1.4f)
                    lineTo(12f, 15f)
                    lineToRelative(-1.8f, -4.6f)
                    lineTo(6f, 9f)
                    lineToRelative(4.2f, -1.4f)
                    close()
                }
                path(fill = SolidColor(Color.White), pathFillType = PathFillType.NonZero) {
                    moveTo(19f, 13f)
                    lineToRelative(0.9f, 2.1f)
                    lineTo(22f, 16f)
                    lineToRelative(-2.1f, 0.9f)
                    lineTo(19f, 19f)
                    lineToRelative(-0.9f, -2.1f)
                    lineTo(16f, 16f)
                    lineToRelative(2.1f, -0.9f)
                    close()
                }
            }.build()
    }

    val Send: ImageVector by lazy {
        ImageVector
            .Builder(
                name = "Send",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = SolidColor(Color.White), pathFillType = PathFillType.NonZero) {
                    moveTo(20f, 12f)
                    lineTo(4f, 4f)
                    lineToRelative(6f, 8f)
                    lineToRelative(-6f, 8f)
                    close()
                }
            }.build()
    }

    /**
     * Opens the poster attach sheet from the chat input bar. A plain plus, matching
     * the design's `M12 5v14M5 12h14`.
     */
    val Plus: ImageVector by lazy {
        ImageVector
            .Builder(
                name = "Plus",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.8f,
                    strokeLineCap = StrokeCap.Round,
                ) {
                    moveTo(12f, 5f)
                    verticalLineToRelative(14f)
                    moveTo(5f, 12f)
                    horizontalLineToRelative(14f)
                }
            }.build()
    }

    val Camera: ImageVector by lazy {
        ImageVector
            .Builder(
                name = "Camera",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
                    moveTo(4f, 8f)
                    arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 2f, dy1 = -2f)
                    horizontalLineToRelative(1.2f)
                    lineToRelative(0.9f, -1.5f)
                    arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 0.9f, dy1 = -0.5f)
                    horizontalLineToRelative(6f)
                    arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 0.9f, dy1 = 0.6f)
                    lineToRelative(0.9f, 1.4f)
                    horizontalLineTo(18f)
                    arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 2f, dy1 = 2f)
                    verticalLineToRelative(10f)
                    arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -2f, dy1 = 2f)
                    horizontalLineTo(6f)
                    arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -2f, dy1 = -2f)
                    close()
                }
                path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
                    moveTo(15.4f, 13f)
                    arcToRelative(3.4f, 3.4f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -6.8f, dy1 = 0f)
                    arcToRelative(3.4f, 3.4f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 6.8f, dy1 = 0f)
                    close()
                }
            }.build()
    }

    val Gallery: ImageVector by lazy {
        ImageVector
            .Builder(
                name = "Gallery",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
                    moveTo(6f, 5f)
                    horizontalLineToRelative(12f)
                    arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 2f, dy1 = 2f)
                    verticalLineToRelative(10f)
                    arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -2f, dy1 = 2f)
                    horizontalLineTo(6f)
                    arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -2f, dy1 = -2f)
                    verticalLineTo(7f)
                    arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 2f, dy1 = -2f)
                    close()
                }
                path(fill = SolidColor(Color.Black)) {
                    moveTo(10.4f, 10f)
                    arcToRelative(1.4f, 1.4f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -2.8f, dy1 = 0f)
                    arcToRelative(1.4f, 1.4f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 2.8f, dy1 = 0f)
                    close()
                }
                path(
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.8f,
                    strokeLineCap = StrokeCap.Round,
                ) {
                    moveTo(4f, 16f)
                    lineToRelative(4.5f, -4.5f)
                    arcToRelative(1.5f, 1.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 2.1f, dy1 = 0f)
                    lineTo(15f, 16f)
                }
            }.build()
    }

    /** The row affordance on the attach sheet's options. */
    val ChevronRight: ImageVector by lazy {
        ImageVector
            .Builder(
                name = "ChevronRight",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.8f,
                    strokeLineCap = StrokeCap.Round,
                ) {
                    moveTo(9.5f, 5f)
                    lineToRelative(7f, 7f)
                    lineToRelative(-7f, 7f)
                }
            }.build()
    }
}
