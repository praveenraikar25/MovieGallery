import Shared
import XCTest
@testable import iosApp

final class AppConfigurationTests: XCTestCase {
    func testReadsTheTokenAndVersion() {
        let config = AppConfiguration.make(info: ["TMDBReadAccessToken": "tmdb-token", "CFBundleVersion": "2"], isDebug: true)

        XCTAssertEqual(config.tmdbAccessToken, "tmdb-token")
        XCTAssertEqual(config.appVersion, "2")
        XCTAssertTrue(config.isDebug)
    }

    func testAMissingTokenIsEmptyRatherThanACrash() {
        let config = AppConfiguration.make(info: ["CFBundleVersion": "2"], isDebug: false)

        XCTAssertEqual(config.tmdbAccessToken, "")
        XCTAssertFalse(config.isDebug)
    }

    func testAMissingVersionIsZero() {
        XCTAssertEqual(AppConfiguration.make(info: [:], isDebug: false).appVersion, "0")
    }

    func testTheConfigNeverPrintsTheToken() {
        let config = AppConfiguration.make(info: ["TMDBReadAccessToken": "tmdb-token"], isDebug: false)

        XCTAssertFalse(config.description.contains("tmdb-token"))
    }
}
