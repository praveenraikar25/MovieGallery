import Shared
import SwiftUI

/// The ranked top TV shows from the BFF, a page at a time (tv-shows). Tiles can't be tapped.
struct TopTvShowsView: View {
    let graph: MovieGalleryGraph

    var body: some View {
        ViewModelScope(create: {
            ObservedViewModel<TopTvShowsViewModel, TopTvShowsUiState>(
                { graph.topTvShowsViewModel() },
                state: { $0.uiState }
            )
        }) { model in
            TopTvShowsContent(model: model)
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) {
                VStack(spacing: 0) {
                    Text("Top TV Shows").font(.headline)
                    Text("The highest rated series")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .toolbar(.hidden, for: .tabBar)
    }
}

private struct TopTvShowsContent: View {
    let model: ObservedViewModel<TopTvShowsViewModel, TopTvShowsUiState>

    var body: some View {
        let state = model.state
        Group {
            if state.isLoading {
                ProgressView()
            } else if let error = state.error, state.shows.isEmpty {
                // Checked against the raw list: `isEmpty` requires no error, as on Android.
                ErrorStateView(error: error) { model.viewModel.loadNextPage() }
            } else if state.isEmpty {
                Text("No shows to display").font(.headline)
            } else {
                ScrollView {
                    VStack(spacing: 16) {
                        PosterGrid(
                            items: state.shows.map { $0.toPosterGridItem() },
                            onItemAppear: { index in
                                // Read the latest state, not the one this closure was built with.
                                let current = model.state
                                if TvShowsPaging.shouldLoadMore(
                                    index: index,
                                    count: current.shows.count,
                                    hasError: current.error != nil
                                ) {
                                    model.viewModel.loadNextPage()
                                }
                            }
                        )
                        footer(state)
                    }
                    .padding(.vertical, 16)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    @ViewBuilder
    private func footer(_ state: TopTvShowsUiState) -> some View {
        if state.isLoadingMore {
            ProgressView().frame(height: 48)
        } else if state.error != nil {
            VStack(spacing: 10) {
                Text("Couldn't load more shows.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                Button("Retry") { model.viewModel.loadNextPage() }
                    .buttonStyle(.bordered)
            }
            .padding(.vertical, 16)
        }
    }
}
