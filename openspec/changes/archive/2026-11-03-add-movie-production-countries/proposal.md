# Proposal

## Why

The app talks to REST backends only, and we want hands-on experience with GraphQL. The public Countries GraphQL API
(`countries.trevorblades.com`) fits here because TMDB already tags every movie with ISO 3166-1 production-country codes,
the same codes the API takes as input. On the Movie Detail screen TMDB says where a film is from, and the GraphQL API
describes that place. That gives users a small new piece of information and gives the codebase a real GraphQL client.

## What Changes

- Movie Detail shows a "Made in" row of country chips (flag + country name) for the movie's production countries, taken
  from the TMDB detail response the screen already fetches. The row is hidden when TMDB lists no countries.
- Tapping a chip opens a bottom sheet with that country's details: flag, name, native name, capital, continent,
  languages and currency. These come from the Countries GraphQL API.
- The sheet has its own loading, error-with-retry and "no details" states. A Countries API failure never affects the
  rest of the Movie Detail screen.
- The app gains a GraphQL client (Apollo Kotlin) with a checked-in schema and typed, generated query models. It is the
  app's fourth backend, alongside TMDB, the BFF and Firebase.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `movie-detail`: adds requirements for the production-country row and the country details sheet (loading, error,
  retry and unknown-country states). Existing requirements are unchanged.

## Impact

- **Backends touched:** TMDB (reads the `production_countries` field already present in the `movie/{id}` response; no
  new endpoint) and a **new** Countries GraphQL backend (public, unauthenticated). BFF and Firebase AI: none.
- **Code:** `data/remote/dto/MovieDtos.kt` + `MovieMappers.kt` (production countries), `domain/model` (production
  country + country details models), a new `CountryRepository` interface/impl, a new use case, a new GraphQL data
  source, a new Hilt module for the Apollo client, `MovieDetailViewModel`/`UiState`/`Screen`, and a new chip component.
- **Dependencies:** Apollo Kotlin (runtime + Gradle plugin), latest stable, declared in `gradle/libs.versions.toml`.
- **Docs:** CLAUDE.md "Backends" and "Repo map" sections, plus the `openspec/config.yaml` context line that says the
  app has three backends.

## Out of scope

- Browsing or discovering movies by country (TMDB `discover/movie?with_origin_country=`). That is a follow-up change
  that can reuse this GraphQL client.
- Spoken languages, origin country, or production companies on the detail screen.
- Persisting country data in Room or using Apollo's normalized cache. An in-memory cache for the session is enough for
  data this static.
- Country chips on TV show screens.
- Localising country names. The chip shows TMDB's English name.

## Pre-production debt

None. The Countries API is a public demo endpoint with no SLA. If it is down, only the details sheet fails, and it
shows its own error and retry, so nothing is hardcoded or faked.
