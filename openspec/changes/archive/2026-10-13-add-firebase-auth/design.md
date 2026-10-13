# Design

## Context

Today `LoginViewModel` has no dependencies and compares against `admin` / `admin`; "Continue with Google" calls `onAuthenticated` directly; `AppNavHost` always starts at `Routes.LOGIN`; Profile's `onSignOut` only navigates. There is no auth layer. Firebase is already wired (google-services plugin, `firebase-bom` 34.x, AI Logic, App Check), so `firebase-auth` joins the same BOM. Backend touched: Firebase Authentication only; no Retrofit stack is involved, so neither `safeApiCall` nor `safeAiCall` applies (see decision 4). Behavior is specified in `specs/authentication` and `specs/app-navigation`; this document covers structure only.

## Goals / Non-Goals

**Goals:**
- Real email/password and Google sign-in through Firebase Auth, following UI -> use case -> repository interface -> data impl.
- Session persistence with a synchronous start-destination decision (no splash).
- Domain layer stays free of `android.*` and Firebase imports.
- Every spec scenario is reachable from a JVM unit test using a fake repository.

**Non-Goals:**
- Password reset, email verification, MFA, other providers.
- Showing the user on Profile/Home, or scoping the watchlist per user.
- Reacting to server-side session revocation while the app is running.

## Decisions

1. **Auth is its own repository, not folded into an existing one.** `domain/repository/AuthRepository` exposes `val isSignedIn: Boolean` (synchronous read), `suspend fun signIn(email, password): AuthResult<Unit>`, `suspend fun signUp(email, password): AuthResult<Unit>`, `suspend fun signInWithGoogle(idToken: String): AuthResult<Unit>`, and `fun signOut()`. Implemented by `data/repository/AuthRepositoryImpl` wrapping `FirebaseAuth`, bound in `RepositoryModule`. One use case per action in `domain/usecase/` (`SignInUseCase`, `SignUpUseCase`, `SignInWithGoogleUseCase`, `SignOutUseCase`, `IsSignedInUseCase`), matching the existing one-class-per-use-case convention. *Alternative:* call `FirebaseAuth` from the ViewModel. Rejected: breaks layering and makes ViewModels untestable without Firebase.

2. **Fetching the Google ID token stays in the UI layer; the repository only receives the token string.** Credential Manager needs an `Activity` context to show the account chooser, which the domain/data layers must not hold. A composable helper in `ui/screens/login` (Credential Manager `GetGoogleIdOption` / "Sign in with Google" button flow, using the generated `default_web_client_id` string resource) returns an ID token, a cancellation, or a failure; the screen forwards the token to `LoginViewModel.onGoogleToken(idToken)`, which calls `SignInWithGoogleUseCase`. The ViewModel exposes `onGoogleCancelled()` (no error) and `onGoogleFailed()` (shows the Google failure message). *Alternative:* inject the Activity into a data-layer class. Rejected: leaks the Activity and breaks the domain boundary. *Alternative:* legacy `GoogleSignInClient`. Rejected: deprecated.

3. **Session persistence relies on Firebase's own persistence.** `FirebaseAuth.currentUser` is restored from local storage and readable synchronously, so `AuthRepository.isSignedIn` is a plain property. A small `@HiltViewModel` `AppSessionViewModel` (scoped to the nav host) exposes `isSignedIn` and `signOut()`; `AppNavHost` reads it once to choose `startDestination`, and Profile's `onSignOut` calls `signOut()` before the existing `popUpTo(0)` navigation. No new Profile ViewModel is needed. *Alternative:* observe an auth-state `Flow` and drive navigation reactively. Rejected for now: needs a loading gate on cold start and adds nothing the specs require; revisit if revocation handling is added.

4. **Errors map to a dedicated `AuthError`, not `AppError`.** `AppError` is shaped around HTTP/TMDB failures (`Unauthorized`, `NotFound`, `Http(code)`) and its copy is mapped elsewhere. Auth needs distinct cases (invalid credentials, email in use, weak password, network, too many requests, unknown). Add `domain/model/AuthError` (sealed interface) and make the auth repository return `AuthResult<T>` (success or `AuthError`), reusing the `DataResult` pattern rather than extending `AppError`. A `safeAuthCall` in `data/remote/auth/` catches `FirebaseAuthInvalidCredentialsException` / `FirebaseAuthInvalidUserException` (-> invalid credentials, deliberately not distinguishing unknown account from wrong password), `FirebaseAuthUserCollisionException`, `FirebaseAuthWeakPasswordException`, `FirebaseNetworkException`, `FirebaseTooManyRequestsException`, and everything else; it rethrows `CancellationException`. The ViewModel maps `AuthError` to the exact strings in the spec. *Alternative:* extend `AppError` with auth cases. Rejected: widens a type every other screen's `when` has to handle.

5. **Client-side validation lives in the ViewModel, ahead of the network call.** Blank check, then email-format check (`android.util.Patterns` is not available in plain JVM tests, so use a small regex in a pure Kotlin helper), then, in create-account mode only, the 6-character minimum. This yields the "Enter a valid email address" result for the old `admin` input and avoids a round trip. Trim the email, never the password. The 6-character rule mirrors Firebase's minimum; the server-side weak-password error is still mapped as a backstop.

6. **`LoginUiState` grows, `isAuthenticated` stays.** Add `mode` (`SignIn` / `CreateAccount`) and `isLoading`; keep `isAuthenticated` as the navigation trigger the screen already observes. `onToggleMode()` flips the mode and clears the error but keeps the typed fields. While `isLoading`, the ViewModel ignores further submits as well as the UI disabling the buttons.

7. **Dependencies** are added to `gradle/libs.versions.toml`, latest stable at implementation time: `firebase-auth` (no version, via the existing BOM), `androidx-credentials`, `androidx-credentials-play-services-auth`, and `googleid` (new version entries). `app/google-services.json` must be regenerated after enabling the providers; the `default_web_client_id` resource is generated from it by the google-services plugin.

## Risks / Trade-offs

- **Console/config drift** (providers disabled, SHA-1 missing, stale `google-services.json`) makes Google sign-in fail with the generic Google failure message -> list the console steps as the first tasks and log the underlying exception in debug builds.
- **Sign-up accepts any email without verification** -> accepted; verification is out of scope and called out in the proposal.
- **Signed-in state vs. in-memory watchlist**: the watchlist is not per-user, so a different user signing in on the same process sees the previous user's list until the app restarts -> accepted for now; noted in out-of-scope (per-user data).
- **Session revoked server-side** is only noticed on the next token refresh, and the UI does not react -> accepted; out of scope.
- **Not distinguishing "unknown account" from "wrong password"** is intentional (avoids account enumeration) but means users get one message for both.
- **Cold-start read of `currentUser`** is synchronous from local storage; if it ever proves slow, add a loading gate (would need a design update).

## Migration Plan

1. Enable Email/Password and Google providers in the Firebase console; register debug (and release) SHA-1 fingerprints; re-download `google-services.json`.
2. Land the change. No data migration; the `admin` / `admin` shortcut disappears.
3. Rollback: revert the change and the `google-services.json` update; no persisted app data depends on it.
