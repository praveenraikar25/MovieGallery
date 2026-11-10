package com.raikar.moviegallery.data.remote.ai

import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.DataResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ReplyTextTest {
    @Test
    fun `a reply comes back trimmed`() {
        assertEquals(DataResult.Success("Heat (1995)"), DataResult.Success("  Heat (1995)\n").requireReplyText())
    }

    @Test
    fun `a null reply is an Unknown failure`() {
        assertNoText(DataResult.Success<String?>(null).requireReplyText())
    }

    @Test
    fun `a blank reply is an Unknown failure`() {
        assertNoText(DataResult.Success<String?>(" \n ").requireReplyText())
    }

    @Test
    fun `a failure passes through unchanged`() {
        val failure = DataResult.Failure(AppError.Network)

        assertEquals(failure, failure.requireReplyText())
    }

    private fun assertNoText(result: DataResult<String>) {
        val error = assertIs<AppError.Unknown>(assertIs<DataResult.Failure>(result).error)
        assertEquals("The model returned no text", error.cause.message)
    }
}
