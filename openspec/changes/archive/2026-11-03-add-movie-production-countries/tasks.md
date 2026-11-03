# Tasks

Order follows CLAUDE.md "Adding a TMDB endpoint" (for the `production_countries` field) and then "Adding a feature
screen" (domain → repository → DI → use case → UI → tests). No new route or tab is needed. The sheet lives inside
Movie Detail.

## 1. GraphQL client setup

- [x] 1.1 Add `apollo = "5.2.0"`, the `apollo-runtime` library and the `com.apollographql.apollo` plugin to
  `gradle/libs.versions.toml`, apply the plugin in the root (`apply false`) and `app` build files, and configure
  `apollo { service("countries") { packageName.set("com.raikar.moviegallery.data.remote.graphql.countries");
  introspection { endpointUrl.set("https://countries.trevorblades.com/graphql"); schemaFile.set(file("src/main/graphql/countries/schema.graphqls")) } } }`.
  Verify with `./gradlew assembleDebug`. If Apollo 5.2.0 is incompatible with AGP 9.2 / Kotlin 2.3, record the fallback
  version in design.md (Risks).
- [x] 1.2 Run `./gradlew :app:downloadCountriesApolloSchemaFromIntrospection`, commit
  `app/src/main/graphql/countries/schema.graphqls`, and add `CountryDetails.graphql` (query from design.md §2). Verify
  `./gradlew assembleDebug` generates `CountryDetailsQuery` with no network access needed afterwards.
- [x] 1.3 Add `di/GraphQlModule.kt` providing a `@Singleton ApolloClient` derived from the base `OkHttpClient`, with
  debug-only body logging (design.md §3). Verify the Hilt graph compiles with `./gradlew assembleDebug`.

## 2. Production countries from TMDB

- [x] 2.1 Add `ProductionCountry(code, name)` with a computed `flag` to `domain/model/`, and add
  `productionCountries: List<ProductionCountry>` to `MovieDetail`. Verify with new `ProductionCountryTest` cases:
  `"KR"` → 🇰🇷, lowercase `"kr"` → 🇰🇷, `"XYZ"`/`""`/`"K1"` → `null` (spec: "Country code with no flag").
