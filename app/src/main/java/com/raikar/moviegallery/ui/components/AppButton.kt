package com.raikar.moviegallery.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.raikar.moviegallery.ui.theme.Sizes
import com.raikar.moviegallery.ui.theme.movieColors

enum class ButtonVariant { Primary, Secondary, Ghost, Destructive }

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    shape: Shape = MaterialTheme.shapes.large,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val background: Color
    val contentColor: Color
    val border: BorderStroke?

    when (variant) {
        ButtonVariant.Primary -> {
            background = MaterialTheme.colorScheme.primary
            contentColor = MaterialTheme.colorScheme.onPrimary
            border = null
        }
        ButtonVariant.Secondary -> {
            background = MaterialTheme.colorScheme.surface
            contentColor = MaterialTheme.colorScheme.onSurface
            border = BorderStroke(1.dp, MaterialTheme.movieColors.border)
        }
        ButtonVariant.Ghost -> {
            background = Color.Transparent
            contentColor = MaterialTheme.colorScheme.primary
            border = null
        }
        ButtonVariant.Destructive -> {
            background = Color.Transparent
            contentColor = MaterialTheme.colorScheme.error
            border = null
        }
    }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(Sizes.ControlHeight)
                .clip(shape)
                .background(background)
                .then(if (border != null) Modifier.border(border, shape) else Modifier)
                .clickable(enabled = enabled, onClick = onClick)
                .alpha(if (enabled) 1f else 0.5f),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
            )
        }
    }
}
