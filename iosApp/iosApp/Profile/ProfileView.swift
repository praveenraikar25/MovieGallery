import Shared
import SwiftUI

/// The signed-in account and the hub for the AI chat, Top TV Shows, the settings rows and sign-out
/// (profile; app-navigation "Secondary features are reached from Profile").
struct ProfileView: View {
    let graph: MovieGalleryGraph
    let onSignOut: () -> Void

    var body: some View {
        ViewModelScope(create: {
            ObservedViewModel<ProfileViewModel, ProfileUiState>({ graph.profileViewModel() }, state: { $0.uiState })
        }) { model in
            ProfileContent(state: model.state, onSignOut: onSignOut)
        }
    }
}

private struct ProfileContent: View {
    let state: ProfileUiState
    let onSignOut: () -> Void

    var body: some View {
        List {
            VStack(spacing: 4) {
                InitialAvatar(text: state.initial)
                    .padding(.bottom, 6)
                // The ViewModel leaves `name` null when the account has none, or only whitespace.
                if let name = state.name {
                    Text(name).font(.headline)
                }
                Text(state.email)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity)
            .listRowBackground(Color.clear)

            Section {
                NavigationLink(value: Route.chat) {
                    Label {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Find Movies with AI")
                            Text("Chat with an assistant for personalized picks")
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                        }
                    } icon: {
                        Image(systemName: "sparkles")
                    }
                }
            } header: {
                // Spelled in capitals: iOS 26 no longer uppercases list headers, and Android shows "DISCOVER".
                Text("DISCOVER")
            }

            Section {
                NavigationLink("Top TV Shows", value: Route.topTvShows)
                // Placeholders with no destination yet, as on Android (profile "Placeholder entries are inert").
                InertRow(label: "Preferences")
                InertRow(label: "Notifications")
                InertRow(label: "About")
                Button("Sign Out", role: .destructive, action: onSignOut)
            }
        }
        .navigationTitle("Profile")
    }
}

/// A row that looks like the others, chevron included, but does nothing when tapped.
private struct InertRow: View {
    let label: String

    var body: some View {
        HStack {
            Text(label)
            Spacer()
            Image(systemName: "chevron.right")
                .font(.footnote.weight(.semibold))
                .foregroundStyle(.tertiary)
        }
    }
}
