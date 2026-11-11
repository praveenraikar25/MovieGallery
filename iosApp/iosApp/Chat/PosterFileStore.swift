import Foundation
import UIKit

/// Where a picked or captured poster is written before it goes to `sendPoster(imageUri:)`, which
/// takes a `file://` URL (add-ios-chat-and-google-signin design, decision 4). The files stay after
/// sending because the chat bubble still shows them, and `prune` removes old ones, as Android's
/// `prunePosterCaptures` does.
///
/// A file is written only once the user has picked or captured an image, so cancelling the camera
/// or the photo picker leaves nothing behind (ai-chat "Cancelling the camera", "Cancelling the gallery").
struct PosterFileStore {
    /// Long enough to read a poster, small enough to keep the request light. Library and camera
    /// images are often 12 MP or more, and HEIC, which this re-encodes as JPEG.
    static let maxPixelEdge: CGFloat = 1600
    static let jpegQuality: CGFloat = 0.85
    /// Captures older than this are from an earlier visit, and nothing can still show them.
    static let pruneAge: TimeInterval = 24 * 60 * 60

    let directory: URL
    var now: () -> Date = Date.init

    /// `Caches/poster-captures`, which the system may clear when space runs low.
    static func standard() -> PosterFileStore {
        let caches = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask)[0]
        return PosterFileStore(directory: caches.appendingPathComponent("poster-captures", isDirectory: true))
    }

    /// Writes [data] as a downscaled JPEG and returns its `file://` URL string.
    ///
    /// Data that can't be decoded is written unchanged: the AI bridge then fails to load it and
    /// reports an unreadable image, so the shared error copy and Retry appear, with no Swift-only copy.
    func save(_ data: Data) throws -> String {
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let file = directory.appendingPathComponent("poster-\(Int64(now().timeIntervalSince1970 * 1000)).jpg")
        let output = UIImage(data: data).flatMap { Self.downscaled($0).jpegData(compressionQuality: Self.jpegQuality) }
        try (output ?? data).write(to: file, options: .atomic)
        return file.absoluteString
    }

    /// Writes an image the camera returned, as `save` does.
    func save(_ image: UIImage) throws -> String {
        guard let data = Self.downscaled(image).jpegData(compressionQuality: Self.jpegQuality) else {
            throw CocoaError(.fileWriteUnknown)
        }
        return try save(data)
    }

    /// Deletes files last modified more than [age] ago. Blocking I/O; call it off the main thread.
    func prune(olderThan age: TimeInterval = pruneAge) {
        let cutoff = now().addingTimeInterval(-age)
        let files = (try? FileManager.default.contentsOfDirectory(
            at: directory,
            includingPropertiesForKeys: [.contentModificationDateKey]
        )) ?? []
        for file in files {
            let modified = (try? file.resourceValues(forKeys: [.contentModificationDateKey]))?.contentModificationDate
            if let modified, modified < cutoff {
                try? FileManager.default.removeItem(at: file)
            }
        }
    }

    /// Scales so the longest edge is at most `maxPixelEdge` pixels, and bakes in the orientation.
    static func downscaled(_ image: UIImage) -> UIImage {
        let pixelSize = CGSize(width: image.size.width * image.scale, height: image.size.height * image.scale)
        let factor = min(1, maxPixelEdge / max(pixelSize.width, pixelSize.height, 1))
        let target = CGSize(width: (pixelSize.width * factor).rounded(), height: (pixelSize.height * factor).rounded())
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        return UIGraphicsImageRenderer(size: target, format: format).image { _ in
            image.draw(in: CGRect(origin: .zero, size: target))
        }
    }
}
