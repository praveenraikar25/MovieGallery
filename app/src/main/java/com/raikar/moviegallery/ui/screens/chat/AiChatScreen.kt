package com.raikar.moviegallery.ui.screens.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
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
    var showPosterSheet by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val context = LocalContext.current

    // Stored as a String so it survives process death: the camera app is heavy enough
    // to have this activity recreated behind it, and TakePicture hands back only a
    // success flag — the destination has to be remembered on this side.
    var pendingCaptureUri by rememberSaveable { mutableStateOf<String?>(null) }

    // The photo picker needs no permission at all, and ACTION_IMAGE_CAPTURE is served
    // by the camera app, so neither of these launchers is gated on a runtime grant.
    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) viewModel.sendPoster(uri.toString())
        }
    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
            val captureUri = pendingCaptureUri
            pendingCaptureUri = null
            if (saved && captureUri != null) viewModel.sendPoster(captureUri)
        }

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
                item(key = TYPING_INDICATOR_KEY) {
                    TypingBubble(label = if (uiState.isIdentifyingPoster) "Identifying poster…" else null)
                }
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
            onAttachClick = { showPosterSheet = true },
            onSend = {
                viewModel.sendMessage(input)
                input = ""
            },
        )
    }

    if (showPosterSheet) {
        PosterSourceSheet(
            onDismiss = { showPosterSheet = false },
            onTakePhoto = {
                showPosterSheet = false
                val captureUri = createPosterCaptureUri(context)
                pendingCaptureUri = captureUri.toString()
                cameraLauncher.launch(captureUri)
            },
            onChooseFromGallery = {
                showPosterSheet = false
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
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
        // An attached image is the bubble, not something inside one: it gets the
        // bubble's shape but none of its padding or fill, so no background peeks out
        // around the poster.
        if (message.imageUri != null) {
            AsyncImage(
                model = message.imageUri,
                contentDescription = "Poster you sent",
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .width(PosterBubbleWidth)
                        .aspectRatio(POSTER_ASPECT_RATIO)
                        .clip(shape)
                        .background(MaterialTheme.movieColors.surfaceAlt),
            )
            return@Row
        }

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
    onAttachClick: () -> Unit,
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
        CircularIconButton(
            icon = AppIcons.Plus,
            contentDescription = "Add a movie poster",
            background = MaterialTheme.movieColors.surfaceAlt,
            tint = MaterialTheme.movieColors.textMuted,
            enabled = enabled,
            onClick = onAttachClick,
        )
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
        CircularIconButton(
            icon = AppIcons.Send,
            contentDescription = "Send",
            background = MaterialTheme.colorScheme.primary,
            tint = MaterialTheme.colorScheme.onPrimary,
            enabled = enabled && value.isNotBlank(),
            onClick = onSend,
        )
    }
}

/**
 * The 36.dp circular control the input bar is built from. Disabled is shown by
 * fading the whole button rather than recolouring it, which is what the design does
 * for the attach button while a reply is in flight.
 */
@Composable
private fun CircularIconButton(
    icon: ImageVector,
    contentDescription: String,
    background: Color,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(InputControlSize)
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .clip(CircleShape)
                .background(background)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = enabled,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
    }
}

/**
 * Shown in place of a reply while the model is generating one. [label] names the
 * wait when it is longer than a text reply — identifying a poster has to upload the
 * image first, so silence there reads as a hang.
 */
@Composable
private fun TypingBubble(label: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
    ) {
        Row(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp))
                    .background(MaterialTheme.movieColors.surfaceAlt)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.movieColors.textMuted,
                strokeWidth = 2.dp,
                modifier = Modifier.size(14.dp),
            )
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.movieColors.textMuted,
                )
            }
        }
    }
}

/**
 * The poster source chooser. A sheet rather than two icons in the input bar: the bar
 * is already three controls wide at 36.dp each, and the sheet has room to say what
 * the image is for, which the icons could not.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PosterSourceSheet(
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onChooseFromGallery: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Add a movie poster",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "I'll identify the title from the image",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.movieColors.textMuted,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            PosterSourceRow(
                icon = AppIcons.Camera,
                label = "Take Photo",
                onClick = onTakePhoto,
            )
            PosterSourceRow(
                icon = AppIcons.Gallery,
                label = "Choose from Gallery",
                onClick = onChooseFromGallery,
            )
            Text(
                text = "Cancel",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.movieColors.textMuted,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .padding(top = 4.dp)
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.movieColors.surfaceAlt)
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 13.dp),
            )
        }
    }
}

@Composable
private fun PosterSourceRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.movieColors.surfaceAlt)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(34.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(17.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = AppIcons.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.movieColors.textMuted,
            modifier = Modifier.size(16.dp),
        )
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

/** Wide enough to recognise the poster, narrow enough to stay a chat bubble. */
private val PosterBubbleWidth = 120.dp

/** Standard poster proportions, so a photographed poster is not letterboxed. */
private const val POSTER_ASPECT_RATIO = 2f / 3f

private val InputControlSize = 36.dp

private const val DISABLED_ALPHA = 0.5f
