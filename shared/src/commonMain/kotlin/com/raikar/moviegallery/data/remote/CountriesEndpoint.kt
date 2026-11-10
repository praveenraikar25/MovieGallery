package com.raikar.moviegallery.data.remote

/**
 * The Countries GraphQL backend, which both platforms' `ApolloClient`s point at. Public and
 * unauthenticated, so it needs no qualifier while it is the app's only Apollo client. Add one (as
 * `TmdbClient`/`BffClient` do) if a second GraphQL backend arrives.
 */
const val COUNTRIES_URL = "https://countries.trevorblades.com/graphql"
