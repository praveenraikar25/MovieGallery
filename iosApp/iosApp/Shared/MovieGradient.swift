import SwiftUI

/// The generated gradient shown where a movie has no image. It uses the same palettes as Android's
/// `MovieGradients` and the same `(id - 1) mod n` choice, so a title gets the same colours on both
/// platforms. It's theme, not copy, so it lives in Swift.
enum MovieGradient {
    private static let palettes: [[UInt32]] = [
        [0x2C3E50, 0x4CA1AF],
        [0x834D9B, 0xD04ED6],
        [0x1D4350, 0xA43931],
        [0x0F2027, 0x2C5364],
        [0x3A1C71, 0xD76D77, 0xFFAF7B],
        [0x141E30, 0x243B55],
        [0x360033, 0x0B8793],
        [0x4B134F, 0xC94B4B],
        [0x0B486B, 0xF56217],
        [0x1A2980, 0x26D0CE],
        [0x333333, 0xDD1818],
        [0x403A3E, 0xBE5869],
        [0x1F1C2C, 0x928DAB],
        [0x0F0C29, 0x302B63, 0x24243E],
        [0x2B5876, 0x4E4376],
        [0x614385, 0x516395],
        [0x525252, 0x3D7236],
        [0x000428, 0x004E92],
        [0x232526, 0x414345],
        [0x1B1B1B, 0x7B4397],
    ]

    /// Top-left to bottom-right, like Android's 135° gradient.
    static func gradient(for movieId: Int32) -> LinearGradient {
        let count = palettes.count
        let index = ((Int(movieId) - 1) % count + count) % count
        let colors = palettes[index].map { hex in
            Color(
                red: Double((hex >> 16) & 0xFF) / 255,
                green: Double((hex >> 8) & 0xFF) / 255,
                blue: Double(hex & 0xFF) / 255
            )
        }
        return LinearGradient(colors: colors, startPoint: .topLeading, endPoint: .bottomTrailing)
    }
}
