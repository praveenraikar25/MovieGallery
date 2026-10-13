package com.raikar.moviegallery.ui.screens.login

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailValidatorTest {
    @Test
    fun `accepts ordinary addresses`() {
        assertTrue(isValidEmail("alex@example.com"))
        assertTrue(isValidEmail("first.last+tag@mail.example.co.uk"))
    }

    @Test
    fun `rejects malformed addresses`() {
        assertFalse(isValidEmail("not-an-email"))
        assertFalse(isValidEmail("a@b"))
        assertFalse(isValidEmail("@example.com"))
        assertFalse(isValidEmail("a b@example.com"))
        assertFalse(isValidEmail("a@@example.com"))
    }

    @Test
    fun `rejects the old placeholder credential`() {
        assertFalse(isValidEmail("admin"))
    }
}
