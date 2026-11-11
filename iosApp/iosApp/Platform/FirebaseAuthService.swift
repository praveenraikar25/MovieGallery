import FirebaseAuth
import Foundation
import GoogleSignIn
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

    /// Firebase on iOS needs the Google access token as well as the ID token Kotlin passes back. The
    /// shared bridge carries only the ID token, as Android's flow does, so the access token comes from
    /// the Google user `GoogleSignInFlow` has just signed in (add-ios-chat-and-google-signin design,
    /// decision 7). With no such user the call fails, and Kotlin shows "Google sign-in failed".
    func signInWithGoogle(idToken: String, completion: @escaping (BridgedAuthError?) -> Void) {
        guard let accessToken = GIDSignIn.sharedInstance.currentUser?.accessToken.tokenString else {
            completion(BridgedAuthError(domain: "MovieGallery", code: 0, message: "No signed-in Google user"))
            return
        }
        let credential = GoogleAuthProvider.credential(withIDToken: idToken, accessToken: accessToken)
        Auth.auth().signIn(with: credential) { _, error in
            completion(error.map(bridgedAuthError))
        }
    }

    /// Firebase's `signOut()` throws, but signing out can't fail on Android, so the error is dropped.
    /// The Google session ends too, so the next "Continue with Google" shows the account chooser.
    func signOut() {
        GIDSignIn.sharedInstance.signOut()
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
