package com.raikar.moviegallery.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CurrentUserTest {
    @Test
    fun `name is the trimmed display name`() {
        assertEquals("Jane Doe", CurrentUser("jane@example.com", "  Jane Doe ").name)
    }

    @Test
    fun `name is null when display name is null, empty or blank`() {
        assertNull(CurrentUser("jane@example.com", null).name)
        assertNull(CurrentUser("jane@example.com", "").name)
        assertNull(CurrentUser("jane@example.com", "   ").name)
    }

    @Test
    fun `initial comes from the display name, uppercased`() {
        assertEquals("J", CurrentUser("sam@example.com", "jane doe").initial)
    }

    @Test
    fun `initial comes from the email when there is no display name`() {
        assertEquals("S", CurrentUser("sam@example.com", null).initial)
        assertEquals("S", CurrentUser("sam@example.com", "  ").initial)
    }

    @Test
    fun `initial falls back when neither name nor email is available`() {
        assertEquals(CurrentUser.FALLBACK_INITIAL, CurrentUser(null, null).initial)
        assertEquals(CurrentUser.FALLBACK_INITIAL, CurrentUser("", null).initial)
    }
}
