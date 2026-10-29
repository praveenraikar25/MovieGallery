package com.raikar.moviegallery.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.raikar.moviegallery.data.remote.auth.safeAuthCall
import com.raikar.moviegallery.domain.model.AuthResult
import com.raikar.moviegallery.domain.model.CurrentUser
import com.raikar.moviegallery.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl
    @Inject
    constructor(
        private val firebaseAuth: FirebaseAuth,
    ) : AuthRepository {
        // Firebase restores the persisted user from local storage, so this is valid at cold start.
        override val isSignedIn: Boolean
            get() = firebaseAuth.currentUser != null

        // The listener fires once on registration with the current user, then on every change.
        override val currentUserId: Flow<String?> =
            callbackFlow {
                val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser?.uid) }
                firebaseAuth.addAuthStateListener(listener)
                awaitClose { firebaseAuth.removeAuthStateListener(listener) }
            }.distinctUntilChanged()

        override val currentUser: Flow<CurrentUser?> =
            callbackFlow {
                val listener =
                    FirebaseAuth.AuthStateListener { auth ->
                        trySend(auth.currentUser?.let { CurrentUser(email = it.email, displayName = it.displayName) })
                    }
                firebaseAuth.addAuthStateListener(listener)
                awaitClose { firebaseAuth.removeAuthStateListener(listener) }
            }.distinctUntilChanged()

        override suspend fun signIn(
            email: String,
            password: String,
        ): AuthResult<Unit> =
            safeAuthCall {
                firebaseAuth.signInWithEmailAndPassword(email, password).await()
                Unit
            }

        override suspend fun signUp(
            email: String,
            password: String,
        ): AuthResult<Unit> =
            safeAuthCall {
                firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                Unit
            }

        override suspend fun signInWithGoogle(idToken: String): AuthResult<Unit> =
            safeAuthCall {
                firebaseAuth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
                Unit
            }

        override fun signOut() = firebaseAuth.signOut()
    }
