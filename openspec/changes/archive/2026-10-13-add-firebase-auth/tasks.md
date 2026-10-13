# Tasks

## 1. Firebase console and dependencies

- [x] 1.1 Manual (user): in the Firebase console enable the Email/Password and Google sign-in providers, register the debug (and release) SHA-1 fingerprints, and re-download `app/google-services.json`; verify the file now contains an OAuth client of type 3 (web client)
- [x] 1.2 Add `firebase-auth` (BOM-managed), `androidx-credentials`, `androidx-credentials-play-services-auth` and `googleid` to `gradle/libs.versions.toml` at their latest stable versions and wire them into `app/build.gradle.kts`; verify `./gradlew assembleDebug` succeeds and `R.string.default_web_client_id` resolves

## 2. Domain and data layers

- [x] 2.1 Add `domain/model/AuthError` (invalid credentials, email in use, weak password, network, too many requests, unknown) and `AuthResult`; verify it compiles with no `android.*` or Firebase imports in `domain/`
- [x] 2.2 Add `domain/repository/AuthRepository` and the use cases `SignInUseCase`, `SignUpUseCase`, `SignInWithGoogleUseCase`, `SignOutUseCase`, `IsSignedInUseCase`; verify each is a single class wrapping one repository call
- [x] 2.3 Add `data/remote/auth/safeAuthCall` mapping Firebase auth exceptions to `AuthError` (rethrowing `CancellationException`) and `data/repository/AuthRepositoryImpl` over `FirebaseAuth`; bind it in `di/RepositoryModule`; verify a JVM test of the exception-to-`AuthError` mapping passes for each case (invalid credentials and unknown user both map to invalid credentials)
- [x] 2.3a Add `FakeAuthRepository` to `app/src/test/.../testutil/` with scriptable results and call recording; verify it is used by the tests in groups 3 and 4

## 3. Login screen

- [x] 3.1 Extend `LoginUiState` with `mode` and `isLoading`; add a pure-Kotlin email-format validator; verify validator unit tests cover valid, malformed and the old `admin` input
- [x] 3.2 Rewrite `LoginViewModel` to inject the use cases: blank check, email-format check, 6-character minimum in create-account mode, `onToggleMode()`, loading guard against double submits, `AuthError` to message mapping, and `onGoogleToken` / `onGoogleCancelled` / `onGoogleFailed`; verify `LoginViewModelTest` (rewritten with `FakeAuthRepository`) covers every scenario in `specs/authentication/spec.md`
- [x] 3.3 Update `LoginScreen`: mode-aware primary button ("Log In" / "Create Account") and toggle text, progress indicator, disabled controls while loading, and "Continue with Google" wired to the Credential Manager helper (ID token, cancellation, failure); verify on a device or emulator that sign-up, sign-in and Google sign-in reach the main screen and that a dismissed Google chooser shows no error
- [x] 3.4 Update the login screen preview/state tests if any depend on the old `isAuthenticated`-only state; verify `./gradlew :app:testDebugUnitTest --tests "*LoginViewModelTest"` passes

## 4. Session and navigation

- [x] 4.1 Add `AppSessionViewModel` (`isSignedIn`, `signOut()`) and use it in `AppNavHost` to pick `startDestination` (`Routes.MAIN` when signed in, else `Routes.LOGIN`); verify `AppSessionViewModelTest` covers signed-in and signed-out starts using `FakeAuthRepository`
- [x] 4.2 Make Profile's `onSignOut` call `AppSessionViewModel.signOut()` before the existing `popUpTo(0)` navigation to login; verify the sign-out test asserts the fake repository recorded a sign-out and that on a device the app reopens on login after sign-out and on Home after signing in and relaunching

## 5. Docs and final checks

- [x] 5.1 Update `CLAUDE.md` (repo map: auth repository, use cases, `data/remote/auth`, Firebase Auth as a fourth backend, build prerequisite for the providers and `google-services.json`) and `.claude/skills/android-architecture/SKILL.md` if it lists layers or backends; verify both mention the auth layer and agree
- [x] 5.2 Run `./gradlew ktlintFormat` then `./gradlew ktlintCheck`, `./gradlew test` and `./gradlew assembleDebug`; verify all pass

## Workflow follow-up

- Archive the change after review (`/opsx:archive`) once `./gradlew test` passes, so the `authentication` and `app-navigation` specs are updated.
