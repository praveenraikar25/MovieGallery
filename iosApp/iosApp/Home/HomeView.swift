import Shared
import SwiftUI

/// The movie rails on the shared `HomeViewModel`: one loading state, then the rails in the order the
/// ViewModel gives them, or a full-screen error with Retry when every rail is unavailable
/// (movie-browse). Card taps and the avatar arrive in phase 5c.
struct HomeView: View {
    let graph: MovieGalleryGraph

    var body: some View {
        ViewModelScope(create: {
            ObservedViewModel<HomeViewModel, HomeUiState>({ graph.homeViewModel() }, state: { $0.uiState })
        }) { model in
            HomeContent(model: model)
        }
    }
}

private struct HomeContent: View {
    let model: ObservedViewModel<HomeViewModel, HomeUiState>

    var body: some View {
        NavigationStack {
            Group {
                let state = model.state
                if state.isLoading {
                    ProgressView()
                } else if let error = state.error {
                    ErrorStateView(error: error) { model.viewModel.retry() }
                } else {
                    ScrollView {
                        LazyVStack(alignment: .leading, spacing: 24) {
                            ForEach(state.sections, id: \.title) { section in
                                MovieRail(section: section)
                            }
                        }
                        .padding(.vertical)
                    }
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .navigationTitle("MovieGallery")
        }
    }
}
