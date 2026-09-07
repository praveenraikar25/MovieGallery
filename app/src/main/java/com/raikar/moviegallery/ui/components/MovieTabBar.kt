package com.raikar.moviegallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.raikar.moviegallery.ui.theme.MicroTextStyle
import com.raikar.moviegallery.ui.theme.Sizes
import com.raikar.moviegallery.ui.theme.movieColors

enum class TabItem(
    val label: String,
    val icon: ImageVector,
) {
    Home("Home", Icons.Outlined.Home),
    Search("Search", Icons.Outlined.Search),
    Watchlist("Watchlist", AppIcons.BookmarkOutline),
    Profile("Profile", Icons.Outlined.AccountCircle),
}

@Composable
fun MovieTabBar(
    active: TabItem,
    onSelect: (TabItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        HorizontalDivider(color = MaterialTheme.movieColors.border)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(top = 10.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            TabItem.entries.forEach { tab ->
                TabBarItem(
                    tab = tab,
                    selected = tab == active,
                    onClick = { onSelect(tab) },
                )
            }
        }
    }
}

@Composable
private fun RowScope.TabBarItem(
    tab: TabItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.movieColors.inactiveTab
    Column(
        modifier =
            Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = color,
            modifier = Modifier.size(Sizes.TabIcon),
        )
        Text(text = tab.label, style = MicroTextStyle, color = color)
    }
}
