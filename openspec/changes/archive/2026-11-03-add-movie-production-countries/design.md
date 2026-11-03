# Design

## Context

- `movie/{id}` already returns `production_countries: [{ "iso_3166_1": "KR", "name": "South Korea" }]`.
  `MovieDetailDto` drops the field today because `ignoreUnknownKeys` is on.
- All networking is Retrofit through two stacks (`@TmdbClient`, `@BffClient`) sharing one base `OkHttpClient`.
  Failures become `DataResult.Failure(AppError)` via `safeApiCall`. There is no GraphQL client yet.
- The Countries API (`https://countries.trevorblades.com/graphql`) is public, needs no auth, and exposes
  `country(code: ID!): Country`, which returns `null` for an unknown code.
- `MovieDetailViewModel` holds one `MutableStateFlow<MovieDetailUiState>`. `AiChatScreen` already uses a Material 3
  `ModalBottomSheet`, so this change follows that pattern.

## Goals / Non-Goals

**Goals:**
- Add a GraphQL client the way the app adds any backend: a qualifier-free Hilt provider, a data source, and a repository
  that returns `DataResult`. A later "browse by country" change should reuse it without rework.
- Keep the movie load path untouched. Countries are fetched only when a chip is tapped.

**Non-Goals:**
- Apollo normalized cache, persisted queries, or GraphQL subscriptions.
- A generic GraphQL abstraction for other endpoints.

## Decisions

### 1. Apollo Kotlin 5.2.0 as the GraphQL client
Apollo Kotlin (`com.apollographql.apollo` plugin + `apollo-runtime`, 5.2.0, the latest stable on Maven Central) is
added through `libs.versions.toml`. It generates typed Kotlin models from `.graphql` operations checked against the
schema at build time, so a query typo fails the build rather than the request.

*Alternatives:* hand-written POST bodies through Retrofit + kotlinx.serialization. That needs no new dependency, but
it gives up schema validation and generated types, which are the point of the exercise. Ktor-based clients
(graphql-kotlin) would pull in a second HTTP stack.

### 2. Schema and operations live in `app/src/main/graphql/countries/`
- `schema.graphqls`: downloaded once with `./gradlew :app:downloadCountriesApolloSchemaFromIntrospection` and
  committed, so builds never hit the network.
- `CountryDetails.graphql`:
  ```graphql
  query CountryDetails($code: ID!) {
    country(code: $code) { code name native emoji capital currencies continent { name } languages { name } }
  }
  ```
- The Apollo service is named `countries` with `packageName = "com.raikar.moviegallery.data.remote.graphql.countries"`.
  Generated code stays in the data layer and never reaches domain or UI.

### 3. Apollo shares the base OkHttpClient
A new `di/GraphQlModule.kt` provides a `@Singleton ApolloClient` built with `.serverUrl(COUNTRIES_URL)` and
`.okHttpClient(base.newBuilder().addInterceptor(logging).build())`. This reuses the app's connection pool and timeouts,
the same way the TMDB and BFF clients derive from the base client. No qualifier is needed while there is only one
Apollo client. If a second GraphQL backend arrives, add a `@CountriesClient` qualifier then.

### 4. `safeGraphQlCall` maps Apollo outcomes to `AppError`
Apollo 4+ does not throw on transport errors. It returns `ApolloResponse` with `exception` and/or `errors`. A new
`data/remote/SafeGraphQlCall.kt` mirrors `safeApiCall`:

| Apollo outcome | Result |
|---|---|
| `exception is ApolloNetworkException` or `ApolloOfflineException` | `AppError.Network` |
| `exception is ApolloHttpException` | `AppError.Http(statusCode)` (401 → `Unauthorized`, 404 → `NotFound`) |
| `exception is ApolloParseException` | `AppError.Serialization` |
| `exception` of any other type | `AppError.Unknown(exception)` |
| non-empty `errors` with no `data` | `AppError.Unknown(...)`, carrying the first error message |
| `data` present | `Success(data)` |
| `CancellationException` | rethrown |

