# Proposal

## Why

MovieGallery was built without spec-driven development, so the behavior of its existing screens is recorded only in code. Adopting OpenSpec now needs a baseline: future changes must be able to write ADDED/MODIFIED deltas against specs that describe what the app does today.

## What Changes

- Document the app's current, user-observable behavior as nine capability specs.
- Specs describe behavior **as-is**, including placeholder behavior (hardcoded sign-in, hardcoded profile, non-persistent watchlist, inert profile rows). Each such gap is flagged with a `> Note:` so a later change can address it with a normal MODIFIED delta.
- No code, configuration, or runtime behavior changes. This change has no implementation tasks.

## Capabilities

### New Capabilities
- `authentication`: Sign-in form validation, the credential check, and signing out.
- `app-navigation`: Start destination, the four bottom tabs and their state retention, and the screens reached outside the tabs.
- `movie-browse`: The Home screen's movie rails, their partial-failure behavior, and retry.
- `movie-search`: Query-driven movie search, the empty-query fallback, result labelling, and retry.
- `movie-detail`: A single movie's details, cast, missing-data fallbacks, the watchlist toggle, and errors.
- `watchlist`: Saving and removing movies, the Watchlist screen, ordering, and persistence limits.
- `tv-shows`: The paginated Top TV Shows list, de-duplication, and load/load-more errors.
- `ai-chat`: The AI movie-recommendation chat, poster identification, assistant guardrails, and retry.
- `profile`: The Profile screen's user summary, entry points, placeholder rows, and sign-out.

### Modified Capabilities
<!-- None: no specs exist yet. -->

## Impact

- Adds `openspec/specs/<capability>/spec.md` for each capability above once archived.
- No affected code, APIs, dependencies, or build configuration.
