package com.raikar.moviegallery.data.remote.auth

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GetTokenResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FirebaseAuthTokenProviderTest {
    private val firebaseAuth = mockk<FirebaseAuth>()
    private val provider = FirebaseAuthTokenProvider(firebaseAuth)

    private fun signedInUser(token: String?): FirebaseUser {
        val result = mockk<GetTokenResult> { every { this@mockk.token } returns token }
        return mockk { every { getIdToken(any()) } returns Tasks.forResult(result) }
    }

    @Test
    fun `no signed-in user yields no token`() =
        runTest {
            every { firebaseAuth.currentUser } returns null

            assertNull(provider.idToken())
        }

    @Test
    fun `returns the signed-in user's token`() =
        runTest {
            every { firebaseAuth.currentUser } returns signedInUser("abc")

            assertEquals("abc", provider.idToken())
        }

    @Test
    fun `passes forceRefresh through to Firebase`() =
        runTest {
            val user = signedInUser("abc")
            every { firebaseAuth.currentUser } returns user

            provider.idToken(forceRefresh = false)
            provider.idToken(forceRefresh = true)

            verify(exactly = 1) { user.getIdToken(false) }
            verify(exactly = 1) { user.getIdToken(true) }
        }
}
