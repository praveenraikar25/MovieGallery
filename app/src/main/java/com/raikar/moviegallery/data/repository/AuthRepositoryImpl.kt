package com.raikar.moviegallery.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.raikar.moviegallery.data.remote.auth.safeAuthCall
import com.raikar.moviegallery.domain.model.AuthResult
import com.raikar.moviegallery.domain.repository.AuthRepository
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
