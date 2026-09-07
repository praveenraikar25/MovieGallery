package com.raikar.moviegallery.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.raikar.moviegallery.ui.components.InitialAvatar
import com.raikar.moviegallery.ui.theme.Sizes
import com.raikar.moviegallery.ui.theme.movieColors

@Composable
fun ProfileScreen(onSignOut: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier =
                Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 22.dp),
        )

        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            InitialAvatar(text = "A", size = Sizes.AvatarLg)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Alex Rivera",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "alex.rivera@email.com",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.movieColors.textMuted,
            )
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.movieColors.border, MaterialTheme.shapes.large),
        ) {
            ProfileRow(label = "Preferences")
            HorizontalDivider(color = MaterialTheme.movieColors.border)
            ProfileRow(label = "Notifications")
            HorizontalDivider(color = MaterialTheme.movieColors.border)
            ProfileRow(label = "About")
            HorizontalDivider(color = MaterialTheme.movieColors.border)
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onSignOut,
                        ).padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(
                    text = "Sign Out",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ProfileRow(label: String) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.movieColors.textMuted,
        )
    }
}
