import FirebaseAILogic
import Foundation
import Shared
import UIKit

/// Firebase AI Logic for the shared code: adopts `FirebaseAiBridge` with the Firebase iOS SDK
/// (add-ios-app-shell design, decision 5). The model, timeout and prompts come from the shared
/// `ChatModelConfig`, so both apps send Gemini the same instructions.
final class FirebaseAiService: NSObject, FirebaseAiBridge {
    /// Built on first use, so Firebase is already configured.
    private lazy var model: GenerativeModel = {
        let config = ChatModelConfig.shared
        return FirebaseAI.firebaseAI(backend: .googleAI()).generativeModel(
            modelName: config.MODEL_NAME,
            systemInstruction: ModelContent(role: "system", parts: config.systemPrompt(platformName: "iOS")),
            requestOptions: RequestOptions(timeout: TimeInterval(config.REQUEST_TIMEOUT_MILLIS) / 1000)
        )
    }()

    func sendMessage(
        history: [ChatTurn],
        message: String,
        completion: @escaping (String?, BridgedAiFailure?) -> Void
    ) {
        let chat = model.startChat(history: history.map(modelContent))
        reply(completion) { try await chat.sendMessage(message) }
    }

    func identifyPoster(
        history: [ChatTurn],
        imageUri: String,
        completion: @escaping (String?, BridgedAiFailure?) -> Void
    ) {
        guard let url = URL(string: imageUri), let image = UIImage(contentsOfFile: url.path) else {
            completion(nil, BridgedAiFailure(kind: .imageunreadable, message: "Couldn't read the image"))
            return
        }
        let chat = model.startChat(history: history.map(modelContent))
        reply(completion) { try await chat.sendMessage(image, ChatModelConfig.shared.IDENTIFY_POSTER_PROMPT) }
    }

    /// Runs one request and calls [completion] exactly once, on the main actor.
    private func reply(
        _ completion: @escaping (String?, BridgedAiFailure?) -> Void,
        _ request: @escaping () async throws -> GenerateContentResponse
    ) {
        Task { @MainActor in
            do {
                completion(try await request().text, nil)
            } catch {
                completion(nil, BridgedAiFailure(kind: aiFailureKind(error), message: error.localizedDescription))
            }
        }
    }
}

/// A fresh chat is seeded with the history on every call, as Android's `GeminiChatDataSource` does.
private func modelContent(_ turn: ChatTurn) -> ModelContent {
    ModelContent(role: turn.isUser ? "user" : "model", parts: turn.text)
}

/// The `NSError` domain Firebase AI Logic 13 gives HTTP errors from the backend; the code is the HTTP status.
let firebaseAiBackendErrorDomain = "com.google.firebase.firebaseai.BackendError"

/// Sorts a Firebase AI failure into the kind Kotlin maps to an `AppError`, matching how Android's
/// `safeAiCall` treats the equivalent exception.
///
/// Firebase AI Logic 13.1.1 wraps every failure that isn't a `GenerateContentError` in
/// `.internalError(underlying:)`: URL errors, JSON decoding errors and backend HTTP errors.
/// - A 403 (API not enabled or the service disabled) or a 400 naming the API key is `.unauthorized`,
///   as Android's `ServiceDisabledException` / `InvalidAPIKeyException` are. An App Check rejection
///   (401) stays `.other`, as it does on Android.
/// - A timed-out URL request is `.timeout`, not `.network`: the model stalling isn't the device
///   being offline.
func aiFailureKind(_ error: Error) -> AiFailureKind {
    var cause = error
    if case let GenerateContentError.internalError(underlying) = error {
        cause = underlying
    }
    if cause is DecodingError {
        return .serialization
    }
    let nsError = cause as NSError
    if nsError.domain == NSURLErrorDomain {
        return nsError.code == NSURLErrorTimedOut ? .timeout : .network
    }
    if nsError.domain == firebaseAiBackendErrorDomain {
        if nsError.code == 403 || (nsError.code == 400 && nsError.localizedDescription.contains("API key")) {
            return .unauthorized
        }
    }
    return .other
}
