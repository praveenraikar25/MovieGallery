import FirebaseAILogic
import Shared
import XCTest
@testable import iosApp

/// The Swift bridges only translate; these pin the translation. What each value then means is
/// Kotlin's job and is tested there (IosAuthRepositoryTest, IosAiChatRepositoryTest).
final class BridgeTranslationTests: XCTestCase {
    // MARK: - Auth

    func testAFirebaseErrorPassesThroughUntouched() {
        let error = NSError(
            domain: "FIRAuthErrorDomain",
            code: 17009,
            userInfo: [NSLocalizedDescriptionKey: "The password is invalid."]
        )

        let bridged = bridgedAuthError(error)

        XCTAssertEqual(bridged.domain, "FIRAuthErrorDomain")
        XCTAssertEqual(bridged.code, 17009)
        XCTAssertEqual(bridged.message, "The password is invalid.")
    }

    func testAnOfflineErrorKeepsItsURLDomain() {
        let bridged = bridgedAuthError(URLError(.notConnectedToInternet))

        XCTAssertEqual(bridged.domain, NSURLErrorDomain)
        XCTAssertEqual(bridged.code, Int64(NSURLErrorNotConnectedToInternet))
    }

    func testAPlainSwiftErrorStillBridges() {
        struct Oops: Error {}

        let bridged = bridgedAuthError(Oops())

        XCTAssertFalse(bridged.domain.isEmpty)
        XCTAssertFalse(bridged.message.isEmpty)
    }

    // MARK: - AI

    func testATimedOutRequestIsATimeoutNotANetworkFailure() {
        XCTAssertEqual(aiFailureKind(GenerateContentError.internalError(underlying: URLError(.timedOut))), .timeout)
    }

    func testOtherURLErrorsAreNetwork() {
        XCTAssertEqual(aiFailureKind(GenerateContentError.internalError(underlying: URLError(.notConnectedToInternet))), .network)
        XCTAssertEqual(aiFailureKind(URLError(.networkConnectionLost)), .network)
    }

    func testADecodingErrorIsSerialization() {
        let decoding = DecodingError.dataCorrupted(.init(codingPath: [], debugDescription: "bad JSON"))

        XCTAssertEqual(aiFailureKind(GenerateContentError.internalError(underlying: decoding)), .serialization)
    }

    func testAServiceDisabledOrBadKeyIsUnauthorized() {
        XCTAssertEqual(aiFailureKind(GenerateContentError.internalError(underlying: backendError(403, "Firebase AI Logic API has not been used"))), .unauthorized)
        XCTAssertEqual(aiFailureKind(GenerateContentError.internalError(underlying: backendError(400, "API key not valid. Please pass a valid API key."))), .unauthorized)
    }

    func testAnAppCheckRejectionIsOtherAsOnAndroid() {
        XCTAssertEqual(aiFailureKind(GenerateContentError.internalError(underlying: backendError(401, "App attestation failed."))), .other)
    }

    func testAQuotaErrorIsOther() {
        XCTAssertEqual(aiFailureKind(GenerateContentError.internalError(underlying: backendError(429, "Resource has been exhausted"))), .other)
    }

    func testAnUnrelatedErrorIsOther() {
        struct Oops: Error {}

        XCTAssertEqual(aiFailureKind(Oops()), .other)
    }

    /// How Firebase AI Logic 13 bridges a backend HTTP error to NSError.
    private func backendError(_ status: Int, _ message: String) -> NSError {
        NSError(domain: firebaseAiBackendErrorDomain, code: status, userInfo: [NSLocalizedDescriptionKey: message])
    }
}
