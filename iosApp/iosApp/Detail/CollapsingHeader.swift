import Shared
import SwiftUI

/// The backdrop and title, driven by `fraction` (0 = expanded, 1 = pinned strip), as Android's
/// `CollapsingHeader` is (movie-detail "Header collapses into a pinned strip on scroll"). The
/// backdrop keeps its expanded size and is cropped by the shrinking header, so it never squashes.
struct CollapsingHeader: View {
    let movie: Movie
    let expandedHeight: CGFloat
    let collapsedHeight: CGFloat
    let fraction: CGFloat
    /// The side safe-area insets the header extends under (landscape), which the title stays inside.
    var leadingInset: CGFloat = 0
    var trailingInset: CGFloat = 0

    @State private var titleHeight: CGFloat = 0

    /// The title lands at about 18pt from 28pt, as on Android.
    private static let collapsedTitleScale: CGFloat = 0.64
    private static let collapsedScrim = 0.35
    private static let titleStartExpanded: CGFloat = 20
    /// Clears the back button in the navigation bar, with a gap.
    private static let titleStartCollapsed: CGFloat = 72
    private static let titleEnd: CGFloat = 20
    private static let titleBottomPadding: CGFloat = 20
    /// How far above the collapsed strip's bottom edge the back button's centre sits, so the
    /// collapsed title lines up with it (measured on iOS 26).
    private static let backButtonCenterFromBottom: CGFloat = 32

    var body: some View {
        let height = lerp(expandedHeight, collapsedHeight, fraction)
        let scale = lerp(1, Self.collapsedTitleScale, fraction)
        let titleX = leadingInset + lerp(Self.titleStartExpanded, Self.titleStartCollapsed, fraction)
        let scaledTitle = titleHeight * scale
        let expandedY = height - Self.titleBottomPadding - scaledTitle
        let collapsedY = max(0, collapsedHeight - Self.backButtonCenterFromBottom - scaledTitle / 2)

        GeometryReader { proxy in
            let titleWidth = max(0, (proxy.size.width - titleX - Self.titleEnd - trailingInset) / scale)
            ZStack(alignment: .topLeading) {
                MovieGradient.gradient(for: movie.id)
                if let url = movie.backdropUrl.flatMap(URL.init(string:)) {
                    AsyncImage(url: url) { phase in
                        phase.image?.resizable().scaledToFill()
                    }
                    .frame(width: proxy.size.width, height: expandedHeight)
                    .clipped()
                    .position(x: proxy.size.width / 2, y: height / 2)
                }
                LinearGradient(colors: [.clear, .black.opacity(0.45)], startPoint: .top, endPoint: .bottom)
                Color.black.opacity(Self.collapsedScrim * fraction)
                Text(movie.title)
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(.white)
                    .lineLimit(2)
                    .frame(width: titleWidth, alignment: .leading)
                    .fixedSize(horizontal: false, vertical: true)
                    .onGeometryChange(for: CGFloat.self) { $0.size.height } action: { titleHeight = $0 }
                    .scaleEffect(scale, anchor: .topLeading)
                    .offset(x: titleX, y: lerp(expandedY, collapsedY, fraction))
            }
        }
        .frame(height: height)
        .clipped()
    }

    private func lerp(_ start: CGFloat, _ end: CGFloat, _ t: CGFloat) -> CGFloat {
        start + (end - start) * t
    }
}
