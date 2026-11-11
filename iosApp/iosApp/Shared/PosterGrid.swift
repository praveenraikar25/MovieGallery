import Shared
import SwiftUI

/// The grid shared by Search, Watchlist and Top TV Shows (add-ios-browse-screens design, decision
/// 5): adaptive columns at least 150 points wide, as Android's `GridCells.Adaptive`.
struct PosterGrid: View {
    let items: [PosterGridItem]
    /// Makes tiles tappable when set; Top TV Shows leaves it nil (tv-shows "Shows are not interactive").
    var onTap: ((Int32) -> Void)?
    var showsBookmark = false
    /// Called with each tile's index as it appears, for paging.
    var onItemAppear: ((Int) -> Void)?

    private let columns = [GridItem(.adaptive(minimum: 150), spacing: 14, alignment: .top)]

    var body: some View {
        LazyVGrid(columns: columns, spacing: 20) {
            ForEach(Array(items.enumerated()), id: \.element.id) { index, item in
                tile(item)
                    .onAppear { onItemAppear?(index) }
            }
        }
        .padding(.horizontal, 20)
    }

    @ViewBuilder
    private func tile(_ item: PosterGridItem) -> some View {
        if let onTap {
            Button { onTap(item.id) } label: {
                PosterGridTile(item: item, showsBookmark: showsBookmark)
            }
            .buttonStyle(.plain)
        } else {
            PosterGridTile(item: item, showsBookmark: showsBookmark)
        }
    }
}

/// One grid tile: the poster, the title, then the shared "year · genres" subtitle, which truncates
/// so the rating beside it stays on one line.
struct PosterGridTile: View {
    let item: PosterGridItem
    var showsBookmark = false

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            MovieGradient.gradient(for: item.id)
                .aspectRatio(2 / 3, contentMode: .fit)
                .overlay {
                    AsyncImage(url: item.posterUrl.flatMap(URL.init(string:))) { phase in
                        if let image = phase.image {
                            image.resizable().scaledToFill()
                        }
                    }
                }
                .clipShape(RoundedRectangle(cornerRadius: 10))
                .overlay(alignment: .topTrailing) {
                    if showsBookmark {
                        Image(systemName: "bookmark.fill")
                            .font(.caption)
                            .foregroundStyle(.white)
                            .frame(width: 28, height: 28)
                            .background(.black.opacity(0.4), in: Circle())
                            .padding(8)
                    }
                }
                .accessibilityLabel(item.title)

            Text(item.title)
                .font(.subheadline.weight(.semibold))
                .lineLimit(1)
                .padding(.top, 5)

            HStack(spacing: 8) {
                Text(item.subtitle)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                    .frame(maxWidth: .infinity, alignment: .leading)
                if let rating = item.ratingLabel {
                    Text("★ \(rating)")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.tint)
                        .fixedSize()
                }
            }
        }
        .contentShape(Rectangle())
    }
}
