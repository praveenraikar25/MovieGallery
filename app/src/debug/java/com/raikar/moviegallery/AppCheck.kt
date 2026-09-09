package com.raikar.moviegallery

import android.app.Application
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug builds attest with a locally generated token. It is printed to Logcat on
 * first launch (filter for "DebugAppCheckProvider") and has to be registered once
 * under App Check → Manage debug tokens in the Firebase console; until then Gemini
 * calls fail and surface as [com.raikar.moviegallery.domain.model.AppError.Unauthorized].
 */
internal fun Application.installAppCheck() {
    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
        DebugAppCheckProviderFactory.getInstance(),
    )
}
