import Shared
import SwiftUI

/// Email and password sign-in and account creation, on the shared `LoginViewModel`. Validation and
/// every message come from the ViewModel; the copy here matches Android's `LoginScreen`.
struct LoginView: View {
    let graph: MovieGalleryGraph
    let onSignedIn: () -> Void

    var body: some View {
        ViewModelScope(create: {
            ObservedViewModel<LoginViewModel, LoginUiState>({ graph.loginViewModel() }, state: { $0.uiState })
        }) { model in
            LoginContent(model: model, onSignedIn: onSignedIn)
        }
    }
}

private struct LoginContent: View {
    let model: ObservedViewModel<LoginViewModel, LoginUiState>
    let onSignedIn: () -> Void

    // The fields own their text and forward each edit to the ViewModel. Binding them straight to
    // `state` drops keystrokes: the new state reaches Swift on a later main-queue hop, and SwiftUI
    // re-reads the stale value in between. The ViewModel only echoes these values, never rewrites them.
    @State private var email = ""
    @State private var password = ""

    private var state: LoginUiState { model.state }
    private var isCreatingAccount: Bool { state.mode == LoginMode.createaccount }

    var body: some View {
        VStack(spacing: 16) {
            Spacer()
            Text("MovieGallery").font(.largeTitle.bold())
            Text("Discover movies you'll love.").foregroundStyle(.secondary)

            VStack(spacing: 12) {
                TextField("Email", text: $email)
                    .keyboardType(.emailAddress)
                    .textContentType(.username)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .onChange(of: email) { _, value in model.viewModel.onEmailChange(value: value) }
                SecureField("Password", text: $password)
                    .textContentType(isCreatingAccount ? .newPassword : .password)
                    .onChange(of: password) { _, value in model.viewModel.onPasswordChange(value: value) }
            }
            .textFieldStyle(.roundedBorder)
            .padding(.top, 24)

            if let message = state.errorMessage {
                Text(message)
                    .foregroundStyle(.red)
                    .font(.callout)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }

            Button {
                model.viewModel.onSubmit()
            } label: {
                Group {
                    if state.isLoading {
                        ProgressView()
                    } else {
                        Text(isCreatingAccount ? "Create Account" : "Log In")
                    }
                }
                .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
            .disabled(state.isLoading)

            Button(isCreatingAccount ? "Already have an account? Log in" : "Don't have an account? Create account") {
                model.viewModel.onToggleMode()
            }
            .disabled(state.isLoading)
            Spacer()
        }
        .padding(24)
        .onChange(of: state.isAuthenticated, initial: true) { _, isAuthenticated in
            if isAuthenticated { onSignedIn() }
        }
    }
}
