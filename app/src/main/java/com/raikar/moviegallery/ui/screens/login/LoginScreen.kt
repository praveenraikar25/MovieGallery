package com.raikar.moviegallery.ui.screens.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raikar.moviegallery.R
import com.raikar.moviegallery.ui.components.AppButton
import com.raikar.moviegallery.ui.components.AppInput
import com.raikar.moviegallery.ui.components.ButtonVariant
import com.raikar.moviegallery.ui.theme.movieColors

@Composable
fun LoginScreen(
    onAuthenticated: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onAuthenticated()
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Marquee",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Discover movies you'll love.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.movieColors.textMuted,
            modifier = Modifier.padding(top = 6.dp),
        )

        Spacer(modifier = Modifier.height(28.dp))

        AppInput(
            value = uiState.email,
            onValueChange = viewModel::onEmailChange,
            placeholder = "Email",
            keyboardType = KeyboardType.Email,
        )
        Spacer(modifier = Modifier.height(12.dp))
        AppInput(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            placeholder = "Password",
            isPassword = true,
        )
        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        AppButton(text = "Log In", onClick = viewModel::onLoginClick, variant = ButtonVariant.Primary)

        Spacer(modifier = Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.movieColors.border)
            Text(
                text = "OR",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.movieColors.textMuted,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.movieColors.border)
        }
        Spacer(modifier = Modifier.height(20.dp))

        AppButton(
            text = "Continue with Google",
            onClick = onAuthenticated,
            variant = ButtonVariant.Secondary,
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_google_g),
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.Unspecified,
                )
            },
        )
    }
}
