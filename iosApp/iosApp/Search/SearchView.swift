import Shared
import SwiftUI

/// Movie search on the shared `SearchViewModel` (movie-search; add-ios-browse-screens design,
/// decision 6). The ViewModel debounces, cancels superseded searches and writes the header label;
/// this view only forwards the text and draws the state.
struct SearchView: View {
    let graph: MovieGalleryGraph
    let onMovieClick: (Int32) -> Void

    var body: some View {
        ViewModelScope(create: {
            ObservedViewModel<SearchViewModel, SearchUiState>({ graph.searchViewModel() }, state: { $0.uiState })
        }) { model in
            SearchContent(model: model, onMovieClick: onMovieClick)
        }
    }
}

private struct SearchContent: View {
    let model: ObservedViewModel<SearchViewModel, SearchUiState>
    let onMovieClick: (Int32) -> Void

    // The field owns its text and forwards each edit, as Login's fields do: a binding read from
    // `state` drops keystrokes, since new state reaches Swift a main-queue hop later. The ViewModel
    // only echoes the query, never rewrites it.
    @State private var query = ""

    var body: some View {
        let state = model.state
        Group {
            if let error = state.error {
                ErrorStateView(error: error) { model.viewModel.retry() }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        Text(state.headerLabel)
                            .font(.subheadline.weight(.medium))
                            .foregroundStyle(.secondary)
                            .padding(.horizontal, 20)
                        PosterGrid(
                            items: state.results.map { $0.toPosterGridItem() },
                            onTap: onMovieClick
                        )
                    }
                    .padding(.vertical, 16)
                }
            }
        }
        .navigationTitle("Search")
        .searchable(text: $query, placement: .navigationBarDrawer(displayMode: .always), prompt: "Search movies…")
        .autocorrectionDisabled()
        .onChange(of: query) { _, value in model.viewModel.onQueryChange(newQuery: value) }
    }
}
