package com.raikar.moviegallery.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.raikar.moviegallery.ui.components.AppIcons
import com.raikar.moviegallery.ui.components.InitialAvatar
import com.raikar.moviegallery.ui.theme.Sizes
import com.raikar.moviegallery.ui.theme.movieColors

@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    onFindMoviesWithAi: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
    ) {
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

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, start = 20.dp, end = 20.dp, bottom = 20.dp),
        ) {
            Text(
                text = "DISCOVER",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.movieColors.textMuted,
                modifier = Modifier.padding(start = 2.dp, bottom = 8.dp),
            )
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.movieColors.border, MaterialTheme.shapes.large)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onFindMoviesWithAi,
                        ).padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBadge(icon = AppIcons.SparkleDouble, size = 34.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Find Movies with AI",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Chat with an assistant for personalized picks",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.movieColors.textMuted,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.movieColors.textMuted,
                )
            }
        }
    }
}

@Composable
private fun IconBadge(
    icon: ImageVector,
    size: Dp,
) {
    Box(
        modifier =
            Modifier
                .size(size)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(size / 2),
        )
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
