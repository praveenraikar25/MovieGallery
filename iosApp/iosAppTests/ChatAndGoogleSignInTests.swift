import AVFoundation
import GoogleSignIn
import UIKit
import XCTest
@testable import iosApp

/// The pure parts of the iOS chat and Google sign-in (add-ios-chat-and-google-signin design,
/// decisions 3, 4, 6 and 8). The screens themselves are checked on the simulator.
final class ChatAndGoogleSignInTests: XCTestCase {
    // MARK: - Google Sign-In setup

    func testTheReversedClientIDFlipsTheDomainParts() {
        XCTAssertEqual(
            GoogleSignInSetup.reversedClientID("123-abc.apps.googleusercontent.com"),
            "com.googleusercontent.apps.123-abc"
        )
    }

    func testNoClientIDIsAProblem() {
        XCTAssertNotNil(GoogleSignInSetup.problem(clientID: nil, urlSchemes: ["com.googleusercontent.apps.123-abc"]))
        XCTAssertNotNil(GoogleSignInSetup.problem(clientID: "", urlSchemes: []))
    }

    func testAMissingURLSchemeIsAProblem() {
        XCTAssertNotNil(GoogleSignInSetup.problem(clientID: "123-abc.apps.googleusercontent.com", urlSchemes: []))
    }

    func testAnEmptyBuildSettingIsAProblem() {
        // An unset GOOGLE_REVERSED_CLIENT_ID leaves an empty scheme in Info.plist.
        XCTAssertNotNil(GoogleSignInSetup.problem(clientID: "123-abc.apps.googleusercontent.com", urlSchemes: [""]))
    }

    func testAMismatchedURLSchemeIsAProblem() {
        XCTAssertNotNil(
            GoogleSignInSetup.problem(
                clientID: "123-abc.apps.googleusercontent.com",
                urlSchemes: ["com.googleusercontent.apps.999-zzz"]
            )
        )
    }

    func testAMatchingSchemeIsReady() {
        XCTAssertNil(
            GoogleSignInSetup.problem(
                clientID: "123-abc.apps.googleusercontent.com",
                urlSchemes: ["other", "com.googleusercontent.apps.123-abc"]
            )
        )
    }

    func testRegisteredSchemesAreReadFromEveryURLType() {
        let info: [String: Any] = [
            "CFBundleURLTypes": [
                ["CFBundleURLSchemes": ["one"]],
                ["CFBundleURLSchemes": ["two", "three"]],
            ],
        ]

        XCTAssertEqual(GoogleSignInSetup.registeredURLSchemes(info: info), ["one", "two", "three"])
        XCTAssertEqual(GoogleSignInSetup.registeredURLSchemes(info: [:]), [])
    }

    // MARK: - Google Sign-In outcome

    func testADismissedChooserIsACancel() {
        let error = NSError(domain: kGIDSignInErrorDomain, code: GIDSignInError.Code.canceled.rawValue)

        XCTAssertEqual(GoogleSignInOutcome(error: error), .cancelled)
    }

    func testAnyOtherGoogleErrorIsAFailure() {
        let error = NSError(domain: kGIDSignInErrorDomain, code: GIDSignInError.Code.keychain.rawValue)

        XCTAssertEqual(GoogleSignInOutcome(error: error), .failed)
    }

    func testAnOfflineErrorIsAFailure() {
        XCTAssertEqual(GoogleSignInOutcome(error: URLError(.notConnectedToInternet)), .failed)
    }

    // MARK: - Camera access

    func testCameraAuthorizationMapsToWhatTakePhotoDoes() {
        XCTAssertEqual(CameraAccess(.authorized), .open)
        XCTAssertEqual(CameraAccess(.notDetermined), .ask)
        XCTAssertEqual(CameraAccess(.denied), .denied)
        XCTAssertEqual(CameraAccess(.restricted), .denied)
    }

    // MARK: - Poster file store

    private var directory: URL!

    override func setUpWithError() throws {
        directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
    }

    override func tearDownWithError() throws {
        try? FileManager.default.removeItem(at: directory)
    }

    func testALargeImageIsSavedAsADownscaledJPEGFileURL() throws {
        let store = PosterFileStore(directory: directory, now: { Date(timeIntervalSince1970: 1000) })
        let data = try XCTUnwrap(image(width: 4000, height: 3000).pngData())

        let uri = try store.save(data)

        let url = try XCTUnwrap(URL(string: uri))
        XCTAssertTrue(url.isFileURL)
        XCTAssertEqual(url.lastPathComponent, "poster-1000000.jpg")
        let saved = try XCTUnwrap(UIImage(contentsOfFile: url.path))
        XCTAssertEqual(max(saved.size.width * saved.scale, saved.size.height * saved.scale), 1600)
    }

    func testASmallImageKeepsItsSize() throws {
        let store = PosterFileStore(directory: directory)

        let uri = try store.save(image(width: 300, height: 450))

        let saved = try XCTUnwrap(UIImage(contentsOfFile: try XCTUnwrap(URL(string: uri)).path))
        XCTAssertEqual(saved.size.width * saved.scale, 300)
        XCTAssertEqual(saved.size.height * saved.scale, 450)
    }

    func testUndecodableDataIsWrittenUnchangedSoTheBridgeReportsItUnreadable() throws {
        let store = PosterFileStore(directory: directory)
        let junk = Data("not an image".utf8)

        let uri = try store.save(junk)

        let path = try XCTUnwrap(URL(string: uri)).path
        XCTAssertEqual(try Data(contentsOf: URL(fileURLWithPath: path)), junk)
        XCTAssertNil(UIImage(contentsOfFile: path))
    }

    func testPruneRemovesOnlyFilesOlderThanADay() throws {
        let now = Date()
        let store = PosterFileStore(directory: directory, now: { now })
        let old = try XCTUnwrap(URL(string: try store.save(image(width: 10, height: 10))))
        try FileManager.default.setAttributes(
            [.modificationDate: now.addingTimeInterval(-PosterFileStore.pruneAge - 60)],
            ofItemAtPath: old.path
        )
        let fresh = try XCTUnwrap(URL(string: try PosterFileStore(directory: directory, now: { now.addingTimeInterval(1) })
                .save(image(width: 10, height: 10))))

        store.prune()

        XCTAssertFalse(FileManager.default.fileExists(atPath: old.path))
        XCTAssertTrue(FileManager.default.fileExists(atPath: fresh.path))
    }

    func testPruningAMissingDirectoryDoesNothing() {
        PosterFileStore(directory: directory).prune()
    }

    private func image(width: CGFloat, height: CGFloat) -> UIImage {
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        return UIGraphicsImageRenderer(size: CGSize(width: width, height: height), format: format).image { context in
            UIColor.systemTeal.setFill()
            context.fill(CGRect(x: 0, y: 0, width: width, height: height))
        }
    }
}
