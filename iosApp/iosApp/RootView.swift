import GoogleSignIn
import Shared
import SwiftUI

/// Login when signed out, the tabs when signed in (app-navigation "App starts at login"). There is
/// no stack between the two, so nothing leads back across them: a sign-in replaces Login
/// (authentication "Successful sign-in removes login from history"), and a sign-out drops the tabs,
/// clearing every tab's ViewModel and stack, for a fresh, empty Login (authentication "Sign-out
/// returns to login and clears history"; add-ios-browse-screens design, decision 2).
struct RootView: View {
    let graph: MovieGalleryGraph
    let session: AppSessionViewModel
    @State private var signedIn: Bool

    init(graph: MovieGalleryGraph, session: AppSessionViewModel) {
        self.graph = graph
        self.session = session
        _signedIn = State(initialValue: session.isSignedIn)
    }

    var body: some View {
        Group {
            if signedIn {
                MainTabView(graph: graph) {
                    session.signOut()
                    signedIn = false
                }
            } else {
                LoginView(graph: graph) { signedIn = true }
            }
        }
        // Google Sign-In returns through the reversed-client-ID URL scheme.
        .onOpenURL { GIDSignIn.sharedInstance.handle($0) }
    }
}
