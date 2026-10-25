package com.pradarshana.core.model

import com.pradarshana.core.model.CounterRules.TargetValidation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CounterRulesTest {

    @Test
    fun `valid target is accepted`() {
        assertEquals(TargetValidation.Valid(108), CounterRules.validateTarget("108"))
        assertEquals(TargetValidation.Valid(1), CounterRules.validateTarget(" 1 "))
    }

    @Test
    fun `zero and negative targets are rejected`() {
        assertEquals(TargetValidation.NotPositive, CounterRules.validateTarget("0"))
        assertEquals(TargetValidation.NotPositive, CounterRules.validateTarget("-5"))
        assertEquals(TargetValidation.NotPositive, CounterRules.validateTarget(0L))
    }

    @Test
    fun `empty, non numeric and huge targets are rejected`() {
        assertEquals(TargetValidation.Empty, CounterRules.validateTarget(""))
        assertEquals(TargetValidation.NotANumber, CounterRules.validateTarget("abc"))
        assertEquals(TargetValidation.NotANumber, CounterRules.validateTarget("1.5"))
        assertEquals(TargetValidation.TooLarge, CounterRules.validateTarget("100001"))
        assertEquals(TargetValidation.NotANumber, CounterRules.validateTarget("99999999999999999999999"))
    }

    @Test
    fun `remaining and progress match the specification example`() {
        assertEquals(81, CounterRules.remaining(108, 27))
        assertEquals(25, CounterRules.progressPercent(108, 27))
        assertEquals(0.25f, CounterRules.progressFraction(108, 27), 0.0001f)
    }

    @Test
    fun `progress shows 100 only when the target is reached`() {
        assertEquals(99, CounterRules.progressPercent(1000, 999))
        assertEquals(100, CounterRules.progressPercent(1000, 1000))
        assertEquals(0, CounterRules.progressPercent(108, 0))
        assertEquals(0, CounterRules.remaining(108, 108))
    }

    @Test
    fun `increment is allowed only while active and below target`() {
        assertTrue(CounterRules.canIncrement(SessionStatus.ACTIVE, 108, 107))
        assertFalse(CounterRules.canIncrement(SessionStatus.ACTIVE, 108, 108))
        assertFalse(CounterRules.canIncrement(SessionStatus.PAUSED, 108, 10))
        assertFalse(CounterRules.canIncrement(SessionStatus.COMPLETED, 108, 108))
        assertFalse(CounterRules.canIncrement(SessionStatus.CANCELLED, 108, 3))
    }

    @Test
    fun `merged count is capped at the target`() {
        assertEquals(108, CounterRules.mergedCount(108, mapOf("phone" to 100, "watch" to 20)))
        assertEquals(30, CounterRules.mergedCount(108, mapOf("phone" to 10, "watch" to 20)))
    }

    @Test
    fun `undos reduce only the device that made them`() {
        val counts = mapOf("phone" to 6, "watch" to 4)
        assertEquals(8, CounterRules.mergedCount(108, counts, mapOf("phone" to 2)))
        assertEquals(4, CounterRules.mergedCount(108, counts, mapOf("phone" to 9)))
        assertEquals(54, CounterRules.mergedCount(108, mapOf("phone" to 54)))
        assertTrue(CounterRules.isHalfway(108, 54))
        assertFalse(CounterRules.isHalfway(108, 53))
        assertFalse(CounterRules.isHalfway(1, 0))
    }

    @Test
    fun `session invariants are enforced`() {
        assertThrows(IllegalArgumentException::class.java) { session(target = 0, completed = 0) }
        assertThrows(IllegalArgumentException::class.java) { session(target = 10, completed = -1) }
        assertThrows(IllegalArgumentException::class.java) { session(target = 10, completed = 11) }
        assertThrows(IllegalArgumentException::class.java) {
            session(target = 10, completed = 9, status = SessionStatus.COMPLETED)
        }
        val ok = session(target = 108, completed = 27)
        assertEquals(81, ok.remainingCount)
        assertEquals(25, ok.progressPercent)
    }

    @Test
    fun `duration formatting`() {
        assertEquals("45s", DurationFormat.format(45_000))
        assertEquals("12m 03s", DurationFormat.format(723_000))
        assertEquals("1h 05m 09s", DurationFormat.format(3_909_000))
        assertEquals("12:03", DurationFormat.formatCompact(723_000))
    }

    private fun session(target: Int, completed: Int, status: SessionStatus = SessionStatus.ACTIVE) =
        PradarshanaSession("s", target, completed, 0L, null, status, 0L)
}
