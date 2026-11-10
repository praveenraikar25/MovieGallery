package com.raikar.moviegallery.platform

/**
 * Firebase Auth as the iOS app supplies it. Swift adopts this protocol with the Firebase iOS SDK
 * and passes it to `startMovieGallery`. Kotlin's `IosAuthRepository` and `IosAuthTokenProvider`
 * build on it, and own the error mapping and the `Flow` and `suspend` shapes, so Swift only
 * forwards SDK results (add-ios-platform-layer design, decisions 1 and 2).
 *
 * Every completion must be called exactly once. It may be called on any queue. A second call is
 * ignored.
 */
interface FirebaseAuthBridge {
    /** `Auth.auth().currentUser`, which Firebase restores from the keychain at `configure()`. */
    fun currentUser(): BridgedUser?

    /**
     * `addStateDidChangeListener`: [onChange] receives the user on every sign-in, sign-out or
     * account switch. Kotlin calls [BridgeRegistration.remove] when it stops collecting, and Swift
     * then calls `removeStateDidChangeListener`.
     */
    fun addAuthStateListener(onChange: (BridgedUser?) -> Unit): BridgeRegistration

    /** `signIn(withEmail:password:)`: completes with `null` on success, else the SDK's error. */
    fun signIn(
        email: String,
        password: String,
        completion: (BridgedAuthError?) -> Unit,
    )

    /** `createUser(withEmail:password:)`: completes with `null` on success, else the SDK's error. */
    fun signUp(
        email: String,
        password: String,
        completion: (BridgedAuthError?) -> Unit,
    )

    /**
     * Signs in with a Google credential built from [idToken]: completes with `null` on success,
     * else the SDK's error. The token comes from Google Sign-In in the UI layer, as on Android.
     */
    fun signInWithGoogle(
        idToken: String,
        completion: (BridgedAuthError?) -> Unit,
    )

    /** `signOut()`. Swift swallows the error it throws: on Android, signing out can't fail. */
    fun signOut()

    /**
     * `currentUser?.getIDTokenForcingRefresh(forceRefresh)`: completes with the token, or `null`
     * when nobody is signed in or no token could be obtained.
     */
    fun idToken(
        forceRefresh: Boolean,
        completion: (String?) -> Unit,
    )
}

/** The fields of a Firebase `User` the app uses. */
data class BridgedUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
)

/**
 * A Firebase Auth failure, passed through untouched: the `NSError`'s `domain`, `code` and
 * `localizedDescription`. Kotlin maps it to an `AuthError`.
 */
data class BridgedAuthError(
    val domain: String,
    val code: Long,
    val message: String,
)

/** A listener registration Kotlin ends when it stops observing. */
interface BridgeRegistration {
    fun remove()
}
