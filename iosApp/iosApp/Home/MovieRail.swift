import Shared
import SwiftUI

/// One titled, horizontally scrolling rail of movie cards.
struct MovieRail: View {
    let section: MovieSection
    let onMovieClick: (Int32) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(section.title)
                .font(.title3.bold())
                .padding(.horizontal)
            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(alignment: .top, spacing: 12) {
                    ForEach(section.movies, id: \.id) { movie in
                        // movie-browse "Tapping a card opens its details".
                        Button { onMovieClick(movie.id) } label: { MovieCard(movie: movie) }
                            .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal)
            }
        }
    }
}

/// A poster (or a placeholder), the title, and the shared subtitle (movie-browse "Movie cards
/// summarize a movie").
private struct MovieCard: View {
    let movie: Movie

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            AsyncImage(url: movie.posterUrl.flatMap(URL.init(string:))) { phase in
                if let image = phase.image {
                    image.resizable().scaledToFill()
                } else {
                    MovieGradient.gradient(for: movie.id)
                }
            }
            .frame(width: 120, height: 180)
            .clipShape(RoundedRectangle(cornerRadius: 8))

            Text(movie.title)
                .font(.subheadline.weight(.semibold))
                .lineLimit(1)
            Text(movie.cardSubtitle)
                .font(.caption)
                .foregroundStyle(.secondary)
                .lineLimit(1)
        }
        .frame(width: 120)
    }
}
