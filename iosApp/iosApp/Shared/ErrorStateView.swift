import Shared
import SwiftUI

/// The full-screen error, with the same title and detail Android shows for the error.
struct ErrorStateView: View {
    let error: any AppError
    let onRetry: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle")
                .font(.largeTitle)
                .foregroundStyle(.secondary)
            Text(ErrorCopyKt.errorTitle(error))
                .font(.headline)
            Text(ErrorCopyKt.errorDetail(error))
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Button("Retry", action: onRetry)
                .buttonStyle(.bordered)
                .padding(.top, 8)
        }
        .padding(32)
    }
}
