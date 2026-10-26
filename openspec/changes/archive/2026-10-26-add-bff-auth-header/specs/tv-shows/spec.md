# Spec Delta

## ADDED Requirements

### Requirement: Shows load only for a signed-in user
The screen SHALL load shows by sending the signed-in user's Firebase ID token to the BFF service as a bearer `Authorization` header on every request, and SHALL NOT contact the BFF service when no token is available.

#### Scenario: Signed-in user opens the screen
- **WHEN** a signed-in user opens Top TV Shows
- **THEN** the request to the BFF service carries the user's ID token as `Authorization: Bearer <token>` and the first page of shows is displayed

#### Scenario: No signed-in user or token
- **WHEN** no user is signed in or no token can be obtained
- **THEN** no request is sent to the BFF service and the screen shows the authorization error state

### Requirement: Rejected token is refreshed once
When the BFF service rejects a request as unauthorized, the app SHALL retry that request once with a freshly refreshed token before reporting a failure.

#### Scenario: Expired token
- **WHEN** the BFF service answers 401 and a refreshed token is accepted on the retry
- **THEN** the shows load normally with no error visible to the user

#### Scenario: Still rejected after refresh
- **WHEN** the retry is also rejected
- **THEN** the screen shows the authorization error state and the user stays signed in

### Requirement: Authorization failure shows its own error state
When loading fails because the user could not be authorized with the BFF service, the screen SHALL show a full-screen error titled "Sign-in required" with the message "Your session couldn't be verified. Sign out and sign back in, then try again." and a Retry action that requests the first page again. The error SHALL NOT mention TMDB.

#### Scenario: Authorization fails on first page
- **WHEN** the first page fails because no token is available or the BFF rejects the refreshed token
- **THEN** the screen shows "Sign-in required" with the message above and a Retry button

#### Scenario: Retry after authorization failure
- **WHEN** the user taps Retry
- **THEN** the first page is requested again, with a fresh token lookup

#### Scenario: Authorization fails on a later page
- **WHEN** pages are loaded and a later page fails for authorization
- **THEN** the loaded shows remain and the "Couldn't load more shows." retry footer is shown

### Requirement: Credentials are never logged
The app SHALL NOT write the user's ID token or the BFF `Authorization` header value to device logs in any build, and SHALL NOT log the ID token after sign-in.

#### Scenario: Debug build logging
- **WHEN** a BFF request is logged in a debug build
- **THEN** the `Authorization` header value is masked

#### Scenario: After signing in
- **WHEN** the user signs in by email, account creation or Google
- **THEN** no log entry contains the ID token
