@testable import iosApp
import XCTest

/// When Top TV Shows asks for the next page: within six tiles of the end, and never after a failure.
final class TvShowsPagingTests: XCTestCase {
    func testATileWellBeforeTheEndDoesNotLoad() {
        XCTAssertFalse(TvShowsPaging.shouldLoadMore(index: 5, count: 20, hasError: false))
    }

    func testATileJustOutsideTheThresholdDoesNotLoad() {
        XCTAssertFalse(TvShowsPaging.shouldLoadMore(index: 13, count: 20, hasError: false))
    }

    func testATileExactlyAtTheThresholdLoads() {
        XCTAssertTrue(TvShowsPaging.shouldLoadMore(index: 14, count: 20, hasError: false))
    }

    func testTheLastTileLoads() {
        XCTAssertTrue(TvShowsPaging.shouldLoadMore(index: 19, count: 20, hasError: false))
    }

    func testAShortFirstPageLoadsFromItsFirstTile() {
        XCTAssertTrue(TvShowsPaging.shouldLoadMore(index: 0, count: 4, hasError: false))
    }

    func testAFailedPageIsNeverRetriedAutomatically() {
        XCTAssertFalse(TvShowsPaging.shouldLoadMore(index: 19, count: 20, hasError: true))
    }

    func testAnIndexPastTheEndDoesNotLoad() {
        XCTAssertFalse(TvShowsPaging.shouldLoadMore(index: 20, count: 20, hasError: false))
    }
}
