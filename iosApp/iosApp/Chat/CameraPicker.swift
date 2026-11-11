import AVFoundation
import SwiftUI
import UIKit

/// Shown, with Dismiss and no Retry, when camera access is off. iOS only: Android's camera needs no
/// permission, so this isn't shared copy (ai-chat "Camera access denied on iOS").
let cameraAccessOffMessage = "Camera access is off. Turn it on in Settings to take a photo."

/// What "Take Photo" does for a given camera authorization (add-ios-chat-and-google-signin design,
/// decision 3). A pure mapping, so it is unit-tested without a camera.
enum CameraAccess: Equatable {
    case open
    case ask
    case denied

    init(_ status: AVAuthorizationStatus) {
        switch status {
        case .authorized: self = .open
        case .notDetermined: self = .ask
        case .denied, .restricted: self = .denied
        @unknown default: self = .denied
        }
    }

    /// Whether this device has a camera at all; false on the simulator, which hides "Take Photo".
    static var isCameraAvailable: Bool {
        UIImagePickerController.isSourceTypeAvailable(.camera)
    }

    /// Resolves the current authorization, asking the first time. Returns whether the camera may open.
    @MainActor
    static func resolve() async -> Bool {
        switch CameraAccess(AVCaptureDevice.authorizationStatus(for: .video)) {
        case .open: true
        case .denied: false
        case .ask: await AVCaptureDevice.requestAccess(for: .video)
        }
    }
}

/// The system camera. SwiftUI has none, so this wraps `UIImagePickerController`. It calls
/// [onCapture] with the photo, or with `nil` when the user backs out, and nothing is written then.
struct CameraPicker: UIViewControllerRepresentable {
    let onCapture: (UIImage?) -> Void

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.sourceType = .camera
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(onCapture: onCapture)
    }

    final class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let onCapture: (UIImage?) -> Void

        init(onCapture: @escaping (UIImage?) -> Void) {
            self.onCapture = onCapture
        }

        func imagePickerController(
            _ picker: UIImagePickerController,
            didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]
        ) {
            onCapture(info[.originalImage] as? UIImage)
        }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            onCapture(nil)
        }
    }
}
