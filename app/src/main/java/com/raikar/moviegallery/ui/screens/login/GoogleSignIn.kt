package com.raikar.moviegallery.ui.screens.login

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.raikar.moviegallery.R

private const val TAG = "GoogleSignIn"

sealed interface GoogleSignInOutcome {
    data class Token(
        val idToken: String,
    ) : GoogleSignInOutcome

    /** The user dismissed the account chooser. */
    data object Cancelled : GoogleSignInOutcome

    data object Failed : GoogleSignInOutcome
}

/**
 * Shows the Google account chooser and returns the ID token to exchange for a Firebase
 * session. Lives in the UI layer because Credential Manager needs an Activity context.
 *
 * The OAuth web client ID is the `default_web_client_id` string the google-services plugin
 * generates from `google-services.json`.
 */
suspend fun requestGoogleIdToken(context: Context): GoogleSignInOutcome {
    val request =
        GetCredentialRequest
            .Builder()
            .addCredentialOption(
                GetSignInWithGoogleOption.Builder(context.getString(R.string.default_web_client_id)).build(),
            ).build()
    return try {
        val credential = CredentialManager.create(context).getCredential(context, request).credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            GoogleSignInOutcome.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
        } else {
            GoogleSignInOutcome.Failed
        }
    } catch (e: GetCredentialCancellationException) {
        // Also what Credential Manager reports when the OAuth client or SHA-1 is misconfigured,
        // so keep the detail visible in logcat.
        Log.w(TAG, "Google sign-in cancelled", e)
        GoogleSignInOutcome.Cancelled
    } catch (e: GetCredentialException) {
        Log.w(TAG, "Google sign-in failed", e)
        GoogleSignInOutcome.Failed
    } catch (e: GoogleIdTokenParsingException) {
        Log.w(TAG, "Google ID token could not be parsed", e)
        GoogleSignInOutcome.Failed
    }
}
