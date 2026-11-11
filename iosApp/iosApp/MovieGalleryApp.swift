import FirebaseAppCheck
import FirebaseCore
import Shared
import SwiftUI

@main
struct MovieGalleryApp: App {
    var body: some Scene {
        WindowGroup {
            // As the unit tests' host the app starts nothing: no Firebase, no Koin. The tests cover
            // pure translation functions, and Firebase would need GoogleService-Info.plist.
            if ProcessInfo.processInfo.environment["XCTestConfigurationFilePath"] != nil {
                Color.clear
            } else {
                RootView(graph: AppEnvironment.shared.graph, session: AppEnvironment.shared.session.viewModel)
            }
        }
    }
}

/// Starts Firebase, then the shared Koin graph, once per launch (add-ios-app-shell design, decision 4).
///
/// A static rather than `@State`: `startKoin` may only run once, and SwiftUI may evaluate a
/// property's initial value more than once.
@MainActor
final class AppEnvironment {
    static let shared = AppEnvironment()

    let graph: MovieGalleryGraph

    /// The app-wide session, owned for the app's lifetime as the activity owns it on Android.
    let session: ViewModelOwner<AppSessionViewModel>

    private init() {
        // App Check must be installed before configure(). Firebase AI Logic rejects calls without it.
        #if DEBUG
            AppCheck.setAppCheckProviderFactory(AppCheckDebugProviderFactory())
        #else
            AppCheck.setAppCheckProviderFactory(AppAttestProviderFactory())
        #endif
        FirebaseApp.configure()
        GoogleSignInSetup.configure()

        // After configure(): AppSessionViewModel reads the auth bridge's currentUser() when it's created.
        let graph = IosKoinKt.startMovieGallery(
            config: AppConfiguration.make(info: Bundle.main.infoDictionary ?? [:], isDebug: AppConfiguration.isDebugBuild),
            authBridge: FirebaseAuthService(),
            aiBridge: FirebaseAiService()
        )
        self.graph = graph
        session = ViewModelOwner { graph.appSessionViewModel() }
    }
}

/// App Attest in release builds; the debug provider covers the simulator.
private final class AppAttestProviderFactory: NSObject, AppCheckProviderFactory {
    func createProvider(with app: FirebaseApp) -> AppCheckProvider? {
        AppAttestProvider(app: app)
    }
}
