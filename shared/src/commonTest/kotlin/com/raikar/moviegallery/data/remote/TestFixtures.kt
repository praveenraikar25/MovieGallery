package com.raikar.moviegallery.data.remote

/*
 * TMDB and BFF response bodies for the shared network and mapper tests. They are Kotlin strings
 * rather than resource files because commonTest also runs on iOS, which has no classpath to read
 * resources from. The Countries GraphQL fixtures stay in app/src/test/resources.
 */

/** The same configuration the clients decode with, so tests exercise the real parser. */
internal val testJson = networkJson

/** The fixture named [name], as it was named when it was a file under `app/src/test/resources`. */
internal fun fixture(name: String): String = checkNotNull(FIXTURES[name]) { "Missing test fixture: $name" }

private val FIXTURES: Map<String, String> =
    mapOf(
        "movie_detail_countries.json" to
            """
            {
              "id": 27205,
              "title": "Inception",
              "release_date": "2010-07-15",
              "vote_average": 8.4,
              "genres": [{ "id": 28, "name": "Action" }],
              "overview": "",
              "poster_path": null,
              "backdrop_path": null,
              "runtime": 148,
              "tagline": "",
              "production_countries": [
                { "iso_3166_1": "GB", "name": "United Kingdom" },
                { "iso_3166_1": "", "name": "Nowhere" },
                { "iso_3166_1": "FR", "name": "France" },
                { "iso_3166_1": "XX", "name": "" },
                { "iso_3166_1": "US", "name": "United States of America" }
              ],
              "credits": { "cast": [] }
            }
            """.trimIndent(),
        "movie_detail_single_country.json" to
            """
            {
              "id": 496243,
              "title": "Parasite",
              "production_countries": [{ "iso_3166_1": "KR", "name": "South Korea" }]
            }
            """.trimIndent(),
        "movie_detail_sparse.json" to
            """
            {
              "id": 700001,
              "title": "Sparse Detail",
              "release_date": null,
              "vote_average": 0.0,
              "genres": [],
              "overview": "",
              "poster_path": null,
              "backdrop_path": null,
              "runtime": 0,
              "tagline": "",
              "credits": {
                "cast": []
              }
            }
            """.trimIndent(),
        "movie_detail_with_credits.json" to
            """
            {
              "id": 1061474,
              "title": "Superman",
              "release_date": "2025-07-09",
              "vote_average": 7.383,
              "genres": [
                { "id": 878, "name": "Science Fiction" },
                { "id": 12, "name": "Adventure" },
                { "id": 28, "name": "Action" }
              ],
              "overview": "Superman must reconcile his alien heritage with his human upbringing.",
              "poster_path": "/ombsmhYUqR4qqOLOxAyr5V8hbyv.jpg",
              "backdrop_path": "/aZgWnhSHmoYcvNjRfsBmVXpJ7Ln.jpg",
              "runtime": 130,
              "tagline": "Look up.",
              "budget": 225000000,
              "status": "Released",
              "credits": {
                "cast": [
                  { "id": 2, "name": "Rachel Brosnahan", "character": "Lois Lane", "order": 1 },
                  { "id": 1, "name": "David Corenswet", "character": "Clark Kent", "order": 0 },
                  { "id": 3, "name": "Nameless Extra", "character": "", "order": 2 }
                ],
                "crew": [
                  { "id": 99, "name": "James Gunn", "job": "Director" }
                ]
              }
            }
            """.trimIndent(),
        "movie_list_popular.json" to
            """
            {
              "page": 1,
              "results": [
                {
                  "id": 1061474,
                  "title": "Superman",
                  "release_date": "2025-07-09",
                  "vote_average": 7.383,
                  "genre_ids": [878, 12, 28],
                  "overview": "Superman must reconcile his alien heritage with his human upbringing.",
                  "poster_path": "/ombsmhYUqR4qqOLOxAyr5V8hbyv.jpg",
                  "backdrop_path": "/aZgWnhSHmoYcvNjRfsBmVXpJ7Ln.jpg",
                  "adult": false,
                  "original_language": "en",
                  "popularity": 512.3,
                  "vote_count": 1834
                },
                {
                  "id": 900667,
                  "title": "A Film With No Metadata",
                  "release_date": "",
                  "vote_average": 0.0,
                  "genre_ids": [],
                  "overview": "",
                  "poster_path": null,
                  "backdrop_path": null
                },
                {
                  "id": 552524,
                  "title": "Unknown Genre Movie",
                  "release_date": "2026-03",
                  "vote_average": 6.5,
                  "genre_ids": [28, 999999],
                  "overview": "Has one genre id TMDB does not publish.",
                  "poster_path": "/abc123.jpg"
                }
              ],
              "total_pages": 51234,
              "total_results": 1024680
            }
            """.trimIndent(),
        "movie_list_upcoming.json" to
            """
            {
              "dates": {
                "maximum": "2026-09-24",
                "minimum": "2026-08-27"
              },
              "page": 1,
              "results": [
                {
                  "id": 1234821,
                  "title": "Jurassic World Rebirth",
                  "release_date": "2026-07-01",
                  "vote_average": 0.0,
                  "genre_ids": [878, 12],
                  "overview": "An unreleased film, so TMDB reports no rating yet.",
                  "poster_path": "/q0fGCmjLuNoXoOwBEGQOWm88vHs.jpg",
                  "backdrop_path": null
                }
              ],
              "total_pages": 12,
              "total_results": 234
            }
            """.trimIndent(),
        "tv_show_list_top.json" to
            """
            {
              "page": 1,
              "totalPages": 500,
              "totalResults": 10000,
              "results": [
                {
                  "id": 1399,
                  "name": "Game of Thrones",
                  "overview": "Seven noble families fight for control of the mythical land of Westeros.",
                  "firstAirDate": "2011-04-17",
                  "voteAverage": 8.4,
                  "posterUrl": "https://image.tmdb.org/t/p/w500/xxx.jpg",
                  "genres": ["Drama", "Sci-Fi & Fantasy"]
                },
                {
                  "id": 2,
                  "name": "Untitled Pilot",
                  "overview": "",
                  "firstAirDate": null,
                  "voteAverage": 0.0,
                  "posterUrl": null,
                  "genres": []
                },
                {
                  "id": 3,
                  "name": "Short Date Show",
                  "overview": "A show with a partial air date.",
                  "firstAirDate": "2026-03",
                  "voteAverage": 6.2,
                  "posterUrl": null,
                  "genres": ["Comedy"]
                }
              ]
            }
            """.trimIndent(),
    )
