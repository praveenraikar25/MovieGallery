import Shared
import SwiftUI

/// The "Cast" heading and its horizontally scrolling row of tiles. Shown only for a non-empty cast.
struct CastRow: View {
    let cast: [CastMember]

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Cast")
                .font(.headline)
                .padding(.horizontal, 20)
            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(alignment: .top, spacing: 8) {
                    ForEach(Array(cast.enumerated()), id: \.offset) { _, member in
                        CastTile(member: member)
                    }
                }
                .padding(.horizontal, 20)
            }
        }
    }
}

private struct CastTile: View {
    let member: CastMember

    var body: some View {
        VStack(spacing: 8) {
            InitialAvatar(text: member.initial, muted: true)
            // Two lines each, reserved even when shorter, so every tile is the same height.
            VStack(spacing: 2) {
                Text(member.name)
                    .font(.subheadline)
                    .lineLimit(2, reservesSpace: true)
                Text(member.role)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(2, reservesSpace: true)
            }
            .multilineTextAlignment(.center)
        }
        .frame(width: 88)
    }
}
