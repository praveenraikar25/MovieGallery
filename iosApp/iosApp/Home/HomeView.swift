import Shared
import SwiftUI

/// The movie rails on the shared `HomeViewModel`: one loading state, then the rails in the order the
/// ViewModel gives them, or a full-screen error with Retry when every rail is unavailable
/// (movie-browse). A card opens Movie Detail; the avatar switches to the Profile tab
/// (app-navigation "Home avatar opens Profile"). The tab's stack is `MainTabView`'s.
struct HomeView: View {
    let graph: MovieGalleryGraph
    let onMovieClick: (Int32) -> Void
    let onProfileClick: () -> Void

    var body: some View {
        ViewModelScope(create: {
            ObservedViewModel<HomeViewModel, HomeUiState>({ graph.homeViewModel() }, state: { $0.uiState })
        }) { model in
            HomeContent(model: model, onMovieClick: onMovieClick, onProfileClick: onProfileClick)
        }
    }
}

private struct HomeContent: View {
    let model: ObservedViewModel<HomeViewModel, HomeUiState>
    let onMovieClick: (Int32) -> Void
    let onProfileClick: () -> Void

    /// Created once, on first appearance, for the same reason `ViewModelScope` defers its model.
    @State private var avatarInitial: ObservedFlow<NSString>?

    var body: some View {
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
                            MovieRail(section: section, onMovieClick: onMovieClick)
                        }
                    }
                    .padding(.vertical)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .navigationTitle("MovieGallery")
        .toolbar {
            if let avatarInitial {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(action: onProfileClick) {
                        InitialAvatar(text: avatarInitial.value as String, size: 32)
                    }
                    .accessibilityLabel("Profile")
                }
            }
        }
        .onAppear {
            if avatarInitial == nil { avatarInitial = ObservedFlow(model.viewModel.avatarInitial) }
        }
    }
}