- [x] 2.2 Add `ProductionCountryDto(@SerialName("iso_3166_1") code, name)` and
  `@SerialName("production_countries") productionCountries` (default empty) to `MovieDetailDto`, and map it in
  `MovieMappers.kt`, keeping TMDB's order and dropping entries with a blank code or name. Verify with a
  `movie_detail_countries.json` fixture in `MovieMappersTest` covering one country, three countries in order, a missing
  field (→ empty list) and a blank entry (spec: "Single production country", "Co-production", "No production
  countries"). Update existing `MovieDetail(...)` constructions in tests so `./gradlew test` compiles and passes.

## 3. Country details repository

- [x] 3.1 Add `CountryDetails` to `domain/model/` and `CountryRepository` to `domain/repository/`
  (`getCountryDetails(code): DataResult<CountryDetails?>`). Verify the domain layer has no `android.*` or
  `com.apollographql` imports (`grep -r` over `domain/` returns nothing).
- [x] 3.2 Add `data/remote/SafeGraphQlCall.kt` with the mapping table from design.md §4. Verify with
  `SafeGraphQlCallTest`: network exception → `Network`, HTTP 500 → `Http(500)`, 401 → `Unauthorized`, errors-only →
  `Unknown`, data → `Success`, and `CancellationException` rethrown.
- [x] 3.3 Add a `CountryDetailsQuery.Data` → `CountryDetails` mapper (`data/remote/CountryMappers.kt`) that trims
  `currencies` and drops blanks, nulls a `native` equal to `name`, and nulls a blank `capital`. Verify with
  `CountryMappersTest` (spec: "Several languages or currencies", "Native name equals name", "Missing capital").
- [x] 3.4 Add `data/repository/CountryRepositoryImpl.kt` (on `@IoDispatcher`, wrapped in `safeGraphQlCall`, with an
  in-memory cache of successes only) and `@Binds` it in `RepositoryModule`. Verify with `CountryRepositoryImplTest`
  against `MockWebServerRule` serving `country_jp.json` / `country_null.json` fixtures: maps Japan correctly,
  `country: null` → `Success(null)`, a second call for the same code makes no second request, a failed call is not
  cached, and a disconnected server → `Failure(Network)`.
- [x] 3.5 Add `domain/usecase/GetCountryDetailsUseCase` and a `testutil/FakeCountryRepository` with a configurable
  result and a call counter. Verify `./gradlew test` passes.

## 4. Movie Detail ViewModel

- [x] 4.1 Add `CountrySheetState` and `countrySheet` to `MovieDetailUiState`, inject `GetCountryDetailsUseCase` into
  `MovieDetailViewModel`, and add `onCountryClick`, `onCountrySheetDismiss` and `retryCountry`, cancelling any
  in-flight country load (design.md §6). Update `MovieDetailViewModelTest.viewModel()` to pass a fake. Verify with new
  `MovieDetailViewModelTest` cases:
  - tap → sheet open and loading → loaded details ("Opening a country")
  - repository `Success(null)` → `notFound = true`, no error ("Historical country")
  - repository `Failure(Network)` → `error` set while `movie` is unchanged ("Offline when opening a country")
  - `retryCountry()` after a failure → loading → details ("Retrying")
  - dismiss → `countrySheet == null` with movie state unchanged ("Dismissing the sheet")
  - tapping country B while A is loading → the sheet ends showing B only
  - a country load failure never sets the screen-level `error`

## 5. Movie Detail UI

- [x] 5.1 Add `ui/components/CountryChip.kt` (pill styling shared with `GenreChip`, clickable, `contentDescription =
  name`) and a pure `countryChipLabel(country)` helper. Verify with `CountryChipLabelTest`: `"🇰🇷 South Korea"` when
  there is a flag, `"Soviet Union"` when `flag` is null.
- [x] 5.2 In `MovieDetailScreen`, add the "Made in" label and a horizontally scrolling chip row under the genre chips,
  omitted when the list is empty, and wire `onCountryClick`. Verify by running the app (`/run`) on a co-production
  (e.g. TMDB id 27205 *Inception*: UK + US) and a single-country film (e.g. 496243 *Parasite*: KR). Confirm the row
  scrolls and the rest of the screen does not.
- [x] 5.3 Add `ui/screens/detail/CountryDetailsSheet.kt` (a `ModalBottomSheet` with loading, error + "Retry",
  not-found and loaded layouts) and a pure `countryDetailRows(details)` helper that returns the four label/value pairs
  with "—" for missing values and ", "-joined lists. Verify with `CountryDetailRowsTest` (spec: "Opening a country",
  "Several languages or currencies", "Missing capital") and by running the app: open Japan, put the device in airplane
  mode and open another country (error + Retry), restore connectivity and Retry, reopen Japan (no loader), and swipe to
  dismiss.
  > Done on a Galaxy S10 with The Dark Knight (UK/US) and Come and See (SU): the loaded, cached-reopen, not-found
  > and dismiss states were checked on device. The offline/Retry state was checked by unit tests only; the phone's
  > network was not toggled.

## 6. Docs

- [x] 6.1 Update CLAUDE.md: "Tech Stack" (Apollo Kotlin for GraphQL), "Backends" (the Countries GraphQL backend via
  the `ApolloClient` in `GraphQlModule`, schema in `src/main/graphql/countries/`, failures mapped by
  `safeGraphQlCall`), "Repo map" (`data/remote/graphql`, `GraphQlModule`), and a short "Adding a GraphQL query"
  checklist. Update `openspec/config.yaml` context from three backends to four. Mirror any layering note in
  `.claude/skills/android-architecture/SKILL.md`. Verify by reading the updated sections against the code.

## 7. Verification

- [x] 7.1 Run `./gradlew ktlintFormat` then `./gradlew ktlintCheck` and verify it reports no violations.
- [x] 7.2 Run `./gradlew test` and verify every test passes, including the new ones.
- [x] 7.3 Run `./gradlew assembleDebug` and verify the build succeeds.
- [x] 7.4 Run `openspec validate add-movie-production-countries --strict` and verify it passes.

## Workflow follow-up

- Archive the change (`/opsx:archive`) after review, once `./gradlew test` passes.
- A follow-up change can add "Explore by country" (TMDB `discover/movie?with_origin_country=`) reusing this Apollo
  client.
