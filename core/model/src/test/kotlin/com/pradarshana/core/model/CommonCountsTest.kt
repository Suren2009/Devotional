package com.pradarshana.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CommonCountsTest {

    @Test
    fun defaultsWhenThereIsNoHistory() {
        assertEquals(listOf(3, 9, 18, 54, 108), CommonCounts.suggest(emptyList()))
    }

    @Test
    fun latestCountIsFirstThenFrequentCountsThenDefaultsWithoutDuplicates() {
        val targets = listOf(21, 108, 108, 21, 9, 7)
        assertEquals(listOf(21, 108, 3, 9, 18, 54), CommonCounts.suggest(targets))
    }

    @Test
    fun aCountUsedOnlyOnceIsNotRepeatedAmongTheDefaults() {
        assertEquals(listOf(18, 3, 9, 54, 108), CommonCounts.suggest(listOf(18)))
    }
}
