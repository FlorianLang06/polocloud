package de.polocloud.node.services.queue

import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Backoff/circuit-breaker for groups whose services crash immediately after starting.
 *
 * Without this, a misconfigured group (bad platform args, a plugin that throws on load, a
 * port already taken) gets re-queued and restarted as fast as [de.polocloud.node.services.factory.FactoryService.start]
 * can spawn it and detect the exit — hammering disk (template copy, task execution) and CPU
 * in a tight loop instead of surfacing the problem. Only services that die young, repeatedly,
 * in a row trigger this: one that survives past [FAST_FAILURE_MILLIS] resets its group back
 * to a clean slate, so a normal crash after hours of uptime never engages backoff at all.
 */
class CrashLoopGuard {

    private val logger = LoggerFactory.getLogger(CrashLoopGuard::class.java)

    private class State {
        var consecutiveFastFailures: Int = 0
        var backoffUntil: Long = 0
    }

    private val states = ConcurrentHashMap<String, State>()

    /** Records that a group's service exited [ranForMillis] after it was started. */
    fun recordExit(groupName: String, ranForMillis: Long) {
        val state = states.getOrPut(groupName) { State() }
        synchronized(state) {
            if (ranForMillis >= FAST_FAILURE_MILLIS) {
                state.consecutiveFastFailures = 0
                state.backoffUntil = 0
                return
            }

            state.consecutiveFastFailures++
            if (state.consecutiveFastFailures < FAILURES_BEFORE_BACKOFF) return

            val shift = (state.consecutiveFastFailures - FAILURES_BEFORE_BACKOFF).coerceAtMost(6)
            val backoffMillis = (BASE_BACKOFF_MILLIS shl shift).coerceAtMost(MAX_BACKOFF_MILLIS)
            state.backoffUntil = System.currentTimeMillis() + backoffMillis
            logger.warn(
                "Group '{}' crashed {} times in a row within {}ms of starting — backing off for {}ms before placing another replica",
                groupName, state.consecutiveFastFailures, FAST_FAILURE_MILLIS, backoffMillis
            )
        }
    }

    /** Whether [groupName] is currently in a backoff window and should not be placed. */
    fun isBackingOff(groupName: String): Boolean {
        val state = states[groupName] ?: return false
        return System.currentTimeMillis() < state.backoffUntil
    }

    private companion object {
        /** A service that dies within this long of starting counts as a "fast" failure. */
        const val FAST_FAILURE_MILLIS = 30_000L

        /** How many consecutive fast failures before backoff kicks in at all. */
        const val FAILURES_BEFORE_BACKOFF = 3

        const val BASE_BACKOFF_MILLIS = 5_000L
        const val MAX_BACKOFF_MILLIS = 5 * 60_000L
    }
}