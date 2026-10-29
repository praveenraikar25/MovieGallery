## 1. Domain

- [x] 1.1 Add `CurrentUser(email, displayName)` to `domain/model` with `name` (trimmed, null when blank) and `initial` (name, else email, else "?", uppercased) properties
- [x] 1.2 Add `currentUser: Flow<CurrentUser?>` to `domain/repository/AuthRepository`

## 2. Data

- [x] 2.1 Implement `currentUser` in `data/repository/AuthRepositoryImpl` using `AuthStateListener` + `distinctUntilChanged()`, mapping `FirebaseUser.email` / `displayName`
- [x] 2.2 Add `currentUser` to `testutil/FakeAuthRepository` (a `MutableStateFlow<CurrentUser?>`), and clear it in `signOut()`

## 3. Use case

- [x] 3.1 Add `domain/usecase/ObserveCurrentUserUseCase` wrapping `authRepository.currentUser`

## 4. UI

- [x] 4.1 Add `ui/screens/profile/ProfileUiState` and `ProfileViewModel` (`@HiltViewModel`, `StateFlow` via `stateIn`)
- [x] 4.2 Update `ProfileScreen` to use `hiltViewModel()`: real email, name row only when present, avatar initial from state
- [x] 4.3 Add `avatarInitial: StateFlow<String>` to `HomeViewModel` and use it in `HomeScreen` instead of the hardcoded "A"

## 5. Tests (one per spec scenario)

- [x] 5.1 `CurrentUser` tests: display name with and without whitespace, blank name, initial from name, from email, fallback "?"
- [x] 5.2 `ProfileViewModelTest`: account with name; account without name (name null); blank name; account switch updates state; sign-out
- [x] 5.3 `HomeViewModelTest`: avatar initial from display name, from email, and after an account switch

## 6. Verification

- [x] 6.1 Run `./gradlew ktlintFormat` then `./gradlew ktlintCheck`
- [x] 6.2 Run `./gradlew test`
- [x] 6.3 Run `./gradlew assembleDebug`
- [x] 6.4 Update the `profile` spec's stale Home-avatar note at archive (the MODIFIED requirement drops it)
