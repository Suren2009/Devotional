package com.pradarshana.core.data

import java.util.concurrent.atomic.AtomicLong

class TestClock(start: Long = 1_700_000_000_000L) : Clock {
    private val time = AtomicLong(start)
    override fun now(): Long = time.get()
    fun advance(millis: Long) {
        time.addAndGet(millis)
    }
}
