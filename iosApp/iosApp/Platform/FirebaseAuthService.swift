import FirebaseAuth
import Foundation
import Shared

/// Firebase Auth for the shared code: adopts `FirebaseAuthBridge` with the Firebase iOS SDK
/// (add-ios-app-shell design, decision 5).
///
/// It only translates. Errors pass through untouched as domain, code and description, and Kotlin's
/// `mapBridgedAuthError` decides what they mean, so iOS shows the same messages as Android.
final class FirebaseAuthService: NSObject, FirebaseAuthBridge {
    func currentUser() -> BridgedUser? {
        Auth.auth().currentUser.map(bridgedUser)
    }

    func addAuthStateListener(onChange: @escaping (BridgedUser?) -> Void) -> any BridgeRegistration {
        let handle = Auth.auth().addStateDidChangeListener { _, user in onChange(user.map(bridgedUser)) }
        return ListenerRegistration { Auth.auth().removeStateDidChangeListener(handle) }
    }

    func signIn(email: String, password: String, completion: @escaping (BridgedAuthError?) -> Void) {
        Auth.auth().signIn(withEmail: email, password: password) { _, error in
            completion(error.map(bridgedAuthError))
        }
    }

    func signUp(email: String, password: String, completion: @escaping (BridgedAuthError?) -> Void) {
        Auth.auth().createUser(withEmail: email, password: password) { _, error in
            completion(error.map(bridgedAuthError))
        }
    }

    /// Not available until Google Sign-In arrives in phase 5c; no 5b screen can call it. Kotlin maps
    /// this to `AuthError.Unknown`.
    func signInWithGoogle(idToken: String, completion: @escaping (BridgedAuthError?) -> Void) {
        completion(BridgedAuthError(domain: "MovieGallery", code: 0, message: "Google sign-in isn't available on iOS yet"))
    }

    /// Firebase's `signOut()` throws, but signing out can't fail on Android, so the error is dropped.
    func signOut() {
        try? Auth.auth().signOut()
    }

    func idToken(forceRefresh: Bool, completion: @escaping (String?) -> Void) {
        guard let user = Auth.auth().currentUser else {
            completion(nil)
            return
        }
        user.getIDTokenForcingRefresh(forceRefresh) { token, _ in completion(token) }
    }
}

private func bridgedUser(_ user: User) -> BridgedUser {
    BridgedUser(uid: user.uid, email: user.email, displayName: user.displayName)
}

/// An `NSError`'s domain, code and description, exactly as the SDK reported them.
func bridgedAuthError(_ error: Error) -> BridgedAuthError {
    let nsError = error as NSError
    return BridgedAuthError(domain: nsError.domain, code: Int64(nsError.code), message: nsError.localizedDescription)
}

/// Ends one auth-state listener when Kotlin stops collecting.
private final class ListenerRegistration: NSObject, BridgeRegistration {
    private let onRemove: () -> Void

    init(onRemove: @escaping () -> Void) {
        self.onRemove = onRemove
    }

    func remove() {
        onRemove()
    }
}
