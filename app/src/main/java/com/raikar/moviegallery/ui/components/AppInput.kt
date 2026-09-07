package com.raikar.moviegallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.raikar.moviegallery.ui.theme.Sizes
import com.raikar.moviegallery.ui.theme.movieColors

@Composable
fun AppInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    leadingIcon: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(Sizes.ControlHeight)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.movieColors.surfaceAlt)
                .border(1.dp, MaterialTheme.movieColors.border, MaterialTheme.shapes.large)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            androidx.compose.foundation.layout
                .Spacer(modifier = Modifier.width(10.dp))
        }
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.movieColors.textMuted,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle =
                    LocalTextStyle.current.copy(
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions =
                    androidx.compose.foundation.text
                        .KeyboardOptions(keyboardType = keyboardType),
                cursorBrush =
                    androidx.compose.ui.graphics
                        .SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
