# Design

## Context

BFF calls go through the `@BffClient` OkHttp client, which today adds only `X-APP-VERSION` (its KDoc says the host takes no auth header). The Firebase ID token is available via `FirebaseAuth.currentUser.getIdToken()`, an async Task (awaited with coroutines elsewhere), while OkHttp interceptors and authenticators are synchronous. `safeApiCall` maps `HttpException` 401 to `AppError.Unauthorized`, whose shared error copy is TMDB-specific. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- Authenticate every BFF request transparently, including future BFF endpoints.
- Keep `NetworkModule` free of a direct `FirebaseAuth` dependency (domain-owned interface).
- Recover from an expired/revoked token with one forced refresh.

**Non-Goals:**
- Forced sign-out, anonymous BFF endpoints, any change to TMDB or AI clients.

## Decisions

**1. Interceptor with a blocking token lookup (over per-call `@Header`).** An interceptor on the BFF client adds `Authorization: Bearer <token>`. Token lookup uses `runBlocking`, which is safe because OkHttp runs interceptors on its own dispatcher thread, never the main thread, and Firebase serves a cached token (refreshing silently near expiry). Rejected: per-call `@Header` — every future endpoint would have to remember it.

**2. Authenticator for 401 (on top of 1).** An OkHttp `Authenticator` on the BFF client force-refreshes the token (`getIdToken(true)`) and retries once. It returns `null` if the failed request already carried a refreshed token (prior response exists with the same header) or no token is available, so a persistent 401 passes through and cannot loop.

**3. Token provider interface in the domain layer.** A small `AuthTokenProvider` (`suspend fun idToken(forceRefresh: Boolean): String?`) is implemented in the data layer on top of `FirebaseAuth` and bound in a Hilt module. The interceptor/authenticator depend on the interface; tests use a fake.

**4. Missing token fails locally.** When the provider yields no token, the interceptor throws a dedicated `IOException` subclass (OkHttp requires interceptors to throw `IOException`; other exceptions would crash the dispatcher thread) without proceeding. `safeApiCall` catches it before the generic `IOException` branch.

**5. New `AppError.AuthRequired`.** Both the local missing-token failure and a persistent BFF 401 map to `AuthRequired`, distinct from the TMDB-specific `Unauthorized`. `TvShowRepositoryImpl` is the mapping point for the 401 (`Unauthorized` -> `AuthRequired`), so TMDB screens keep their copy. The shared error view gains a case for it with the strings in the spec. Rejected: rewording `Unauthorized` generically — would lose the useful TMDB setup hint.

**6. Log redaction.** Add `redactHeader("Authorization")` to the BFF logging interceptor, mirroring the TMDB client, and update the stale KDoc on the BFF client.

**7. Remove debug token logging.** Delete `logIdToken()` and its three call sites in `AuthRepositoryImpl`.

## Risks / Trade-offs

- [Blocking a dispatcher thread if Firebase hangs] -> OkHttp's 15s timeouts bound the call; token is normally cached.
- [Error copy is an assumption] -> Strings in the spec are proposed; confirm during review before apply.
- [Authenticator adds a second token fetch on 401] -> Only on 401; single retry guarded by the prior-response check.
