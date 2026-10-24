package com.pradarshana.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MalaDragTest {

    @Test
    fun sideDefaultsToTheRightEdge() {
        assertEquals(MalaSide.RIGHT, MalaSide.fromStored(null))
        assertEquals(MalaSide.RIGHT, MalaSide.fromStored("nope"))
        assertEquals(MalaSide.LEFT, MalaSide.fromStored("left"))
        assertEquals(MalaSide.RIGHT, MalaSide.fromStored("right"))
    }

    @Test
    fun oneDownwardDragCountsOnceWhenItClearsTheThreshold() {
        assertTrue(MalaDrag.counts(downwardPx = 64f, thresholdPx = 64f))
        assertTrue(MalaDrag.counts(downwardPx = 400f, thresholdPx = 64f))
        assertFalse(MalaDrag.counts(downwardPx = 63.9f, thresholdPx = 64f))
        assertFalse(MalaDrag.counts(downwardPx = 0f, thresholdPx = 64f))
        assertFalse(MalaDrag.counts(downwardPx = -20f, thresholdPx = 64f))
        assertFalse(MalaDrag.counts(downwardPx = 80f, thresholdPx = 0f))
    }

    @Test
    fun targetTenFollowsTheCountMap() {
        assertFraction(0.5f, 0, 10)
        assertFraction(0.6f, 1, 10)
        assertFraction(1f, 5, 10)
        assertFraction(0f, 6, 10)
        assertFraction(0.4f, 10, 10)
    }

    @Test
    fun targetOneHundredEightPlacesTenFiftyEightAndOneHundred() {
        assertFraction(0.5f + 10f / 54f * 0.5f, 10, 108)
        assertFraction(3f / 54f * 0.5f, 58, 108)
        assertFraction(45f / 54f * 0.5f, 100, 108)
        assertFraction(53f / 54f * 0.5f, 108, 108)
        assertTrue(MalaDrag.beadFraction(10, 108) > 0.5f)
        assertTrue(MalaDrag.beadFraction(58, 108) < 0.1f)
        assertTrue(MalaDrag.beadFraction(100, 108) < 0.5f)
        assertTrue(MalaDrag.beadFraction(100, 108) > 0.35f)
    }

    @Test
    fun visitedBeadsFollowTheSamePath() {
        assertFalse(MalaDrag.beadVisited(0.2f, 0, 108))
        assertTrue(MalaDrag.beadVisited(0.55f, 10, 108))
        assertFalse(MalaDrag.beadVisited(0.2f, 10, 108))
        assertTrue(MalaDrag.beadVisited(0.9f, 58, 108))
        assertFalse(MalaDrag.beadVisited(0.3f, 58, 108))
        assertTrue(MalaDrag.beadVisited(0.2f, 100, 108))
        assertFalse(MalaDrag.beadVisited(0.46f, 100, 108))
    }

    private fun assertFraction(expected: Float, count: Int, target: Int) {
        assertEquals(expected, MalaDrag.beadFraction(count, target), 0.001f)
    }
}
