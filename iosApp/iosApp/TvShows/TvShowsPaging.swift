import Foundation

/// When Top TV Shows asks for the next page (add-ios-browse-screens design, decision 9). This
/// mirrors Android's `PREFETCH_THRESHOLD`: load once a tile this close to the end appears, but never
/// after a failure, which only the footer's Retry re-attempts (tv-shows "Load-more failure keeps
/// loaded shows"). The ViewModel ignores calls while a page is in flight or after the last page.
enum TvShowsPaging {
    static let prefetchThreshold = 6

    static func shouldLoadMore(index: Int, count: Int, hasError: Bool) -> Bool {
        !hasError && index < count && count - index <= prefetchThreshold
    }
}
