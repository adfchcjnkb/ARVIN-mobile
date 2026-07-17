package com.arvin.player.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SleepTimerManagerTest {

    @Test
    fun `counts down and fires onFinish at zero`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        var finished = false

        SleepTimerManager.start(
            minutes = 1,
            scope = TestScope(dispatcher),
            dispatcher = dispatcher,
            tickMs = 1000L
        ) { finished = true }

        assertTrue(SleepTimerManager.isActive.value)
        assertEquals(60_000L, SleepTimerManager.remainingMs.value)

        advanceTimeBy(30_000L)
        testScheduler.runCurrent()
        assertTrue(SleepTimerManager.remainingMs.value in 29_000L..31_000L)
        assertFalse(finished)

        advanceTimeBy(31_000L)
        testScheduler.runCurrent()
        assertTrue(finished)
        assertFalse(SleepTimerManager.isActive.value)
    }

    @Test
    fun `cancel stops the timer before it finishes`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        var finished = false

        SleepTimerManager.start(
            minutes = 5,
            scope = TestScope(dispatcher),
            dispatcher = dispatcher,
            tickMs = 1000L
        ) { finished = true }

        advanceTimeBy(5000L)
        testScheduler.runCurrent()
        SleepTimerManager.cancel()

        advanceTimeBy(300_000L)
        testScheduler.runCurrent()

        assertFalse(finished)
        assertFalse(SleepTimerManager.isActive.value)
        assertEquals(0L, SleepTimerManager.remainingMs.value)
    }
}
