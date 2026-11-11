import SwiftUI

/// A circle holding an account's or cast member's initial, as Android's `InitialAvatar` does. The
/// initial itself always comes from the shared code.
struct InitialAvatar: View {
    let text: String
    var size: CGFloat = 68
    var muted = false

    var body: some View {
        Text(text)
            .font(.system(size: size * 0.42, weight: .semibold))
            .foregroundStyle(muted ? AnyShapeStyle(.secondary) : AnyShapeStyle(.white))
            .frame(width: size, height: size)
            .background(muted ? AnyShapeStyle(.quaternary) : AnyShapeStyle(.tint), in: Circle())
    }
}
