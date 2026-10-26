# Tasks

## 1. Token provider

- [x] 1.1 Add the `AuthTokenProvider` interface (domain) and a Firebase-backed implementation bound in Hilt; verify with a unit test that a null user yields no token and that `forceRefresh` is passed through.

## 2. BFF networking

- [x] 2.1 Add the BFF auth interceptor (Bearer header; throws a dedicated `IOException` subclass when no token) to the `@BffClient` client; verify with MockWebServer tests for header present and for no request sent when no token.
- [x] 2.2 Add the 401 `Authenticator` (one forced-refresh retry, `null` when already retried or no token); verify with MockWebServer tests for expired-token recovery and for no retry loop on a persistent 401.
- [ ] 2.3 Add `redactHeader("Authorization")` to the BFF logger and fix the stale client KDoc; verify by reading the debug log of a BFF request in a debug build.

## 3. Error handling and UI copy

- [x] 3.1 Add `AppError.AuthRequired`; map the missing-token exception in `safeApiCall` and BFF 401 to it in `TvShowRepositoryImpl` (TMDB mapping unchanged); verify with repository tests for both paths and a TMDB regression test.
- [x] 3.2 Add the "Sign-in required" title/message to the shared error view for `AuthRequired`; verify the first-page and load-more states in the TV shows ViewModel tests and on a device with a signed-out token.

## 4. Cleanup

- [x] 4.1 Remove `logIdToken()` and its call sites from `AuthRepositoryImpl`; verify no `AuthToken` log output after sign-in and that `./gradlew test` and `./gradlew ktlintCheck` pass.
