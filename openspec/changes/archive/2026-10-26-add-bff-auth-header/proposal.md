# Proposal

## Why

The BFF service now verifies an `Authorization` header and answers 401 Unauthorized when it is missing, so Top TV Shows can no longer load. The user's Firebase sign-in already produces the token the BFF expects; the app just never sends it.

## What Changes

- Every request to the BFF carries `Authorization: Bearer <Firebase ID token>` for the signed-in user.
- If there is no signed-in user or no token can be obtained, the BFF call fails locally with an error and no request is sent.
- If the BFF rejects the request with 401, the app retries once with a freshly refreshed token before reporting failure.
- A BFF authorization failure shows an error screen with BFF-appropriate copy instead of "TMDB rejected the request". No forced sign-out.
- The BFF request log never prints the `Authorization` value.
- Remove the temporary debug code that logs the Firebase ID token after sign-in.

## Backends touched

BFF only. TMDB, Firebase AI and the Firebase Auth sign-in flows are unchanged (Firebase Auth is only read for its token).

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `tv-shows`: adds requirements for authenticated loading and for the authorization-failure error state, and resolves the "> Note:" in "First-page failure shows a full-screen error" about BFF failures reading as TMDB errors (for authorization failures).

## Impact

- BFF networking (`NetworkModule`, a new token provider and auth interceptor/authenticator), error mapping for BFF calls, shared error-state copy, `AuthRepositoryImpl` (debug logging removal).
- No new dependencies. No API or data model changes.

## Out of scope

- Signing the user out or returning them to login on a persistent 401.
- Anonymous/public BFF endpoints (all BFF calls are authenticated).
- A TV show detail screen, watchlist support, or any other TV Shows behavior.
- Changing TMDB error copy.

## Pre-production debt

none
