import Foundation

/// The four tabs, in the order the tab bar shows them (app-navigation "Main screen has four bottom tabs").
enum MainTab: Hashable {
    case home, search, watchlist, profile
}

/// The full-screen destinations a tab's stack can push. Both open with the tab bar hidden, and
/// back returns to the tab they were opened from (app-navigation "Full-screen destinations open
/// above the tabs").
enum Route: Hashable {
    case movie(Int32)
    case topTvShows
}
