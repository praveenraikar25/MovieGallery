package com.raikar.moviegallery.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.raikar.moviegallery.ui.theme.MicroTextStyle
import com.raikar.moviegallery.ui.theme.Sizes
import com.raikar.moviegallery.ui.theme.movieColors

enum class TabItem(
    val label: String,
    val durationMs: Int,
) {
    Home("Home", TabMotion.HOME_MS),
    Search("Search", TabMotion.SEARCH_MS),
    Watchlist("Watchlist", TabMotion.WATCHLIST_MS),
    Profile("Profile", TabMotion.PROFILE_MS),
}

private val IconSlotHeight = 34.dp
private const val SELECTED_FILL = 0.22f
private const val SELECTED_PILL_ALPHA = 0.14f

@Composable
private fun rememberMotionEnabled(key: Any?): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(key) {
        TabMotion.animationsEnabled(
            Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f),
        )
    }
}

@Composable
fun MovieTabBar(
    active: TabItem,
    onSelect: (TabItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val motionEnabled = rememberMotionEnabled(active)
    Column(modifier = modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        HorizontalDivider(color = MaterialTheme.movieColors.border)
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(top = 10.dp, bottom = 8.dp),
        ) {
            val tabWidth = maxWidth / TabItem.entries.size
            val pillX by animateDpAsState(
                targetValue = tabWidth * active.ordinal,
                animationSpec =
                    if (motionEnabled) {
                        spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
                    } else {
                        snap()
                    },
                label = "tabPill",
            )
            Box(
                modifier = Modifier.offset(x = pillX).width(tabWidth).height(IconSlotHeight),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(width = 54.dp, height = 30.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = SELECTED_PILL_ALPHA)),
                )
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                TabItem.entries.forEach { tab ->
                    TabBarItem(
                        tab = tab,
                        selected = tab == active,
                        motionEnabled = motionEnabled,
                        onClick = { onSelect(tab) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.TabBarItem(
    tab: TabItem,
    selected: Boolean,
    motionEnabled: Boolean,
    onClick: () -> Unit,
) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.movieColors.inactiveTab
    // 1f is the resting state. The animation only plays when the tab becomes selected after first composition.
    val progress = remember { Animatable(1f) }
    var firstComposition by remember { mutableStateOf(true) }
    LaunchedEffect(selected) {
        val playIt = selected && motionEnabled && !firstComposition
        firstComposition = false
        if (playIt) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(tab.durationMs, easing = LinearEasing))
        } else {
            progress.snapTo(1f)
        }
    }
    val fill by animateFloatAsState(if (selected) SELECTED_FILL else 0f, label = "tabFill")

    Column(
        modifier =
            Modifier
                .weight(1f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.height(IconSlotHeight), contentAlignment = Alignment.Center) {
            TabIcon(
                tab = tab,
                progress = { progress.value },
                fill = { fill },
                color = color,
                size = Sizes.TabIcon,
                modifier = Modifier.semantics { contentDescription = tab.label },
            )
        }
        Text(
            text = tab.label,
            style = MicroTextStyle,
            color = color,
            modifier =
                Modifier.graphicsLayer {
                    // Label settles in as the icon animation plays.
                    val settle = TabMotion.easeOutBack(TabMotion.phase(progress.value, TabMotion.Range(0f, 0.6f)))
                    translationY = 5.dp.toPx() * (1f - settle)
                    alpha = 0.3f + 0.7f * settle.coerceAtMost(1f)
                },
        )
    }
}
