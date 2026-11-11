import Shared
import SwiftUI

/// One movie, on its own shared `MovieDetailViewModel` (movie-detail; add-ios-browse-screens design,
/// decisions 7 and 8). Pushed onto the current tab's stack with the tab bar hidden; the system back
/// button and edge swipe return to the tab.
struct MovieDetailView: View {
    let graph: MovieGalleryGraph
    let movieId: Int32

    var body: some View {
        ViewModelScope(create: {
            ObservedViewModel<MovieDetailViewModel, MovieDetailUiState>(
                { graph.movieDetailViewModel(movieId: movieId) },
                state: { $0.uiState }
            )
        }) { model in
            MovieDetailContent(model: model)
        }
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
        // A visible but clear bar background is what lets the dark colour scheme apply, which turns
        // the status bar light over the backdrop, as Android's detail screen does.
        .toolbarBackground(Color.clear, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .toolbar(.hidden, for: .tabBar)
    }
}

private struct MovieDetailContent: View {
    let model: ObservedViewModel<MovieDetailViewModel, MovieDetailUiState>

    // The sheet follows `state.countrySheet` through local state rather than a binding read from
    // `state`: after a dismiss, that binding would still read the old sheet for one main-queue hop
    // and could present it again. `lastSheet` keeps the content drawn while the sheet slides away.
    @State private var showsCountrySheet = false
    @State private var lastSheet: CountrySheetState?

    var body: some View {
        let state = model.state
        Group {
            if let detail = state.movie {
                LoadedMovieDetail(
                    detail: detail,
                    isInWatchlist: state.isInWatchlist,
                    // The loaded detail is passed in, so the toggle never fires before it exists.
                    onToggleWatchlist: { model.viewModel.onToggleWatchlist(movie: detail) },
                    onCountryClick: { model.viewModel.onCountryClick(country: $0) }
                )
            } else if let error = state.error {
                ErrorStateView(error: error) { model.viewModel.retry() }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .onChange(of: state.countrySheet, initial: true) { _, sheet in
            if let sheet {
                lastSheet = sheet
                showsCountrySheet = true
            } else {
                showsCountrySheet = false
            }
        }
        .sheet(isPresented: $showsCountrySheet, onDismiss: { model.viewModel.onCountrySheetDismiss() }) {
            if let sheet = model.state.countrySheet ?? lastSheet {
                CountryDetailsSheet(state: sheet) { model.viewModel.retryCountry() }
                    .presentationDetents([.medium, .large])
            }
        }
    }
}

/// The loaded movie: a scroll view under the collapsing header. The header's size and the extra
/// bottom space come from the shared collapse math, fed with the scroll offset in points.
private struct LoadedMovieDetail: View {
    let detail: MovieDetail
    let isInWatchlist: Bool
    let onToggleWatchlist: () -> Void
    let onCountryClick: (ProductionCountry) -> Void

    @Environment(\.verticalSizeClass) private var verticalSizeClass
    @State private var offset: CGFloat = 0
    @State private var contentHeight: CGFloat = 0
    @State private var viewportHeight: CGFloat = 0

    var body: some View {
        GeometryReader { proxy in
            // The top inset includes the navigation bar, whose back button the pinned strip holds.
            let topInset = proxy.safeAreaInsets.top
            let expanded: CGFloat = verticalSizeClass == .compact ? 220 : 340
            let collapsed = min(expanded, topInset)
            let fraction = CGFloat(
                HeaderCollapseKt.collapseFraction(
                    firstVisibleItemIndex: 0,
                    firstVisibleItemScrollOffset: Int32(offset.rounded()),
                    expandedHeightPx: Float(expanded),
                    collapsedHeightPx: Float(collapsed)
                )
            )
            let extraBottom = CGFloat(
                HeaderCollapseKt.extraBottomPaddingPx(
                    viewportHeightPx: Int32(viewportHeight.rounded()),
                    contentHeightPx: Int32(contentHeight.rounded()),
                    collapseDistancePx: Int32((expanded - collapsed).rounded())
                )
            )

            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    // Holds the header's place, so content starts below the image and then
                    // scrolls up behind the pinned strip.
                    Color.clear.frame(height: expanded)
                    MovieDetailBody(
                        detail: detail,
                        isInWatchlist: isInWatchlist,
                        onToggleWatchlist: onToggleWatchlist,
                        onCountryClick: onCountryClick
                    )
                }
                .onGeometryChange(for: CGFloat.self) { $0.size.height } action: { contentHeight = $0 }
                .onGeometryChange(for: CGFloat.self) { -$0.frame(in: .scrollView).minY } action: { offset = max(0, $0) }
                .padding(.bottom, extraBottom)
            }
            .onGeometryChange(for: CGFloat.self) { $0.size.height } action: { viewportHeight = $0 }
            .ignoresSafeArea(edges: .top)
            .overlay(alignment: .top) {
                // Edge to edge in landscape too; the title keeps clear of the side insets instead.
                CollapsingHeader(
                    movie: detail.summary,
                    expandedHeight: expanded,
                    collapsedHeight: collapsed,
                    fraction: fraction,
                    leadingInset: proxy.safeAreaInsets.leading,
                    trailingInset: proxy.safeAreaInsets.trailing
                )
                .ignoresSafeArea(edges: [.top, .horizontal])
                .allowsHitTesting(false)
            }
        }
    }
}
