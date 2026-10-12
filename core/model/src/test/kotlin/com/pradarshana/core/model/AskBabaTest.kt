package com.pradarshana.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AskBabaTest {

    @Test
    fun everyNumberFrom1To720HasEnglishAndTelugu() {
        assertNull(AskBaba.answer(0))
        assertNull(AskBaba.answer(721))
        val answers = (AskBaba.MIN..AskBaba.MAX).map { AskBaba.answer(it)!! }
        assertEquals(720, answers.size)
        assertEquals(720, answers.map { it.english }.toSet().size)
        assertTrue(answers.all { it.english.isNotBlank() && it.telugu.any { ch -> ch.code > 127 } })
        assertEquals(1, answers.first().number)
        assertEquals(720, answers.last().number)
    }
}
