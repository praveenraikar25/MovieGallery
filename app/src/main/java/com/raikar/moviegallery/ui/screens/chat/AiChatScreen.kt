package com.raikar.moviegallery.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.ui.components.AppIcons
import com.raikar.moviegallery.ui.theme.movieColors

@Composable
fun AiChatScreen(
    onBack: () -> Unit,
    viewModel: AiChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)

    // The typing indicator is a list item of its own, so the count it contributes has
    // to be part of the scroll key — otherwise the indicator appears off-screen.
    val lastItemIndex = uiState.messages.size + (if (uiState.isSending) 1 else 0) - 1

    LaunchedEffect(lastItemIndex) {
        if (lastItemIndex >= 0) {
            listState.animateScrollToItem(lastItemIndex)
        }
    }

    LaunchedEffect(imeBottomPx) {
        if (lastItemIndex >= 0) {
            listState.scrollToItem(lastItemIndex)
        }
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        ChatHeader(onBack = onBack)
        HorizontalDivider(color = MaterialTheme.movieColors.border)

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(uiState.messages, key = { it.id }) { message ->
                ChatBubble(message = message)
            }
            if (uiState.isSending) {
                item(key = TYPING_INDICATOR_KEY) { TypingBubble() }
            }
        }

        uiState.error?.let { error ->
            ChatErrorRow(
                error = error,
                onRetry = viewModel::retry,
                onDismiss = viewModel::dismissError,
            )
        }

        HorizontalDivider(color = MaterialTheme.movieColors.border)
        ChatInputBar(
            value = input,
            onValueChange = { input = it },
            enabled = !uiState.isSending,
            onSend = {
                viewModel.sendMessage(input)
                input = ""
            },
        )
    }
}

@Composable
private fun ChatHeader(onBack: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Box(
            modifier =
                Modifier
                    .size(30.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.SparkleDouble,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = "AI Movie Search",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Ask for a recommendation",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.movieColors.textMuted,
            )
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val shape =
        if (message.isUser) {
            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
        } else {
            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
        }
    val backgroundColor =
        if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.movieColors.surfaceAlt
    val textColor =
        if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier =
                Modifier
                    .widthIn(max = BubbleMaxWidth)
                    .clip(shape)
                    .background(backgroundColor)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
            )
        }
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    onSend: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.movieColors.surfaceAlt)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty()) {
                Text(
                    text = "Message AI Movie Search…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.movieColors.textMuted,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle =
                    LocalTextStyle.current.copy(
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = enabled && value.isNotBlank(),
                        onClick = onSend,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.Send,
                contentDescription = "Send",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** Shown in place of a reply while the model is generating one. */
@Composable
private fun TypingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
    ) {
        Box(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp))
                    .background(MaterialTheme.movieColors.surfaceAlt)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.movieColors.textMuted,
                strokeWidth = 2.dp,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/**
 * A failed send is reported here rather than through the shared
 * [com.raikar.moviegallery.ui.components.ErrorState], which fills the screen — that
 * would hide the conversation, and its copy is written for TMDB failures.
 */
@Composable
private fun ChatErrorRow(
    error: AppError,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = error.chatMessage,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRetry) {
            Text(text = "Retry", style = MaterialTheme.typography.labelMedium)
        }
        TextButton(onClick = onDismiss) {
            Text(
                text = "Dismiss",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.movieColors.textMuted,
            )
        }
    }
}

private val AppError.chatMessage: String
    get() =
        when (this) {
            AppError.Network -> "No connection — check your internet and try again."
            // The likely causes are all setup rather than user error, so name them: an
            // unregistered App Check debug token is the usual one on a fresh checkout.
            AppError.Unauthorized ->
                "Firebase rejected the request. Check that AI Logic is enabled and this device's " +
                    "App Check debug token is registered."
            AppError.Serialization -> "The assistant sent a reply this app couldn't read."
            AppError.NotFound -> "The assistant is unavailable right now."
            is AppError.Http -> "The assistant is unavailable right now (HTTP $code)."
            is AppError.Unknown -> "Couldn't get a reply. Tap retry to try again."
        }

private const val TYPING_INDICATOR_KEY = "typing-indicator"

/** Keeps long replies readable without stretching one-line replies to match. */
private val BubbleMaxWidth = 280.dp
