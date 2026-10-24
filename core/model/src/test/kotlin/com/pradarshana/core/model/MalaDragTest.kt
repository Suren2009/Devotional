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
}
