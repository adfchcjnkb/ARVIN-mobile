package com.arvin.player.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Simple countdown sleep timer; calls onFinish (pause playback) when it reaches zero.
 * The dispatcher is injectable so tests can drive it with a virtual-time TestDispatcher
 * instead of waiting on real wall-clock delays.
 */
object SleepTimerManager {
    private var job: Job? = null

    private val _remainingMs = MutableStateFlow(0L)
    val remainingMs: StateFlow<Long> = _remainingMs

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive

    fun start(
        minutes: Int,
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        tickMs: Long = 1000L,
        onFinish: () -> Unit
    ) {
        cancel()
        var remaining = minutes * 60_000L
        _remainingMs.value = remaining
        _isActive.value = true
        job = scope.launch(dispatcher) {
            while (remaining > 0) {
                delay(tickMs)
                remaining -= tickMs
                _remainingMs.value = remaining.coerceAtLeast(0)
            }
            _isActive.value = false
            onFinish()
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _isActive.value = false
        _remainingMs.value = 0
    }
}
