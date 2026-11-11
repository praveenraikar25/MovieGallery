import Shared
import SwiftUI

/// Everything below the header, in Android's order: metadata and rating, the watchlist toggle,
/// genres, "Made in", the synopsis and the cast (movie-detail).
struct MovieDetailBody: View {
    let detail: MovieDetail
    let isInWatchlist: Bool
    let onToggleWatchlist: () -> Void
    let onCountryClick: (ProductionCountry) -> Void

    var body: some View {
        let movie = detail.summary
        VStack(alignment: .leading, spacing: 0) {
            VStack(alignment: .leading, spacing: 0) {
                // The metadata line joins only the parts TMDB returned (shared `metadataLabel`).
                HStack(spacing: 0) {
                    Text(detail.metadataLabel).foregroundStyle(.secondary)
                    if movie.hasRating {
                        Text(" · ★ \(movie.ratingLabel)").foregroundStyle(.tint)
                    }
                }
                .font(.subheadline)

                Button(action: onToggleWatchlist) {
                    Label(
                        isInWatchlist ? "In Watchlist" : "Add to Watchlist",
                        systemImage: isInWatchlist ? "bookmark.fill" : "bookmark"
                    )
                }
                .buttonStyle(.borderedProminent)
                .buttonBorderShape(.capsule)
                .padding(.top, 14)

                if !movie.genres.isEmpty {
                    HStack(spacing: 8) {
                        ForEach(movie.genres, id: \.self) { genre in
                            Text(genre)
                                .font(.footnote)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                                .overlay(Capsule().stroke(.quaternary))
                        }
                    }
                    .lineLimit(1)
                    .padding(.top, 16)
                }

                if !detail.productionCountries.isEmpty {
                    Text("Made in")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .padding(.top, 18)
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(detail.productionCountries, id: \.code) { country in
                                CountryChip(country: country) { onCountryClick(country) }
                            }
                        }
                        .padding(.horizontal, 20)
                    }
                    // Bleeds past the column's padding so chips scroll edge to edge.
                    .padding(.horizontal, -20)
                    .padding(.top, 6)
                }

                Text("Synopsis")
                    .font(.headline)
                    .padding(.top, 24)
                Text(movie.synopsisText)
                    .foregroundStyle(.secondary)
                    .padding(.top, 8)
            }
            .padding(20)

            if !detail.cast.isEmpty {
                CastRow(cast: detail.cast)
                    .padding(.bottom, 20)
            }
        }
    }
}

/// A production country that opens its details: filled and ending in a chevron, unlike the
/// outlined genre chips, so it reads as tappable.
private struct CountryChip: View {
    let country: ProductionCountry
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Text(CountryChipLabelKt.countryChipLabel(country: country))
                    .font(.subheadline.weight(.medium))
                    .lineLimit(1)
                Image(systemName: "chevron.right")
                    .font(.caption2.weight(.semibold))
                    .foregroundStyle(.secondary)
            }
            .padding(.leading, 14)
            .padding(.trailing, 10)
            .frame(height: 36)
            .background(.background.secondary, in: Capsule())
            .overlay(Capsule().stroke(.quaternary))
        }
        .buttonStyle(.plain)
        // VoiceOver would otherwise spell out the flag's two regional-indicator letters.
        .accessibilityLabel(country.name)
        .accessibilityHint("Show country details")
    }
}
