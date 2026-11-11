import Shared
import SwiftUI

/// The signed-in main screen: four tabs, each with its own navigation stack (add-ios-browse-screens
/// design, decision 1). `TabView` keeps every tab alive while another is shown, so each tab's
/// ViewModel, text and stack survive a switch (app-navigation "Tabs retain their state").
struct MainTabView: View {
    let graph: MovieGalleryGraph
    let onSignOut: () -> Void

    @State private var selection: MainTab = .home
    @State private var homePath: [Route] = []
    @State private var searchPath: [Route] = []
    @State private var watchlistPath: [Route] = []
    @State private var profilePath: [Route] = []

    var body: some View {
        TabView(selection: $selection) {
            NavigationStack(path: $homePath) {
                HomeView(
                    graph: graph,
                    onMovieClick: { homePath.append(.movie($0)) },
                    onProfileClick: { selection = .profile }
                )
                .routeDestinations(graph: graph)
            }
            .tabItem { Label("Home", systemImage: "house") }
            .tag(MainTab.home)

            NavigationStack(path: $searchPath) {
                SearchView(graph: graph, onMovieClick: { searchPath.append(.movie($0)) })
                    .routeDestinations(graph: graph)
            }
            .tabItem { Label("Search", systemImage: "magnifyingglass") }
            .tag(MainTab.search)

            NavigationStack(path: $watchlistPath) {
                WatchlistView(graph: graph, onMovieClick: { watchlistPath.append(.movie($0)) })
                    .routeDestinations(graph: graph)
            }
            .tabItem { Label("Watchlist", systemImage: "bookmark") }
            .tag(MainTab.watchlist)

            NavigationStack(path: $profilePath) {
                ProfileView(graph: graph, onSignOut: onSignOut)
                    .routeDestinations(graph: graph)
            }
            .tabItem { Label("Profile", systemImage: "person.crop.circle") }
            .tag(MainTab.profile)
        }
    }
}

private extension View {
    /// Registers every `Route` on the enclosing stack, so any tab can push any destination.
    func routeDestinations(graph: MovieGalleryGraph) -> some View {
        navigationDestination(for: Route.self) { route in
            switch route {
            case let .movie(id):
                MovieDetailView(graph: graph, movieId: id)
            case .topTvShows:
                TopTvShowsView(graph: graph)
            case .chat:
                AiChatView(graph: graph)
            }
        }
    }
}
