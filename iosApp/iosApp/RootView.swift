import Shared
import SwiftUI

/// Login when signed out, Home when signed in (app-navigation "App starts at login"). A successful
/// sign-in replaces Login with Home; there is no stack, so nothing leads back to Login
/// (authentication "Successful sign-in removes login from history").
struct RootView: View {
    let graph: MovieGalleryGraph
    @State private var signedIn: Bool

    init(graph: MovieGalleryGraph, isSignedIn: Bool) {
        self.graph = graph
        _signedIn = State(initialValue: isSignedIn)
    }

    var body: some View {
        if signedIn {
            HomeView(graph: graph)
        } else {
            LoginView(graph: graph) { signedIn = true }
        }
    }
}
