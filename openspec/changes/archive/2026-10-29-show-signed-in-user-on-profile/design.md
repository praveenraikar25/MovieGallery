# Design

## Context

Firebase Auth is reached only through `AuthRepository`, which today exposes `isSignedIn` and a `currentUserId` flow
fed by an `AuthStateListener` (see `AuthRepositoryImpl`). Email and display name never leave the data layer.
`ProfileScreen` has no ViewModel; `HomeScreen` renders `InitialAvatar(text = "A")` and its `HomeViewModel` only
loads movie sections. Backend: Firebase Auth only. See proposal.md for motivation and scope.

## Goals / Non-Goals

**Goals:**
- One source of truth for the account's identity, observable so screens follow sign-out and account switches.
- Profile and Home header derive name/email/initial from that one source.

**Non-Goals:**
- Writing or editing the display name, photo URL, or any Firebase profile data.
- Persisting identity in Room; Firebase already restores the user at cold start.

## Decisions

1. **Expose a `currentUser: Flow<CurrentUser?>` on `AuthRepository`, alongside `currentUserId`.**
   `CurrentUser(email: String?, displayName: String?)` lives in `domain/model` (pure Kotlin, no `android.*`).
   `AuthRepositoryImpl` builds it from the same `AuthStateListener` pattern and `distinctUntilChanged()`.
   *Alternative:* a one-shot `currentUser` getter. Rejected: stale after an account switch, and the Home/Profile
   tabs stay alive across sign-out/sign-in in the back stack.
   *Alternative:* widen `currentUserId` to return the user. Rejected: it would touch `WatchlistRepositoryImpl`
   and its tests for no benefit.

2. **Display rules live in the domain model, as pure properties.**
   `CurrentUser.name` returns the trimmed display name or `null` when null/blank; `CurrentUser.initial` returns the
   uppercased first character of `name`, else of the email, else `"?"`. Keeping this out of Compose makes every
   spec scenario a plain JVM test. No name is derived from the email (decided in exploration).

3. **`ObserveCurrentUserUseCase` wraps the flow; two ViewModels consume it.**
   - New `ProfileViewModel` (`@HiltViewModel`) exposes `StateFlow<ProfileUiState>` (`name: String?`,
     `email: String`, `initial: String`) via `stateIn(WhileSubscribed(5_000))`. `ProfileScreen` takes
     `hiltViewModel()` and renders the name `Text` only when `name != null`.
   - `HomeViewModel` gains a separate `avatarInitial: StateFlow<String>` rather than a field on `HomeUiState`,
     because `load()` rebuilds `HomeUiState` from scratch on every retry and would reset it.

4. **Signed-out emission.** `currentUser` emits `null` after sign-out. The ViewModels map that to an empty email and
   the `"?"` initial; the user is navigated to login at the same moment, so this is never meaningfully visible.

## Risks / Trade-offs

- [Firebase user object can be momentarily null at startup] → the listener fires with the restored user on
  registration; state starts from a neutral initial and settles on the first emission.
- [Google accounts may omit an email] → `email` is nullable in the model; the row shows an empty string rather than
  crashing, and the initial falls back as above.
- [Home avatar now depends on auth state, adding a constructor dependency to `HomeViewModel`] → existing
  `HomeViewModelTest` setup must pass a `FakeAuthRepository`-backed use case.
