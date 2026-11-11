import FirebaseCore
import GoogleSignIn
import os
import UIKit

private let log = Logger(subsystem: "com.raikar.moviegallery", category: "GoogleSignIn")

/// What the Google account chooser ended with, as Android's `GoogleSignInOutcome` does. `LoginView`
/// maps each to `onGoogleToken`, `onGoogleCancelled` or `onGoogleFailed`.
enum GoogleSignInOutcome: Equatable {
    case token(String)
    /// The user dismissed the chooser: stay on Login, show nothing (authentication "Cancelled Google sign-in").
    case cancelled
    case failed

    /// Sorts a Google Sign-In error: a dismissed chooser is a cancel, anything else a failure.
    init(error: Error) {
        let nsError = error as NSError
        let isCancel = nsError.domain == kGIDSignInErrorDomain && nsError.code == GIDSignInError.Code.canceled.rawValue
        self = isCancel ? .cancelled : .failed
    }
}

/// Whether the app is set up for Google Sign-In (add-ios-chat-and-google-signin design, decision 8).
/// The SDK raises an Objective-C exception, which Swift can't catch, when the URL scheme is missing,
/// so this is checked before every sign-in.
enum GoogleSignInSetup {
    /// The reversed client ID, the URL scheme Google redirects back to:
    /// `123-abc.apps.googleusercontent.com` becomes `com.googleusercontent.apps.123-abc`.
    static func reversedClientID(_ clientID: String) -> String {
        clientID.split(separator: ".").reversed().joined(separator: ".")
    }

    /// What's missing, for the console, or `nil` when Google Sign-In can run.
    static func problem(clientID: String?, urlSchemes: [String]) -> String? {
        guard let clientID, !clientID.isEmpty else {
            return "GoogleService-Info.plist has no CLIENT_ID. Download it again after enabling the Google provider."
        }
        guard urlSchemes.contains(reversedClientID(clientID)) else {
            return "No URL scheme matches the reversed client ID. Set GOOGLE_REVERSED_CLIENT_ID in Secrets.xcconfig."
        }
        return nil
    }

    /// The URL schemes `Info.plist` registers.
    static func registeredURLSchemes(info: [String: Any]) -> [String] {
        let types = info["CFBundleURLTypes"] as? [[String: Any]] ?? []
        return types.flatMap { $0["CFBundleURLSchemes"] as? [String] ?? [] }
    }

    /// Hands the Firebase client ID to Google Sign-In. Called once at launch, after `FirebaseApp.configure()`.
    static func configure() {
        guard let clientID = FirebaseApp.app()?.options.clientID else { return }
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
    }
}

/// Shows Google's account chooser and returns the ID token to exchange for a Firebase session, as
/// Android's `requestGoogleIdToken` does. The access token Firebase also needs on iOS stays with
/// `GIDSignIn.sharedInstance.currentUser`, where `FirebaseAuthService` reads it (decision 7).
@MainActor
enum GoogleSignInFlow {
    static func signIn() async -> GoogleSignInOutcome {
        let schemes = GoogleSignInSetup.registeredURLSchemes(info: Bundle.main.infoDictionary ?? [:])
        if let problem = GoogleSignInSetup.problem(clientID: FirebaseApp.app()?.options.clientID, urlSchemes: schemes) {
            log.error("Google sign-in isn't set up: \(problem, privacy: .public)")
            return .failed
        }
        guard let presenter = topViewController() else {
            log.error("Google sign-in failed: no window to present from")
            return .failed
        }
        do {
            let result = try await GIDSignIn.sharedInstance.signIn(withPresenting: presenter)
            guard let idToken = result.user.idToken?.tokenString else {
                log.error("Google sign-in failed: no ID token")
                return .failed
            }
            return .token(idToken)
        } catch {
            let outcome = GoogleSignInOutcome(error: error)
            // A misconfigured OAuth client can also surface here, so keep the detail visible.
            log.notice("Google sign-in \(outcome == .cancelled ? "cancelled" : "failed", privacy: .public): \(String(describing: error), privacy: .public)")
            return outcome
        }
    }

    private static func topViewController() -> UIViewController? {
        let window = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)
        var top = window?.rootViewController
        while let presented = top?.presentedViewController {
            top = presented
        }
        return top
    }
}
