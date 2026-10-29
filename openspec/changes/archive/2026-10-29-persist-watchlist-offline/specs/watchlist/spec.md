## MODIFIED Requirements

### Requirement: Movies are added and removed by toggling
Toggling a movie that is not in the watchlist SHALL add it, and toggling a movie already in the watchlist (matched by movie id) SHALL remove it. A movie SHALL appear in the watchlist at most once, even when it is toggled repeatedly in quick succession.

#### Scenario: Toggle twice
- **WHEN** the user toggles the same movie on and then off from Movie Detail
- **THEN** the movie is not in the watchlist

#### Scenario: Rapid repeated taps
- **WHEN** the user taps "Add to Watchlist" for the same movie several times in quick succession
- **THEN** the movie is either in the watchlist exactly once or not at all, matching the last label shown on the button, and the Watchlist tab never lists it twice

### Requirement: Watchlist renders without the network
The Watchlist tab SHALL render saved movies from the information captured when they were saved, without making network requests.

> Note: Accepted gap. Saved details (title, year, rating, genres, synopsis, poster) are not refreshed after saving, so the Watchlist tab can show older values than Movie Detail, for example "TBA" for a film that has since been released.

#### Scenario: Offline
- **WHEN** the device is offline and the user opens the Watchlist tab
- **THEN** previously saved movies are still shown

#### Scenario: Offline after restart
- **WHEN** the user saves movies, the app process is killed, and the app is relaunched with the device offline
- **THEN** the Watchlist tab shows the saved movies

## ADDED Requirements

### Requirement: Watchlist survives app restarts
The watchlist SHALL be saved on the device so it is unchanged after the app process is killed and relaunched, including the order in which movies were added.

#### Scenario: Process restart
- **WHEN** the user saves movie A and then movie B, and the app process is killed and relaunched
- **THEN** the Watchlist tab shows "2 saved" with A before B

#### Scenario: Removal survives restart
- **WHEN** the user removes a saved movie and the app process is killed and relaunched
- **THEN** that movie is not in the watchlist and Movie Detail shows "Add to Watchlist" for it

### Requirement: Watchlist is kept separately per account
Each signed-in account SHALL have its own watchlist on the device. A user SHALL only see, and only change, the movies saved while signed in to their own account.

#### Scenario: Another account on the same device
- **WHEN** user A saves a movie and signs out, and user B signs in on the same device
- **THEN** user B's Watchlist tab shows the empty state, and Movie Detail for A's movie shows "Add to Watchlist"

#### Scenario: Changes stay with the account that made them
- **WHEN** user B saves a movie while signed in on a device user A has also used
- **THEN** the movie does not appear in user A's watchlist when A signs in again

### Requirement: Watchlist is kept across sign-out
An account's saved movies SHALL stay on the device after sign-out and SHALL be shown again when the same account signs in on that device, including after another account has used the device in between or the app has restarted.

> Note: Placeholder behavior. Saved movies stay in the app's private storage after sign-out. Other accounts cannot see them in the app. They are not synced to the account, so they do not appear on another device, after a reinstall, or after app data is cleared.

#### Scenario: Sign out and back in
- **WHEN** the user saves a movie, signs out, and signs in again
- **THEN** the saved movie is still in the watchlist

#### Scenario: Back after another account
- **WHEN** user A saves movies and signs out, user B signs in, saves a movie and signs out, and user A signs in again
- **THEN** user A's Watchlist tab shows exactly A's movies, in the order A added them

## REMOVED Requirements

### Requirement: Watchlist lasts only for the app process
**Reason**: The watchlist is now saved on the device per account instead of being held in memory for one process and shared by every account.
**Migration**: Covered by "Watchlist survives app restarts", "Watchlist is kept separately per account" and "Watchlist is kept across sign-out". Movies saved in memory before this change are not carried over.