`AppError` is reused, and no GraphQL-specific error type leaks into the domain layer.

### 5. Domain models and repository
- `domain/model/ProductionCountry(code: String, name: String)` with a computed `flag: String?`. The flag is derived from
  the two-letter code as regional-indicator symbols, and is `null` when the code is not two A–Z letters. It is pure
  Kotlin, so it stays in domain with no `android.*`. `MovieDetail` gains `productionCountries: List<ProductionCountry>`.
- `domain/model/CountryDetails(code, name, nativeName: String?, flag: String?, capital: String?, continent: String?,
  languages: List<String>, currencies: List<String>)`. The schema has both a comma-joined `currency` string and a
  `currencies: [String!]!` list. The query uses the list, so the mapper only trims and drops blanks.
- `domain/repository/CountryRepository.getCountryDetails(code): DataResult<CountryDetails?>`. Here `null` means the
  source has no record (the spec's "No details available" state), which is a success, not a `NotFound` failure. It is
  kept separate from errors so the sheet can omit Retry.
- `data/repository/CountryRepositoryImpl` runs on the injected `@IoDispatcher`, wraps the query in `safeGraphQlCall`,
  and keeps a `Mutex`-guarded in-memory `Map<String, CountryDetails?>` of successful results. Failures are not cached.
  This satisfies "Reopening a country" with no Apollo cache dependency.
- `domain/usecase/GetCountryDetailsUseCase` wraps the repository call, which matches the one-use-case-per-call pattern.

### 6. ViewModel state: a nullable sheet sub-state
`MovieDetailUiState` gains `countrySheet: CountrySheetState?` (null means closed):

```kotlin
data class CountrySheetState(
    val country: ProductionCountry,      // heading fallback for loading / missing states
    val isLoading: Boolean = true,
    val details: CountryDetails? = null,
    val notFound: Boolean = false,
    val error: AppError? = null,
)
```

`onCountryClick(country)`, `onCountrySheetDismiss()` and `retryCountry()` live on `MovieDetailViewModel`. The load
job is kept in a `Job?` and cancelled on dismiss or on a new tap, so a slow response for country A never fills the
sheet for country B. Keeping this inside the existing ViewModel avoids a second ViewModel scoped to a sheet.

### 7. UI
- `ui/components/CountryChip.kt` reuses `GenreChip`'s pill styling, adds a click handler, and renders
  `"${flag} ${name}"`, or just the name when `flag` is null. It sets `contentDescription = name` so TalkBack does not
  read the flag emoji out letter by letter.
- In `MovieDetailScreen`, a "Made in" label + `LazyRow`/`horizontalScroll` row goes directly under the genre chips.
- `ui/screens/detail/CountryDetailsSheet.kt` is a `ModalBottomSheet` with its own loading (`FilmReelSpinner`, matching
  the app's loader), error ("Couldn't load country details." + `AppButton` "Retry"), not-found and loaded layouts.
  `ErrorState`'s TMDB-specific copy is deliberately not reused.

## Risks / Trade-offs

- [Public demo API with no SLA] → Only the sheet depends on it, and failures show inline with Retry. The movie screen
  never calls it on load.
- [Apollo 5 + AGP 9.2 / Kotlin 2.3 compatibility] → Task 1 verifies with `assembleDebug` before any feature code. If
  the Gradle plugin is incompatible, fall back to the newest 5.x patch release that builds and note it in the PR.
- [TMDB uses historical codes such as `SU`, `XC`, `YU` that the Countries API doesn't know] → They map to the "No
  details available" state rather than an error. Some still have regional-indicator flags that render as letters on
  some devices, which is acceptable.
- [Committed schema drifts from the live API] → It is a frozen demo schema. Re-run the download task if a query ever
  fails validation.
- [Generated code under `build/`] → ktlint already excludes `/build/`, so no lint config change is needed.

## Migration Plan

No data migration. This adds a dependency, no Room schema changes, and nothing persisted. Rollback is a plain revert.
