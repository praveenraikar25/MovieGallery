## Why

Sign-in is a placeholder: the login screen accepts only a hardcoded `admin` / `admin`, "Continue with Google" skips authentication entirely, nothing is remembered between launches, and "Sign Out" only navigates. The app is meant to ship to production, so it needs real accounts and a real session. Firebase is already configured in the project (AI Logic, App Check), which makes Firebase Authentication the natural fit.

## What Changes

- Replace the hardcoded credential with real email/password sign-in against Firebase Authentication.
- Add account creation as a "Create account" mode on the same login screen (no new route).
- Make "Continue with Google" a real Google sign-in, backed by the same Firebase account system.
- Remember the signed-in user across launches: the app opens on the main screen when a session exists and on the login screen otherwise.
- Make "Sign Out" end the Firebase session before returning to the login screen.
- Show a loading state while an auth request is in flight, and map failures to specific user-facing messages (wrong credentials, email already in use, weak password, no connection, too many attempts, Google failure).
- **BREAKING**: the `admin` / `admin` credential stops working; "Sign-in is not remembered" is reversed.

**Backends touched:** Firebase Authentication (new). TMDB, BFF and Firebase AI Logic are untouched.

**Resolves "> Note:" gaps:**
- `authentication` > "Sign-in checks a fixed credential" (hardcoded credential, no account creation).
- `authentication` > "Sign-in is not remembered" (no session storage).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `authentication`: replace the fixed credential with real sign-in; add account creation, Google sign-in, loading state, specific error messages and session persistence; sign-out now ends the session; keep "both fields required", "editing clears the error" and "login removed from history".
- `app-navigation`: "App starts at login" becomes "App starts at login or main depending on session".

## Out of scope

- Password reset / "Forgot password".
- Email verification, multi-factor auth, other providers (Apple, Facebook, phone).
- Showing the signed-in user's name, email or photo on Profile or Home; those remain the existing hardcoded placeholders (see `profile` > "Profile shows a placeholder user identity").
- Account deletion, changing email or password.
- Handling a server-side revoked or deleted account beyond the next failed token refresh.
- Per-user data: the watchlist stays in-memory and is not scoped to the account.

## Pre-production debt

None introduced by this change. Manual Firebase console setup (enable Email/Password and Google providers, register debug and release SHA-1 fingerprints, re-download `google-services.json`) is a prerequisite, listed in `tasks.md`.

## Impact

- **Dependencies** (declared in `gradle/libs.versions.toml`, Firebase ones via the existing BOM): `firebase-auth`, AndroidX Credential Manager (`credentials`, `credentials-play-services-auth`) and `googleid`.
- **Config**: `app/google-services.json` must be regenerated after enabling the providers so it carries an OAuth web client; the web client ID is needed to request a Google ID token.
- **Code**: `domain` gains an auth repository interface, an auth error model and one use case per auth action; `data` gains a Firebase-backed implementation; `di/RepositoryModule` gets a binding; `ui/screens/login` (state, ViewModel, screen); `ui/navigation/AppNavHost` (start destination, sign-out); `ui/screens/profile` and its ViewModel wiring for sign-out.
- **Tests**: new fake auth repository in `testutil`; ViewModel tests for login and sign-out/start-destination logic.
- **Docs**: `CLAUDE.md` repo map and the `android-architecture` skill get the auth layer noted.
