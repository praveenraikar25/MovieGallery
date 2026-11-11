import Foundation
import Shared

/// Builds the `IosAppConfig` that `startMovieGallery` takes from the app's `Info.plist`
/// (add-ios-app-shell design, decision 3).
///
/// The TMDB token reaches `Info.plist` from `Configuration/Secrets.xcconfig` (git-ignored) and the
/// version from `CURRENT_PROJECT_VERSION`. A missing value never stops the app: an empty token makes
/// TMDB answer 401, which Home shows as "TMDB rejected the request", as on Android.
enum AppConfiguration {
    static let tokenKey = "TMDBReadAccessToken"
    static let versionKey = "CFBundleVersion"

    static func make(info: [String: Any], isDebug: Bool) -> IosAppConfig {
        IosAppConfig(
            tmdbAccessToken: (info[tokenKey] as? String) ?? "",
            appVersion: (info[versionKey] as? String) ?? "0",
            isDebug: isDebug
        )
    }

    /// Whether this is a debug build, for HTTP logging.
    static var isDebugBuild: Bool {
        #if DEBUG
            true
        #else
            false
        #endif
    }
}
