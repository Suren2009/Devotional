package com.pradarshana.app.ui

import androidx.compose.ui.test.junit4.ComposeTestRule

/**
 * Under Robolectric `waitUntil` measures its timeout on the virtual frame clock, which elapses
 * before Room's real background threads deliver results. This waits on wall-clock time instead.
 */
fun ComposeTestRule.waitUntilReal(timeoutMillis: Long = 5_000, condition: () -> Boolean) {
    val deadline = System.currentTimeMillis() + timeoutMillis
    while (true) {
        waitForIdle()
        if (condition()) return
        check(System.currentTimeMillis() < deadline) { "Condition not met within $timeoutMillis ms" }
        Thread.sleep(10)
    }
}
