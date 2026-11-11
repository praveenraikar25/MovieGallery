import Shared
import SwiftUI

/// The saved movies, live from the shared `WatchlistViewModel` (watchlist). A movie toggled on
/// Movie Detail appears or disappears here without a reload.
struct WatchlistView: View {
    let graph: MovieGalleryGraph
    let onMovieClick: (Int32) -> Void

    var body: some View {
        ViewModelScope(create: {
            ObservedViewModel<WatchlistViewModel, WatchlistUiState>(
                { graph.watchlistViewModel() },
                state: { $0.uiState }
            )
        }) { model in
            WatchlistContent(state: model.state, onMovieClick: onMovieClick)
        }
    }
}

private struct WatchlistContent: View {
    let state: WatchlistUiState
    let onMovieClick: (Int32) -> Void

    var body: some View {
        if state.isEmpty {
            // As on Android, the empty state stands alone, without the title.
            VStack(spacing: 4) {
                Image(systemName: "bookmark")
                    .font(.system(size: 44))
                    .foregroundStyle(.secondary)
                    .padding(.bottom, 12)
                Text("Your watchlist is empty")
                    .font(.headline)
                Text("Movies you save will appear here.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: 240)
            }
            .multilineTextAlignment(.center)
            .padding(.horizontal, 40)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else {
            ScrollView {
                PosterGrid(
                    items: state.movies.map { $0.toPosterGridItem() },
                    onTap: onMovieClick,
                    showsBookmark: true
                )
                .padding(.vertical, 16)
            }
            .navigationTitle("Watchlist")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Text("\(state.movies.count) saved")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
        }
    }
}
