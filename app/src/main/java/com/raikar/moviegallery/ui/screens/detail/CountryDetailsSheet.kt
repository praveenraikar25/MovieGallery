package com.raikar.moviegallery.ui.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raikar.moviegallery.domain.model.CountryDetails
import com.raikar.moviegallery.ui.components.AppButton
import com.raikar.moviegallery.ui.components.ButtonVariant
import com.raikar.moviegallery.ui.components.FilmReelSpinner
import com.raikar.moviegallery.ui.theme.PillShape
import com.raikar.moviegallery.ui.theme.movieColors

private const val MISSING_VALUE = "—"

/** Details for one production country, from the Countries GraphQL API. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryDetailsSheet(
    state: CountrySheetState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    // Landscape or large text can make the rows taller than the sheet.
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
        ) {
            val details = state.details
            SheetHeading(
                flag = details?.flag ?: state.country.flag,
                name = details?.name ?: state.country.name,
                nativeName = details?.nativeName,
            )
            when {
                state.isLoading ->
                    SheetMessage(height = 196) { FilmReelSpinner() }
                details != null -> DetailRows(details)
                state.error != null ->
                    SheetMessage(height = 196) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            MutedText("Couldn't load country details.")
                            AppButton(
                                text = "Retry",
                                onClick = onRetry,
                                variant = ButtonVariant.Secondary,
                                shape = PillShape,
                            )
                        }
                    }
                state.notFound ->
                    SheetMessage(height = 120) { MutedText("No details available for this country.") }
            }
        }
    }
}

@Composable
private fun SheetHeading(
    flag: String?,
    name: String,
    nativeName: String?,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (flag != null) {
            Text(text = flag, fontSize = 36.sp, modifier = Modifier.clearAndSetSemantics { })
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (nativeName != null) {
                MutedText(nativeName)
            }
        }
    }
}

@Composable
private fun DetailRows(details: CountryDetails) {
    Column(modifier = Modifier.padding(top = 20.dp)) {
        countryDetailRows(details).forEach { (label, value) ->
            HorizontalDivider(color = MaterialTheme.movieColors.border)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.movieColors.textMuted,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SheetMessage(
    height: Int,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth().height(height.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun MutedText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.movieColors.textMuted,
    )
}

/** The sheet's label/value rows, with "—" for anything the source left out. */
internal fun countryDetailRows(details: CountryDetails): List<Pair<String, String>> =
    listOf(
        "Capital" to (details.capital ?: MISSING_VALUE),
        "Continent" to (details.continent ?: MISSING_VALUE),
        "Languages" to details.languages.joinToString(", ").ifEmpty { MISSING_VALUE },
        "Currency" to details.currencies.joinToString(", ").ifEmpty { MISSING_VALUE },
    )
