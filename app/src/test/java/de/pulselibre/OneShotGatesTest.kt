package de.pulselibre

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * Safety gates that decide whether a one-shot device test may run (audit #6).
 * The rule everywhere: fail closed, and consume (persist) before any device action.
 */
class OneShotGatesTest {
    /** Minimal map-backed SharedPreferences: only the calls the gates use. */
    private fun prefs(initial: Map<String, Any> = emptyMap(), commitResult: Boolean = true): SharedPreferences {
        val data = HashMap(initial)
        val editor = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(SharedPreferences.Editor::class.java)) { p, m, a ->
            when (m.name) {
                "putBoolean", "putInt" -> { data[a[0] as String] = a[1]!!; p }
                "commit" -> { commitResult }
                else -> error("unexpected Editor.${m.name}")
            }
        } as SharedPreferences.Editor
        return Proxy.newProxyInstance(javaClass.classLoader, arrayOf(SharedPreferences::class.java)) { _, m, a ->
            when (m.name) {
                "getBoolean" -> data[a[0] as String] ?: a[1]
                "getInt" -> data[a[0] as String] ?: a[1]
                "edit" -> editor
                else -> error("unexpected SharedPreferences.${m.name}")
            }
        } as SharedPreferences
    }

    private fun lambdaGate(gate: (Boolean, Boolean, () -> Boolean, () -> Boolean) -> Boolean) {
        var persisted = 0
        val persist = { persisted++; true }
        assertTrue("released+confirmed+unused must pass", gate(true, true, { false }, persist))
        assertEquals(1, persisted)
        assertFalse("not released", gate(false, true, { false }, persist))
        assertFalse("not confirmed by user", gate(true, false, { false }, persist))
        assertFalse("already consumed", gate(true, true, { true }, persist))
        assertEquals("rejected calls must not persist anything", 1, persisted)
        assertFalse("persist failure means no run", gate(true, true, { false }, { false }))
    }

    @Test fun directoryAfterChainGateFailsClosed() = lambdaGate(CirqaDirectoryAfterChainGate::consume)
    @Test fun directoryReadV2GateFailsClosed() = lambdaGate(CirqaDirectoryReadV2AttemptGate::consume)
    @Test fun historicalSyncMultiPhaseGateFailsClosed() = lambdaGate(CirqaHistoricalSyncV1MultiPhaseAttemptGate::consume)

    @Test fun multiPhaseKeysAreDistinctPerPhase() {
        val keys = (1..3).map(CirqaHistoricalSyncV1MultiPhaseAttemptGate::keyForPhase)
        assertEquals(3, keys.toSet().size)
    }

    @Test fun fileAccessGateAllowsExactlyOneAttempt() {
        val p = prefs()
        assertTrue(CirqaFileAccessAttemptGate.available(p))
        assertTrue(CirqaFileAccessAttemptGate.consume(p))
        assertFalse(CirqaFileAccessAttemptGate.available(p))
        assertFalse(CirqaFileAccessAttemptGate.consume(p))
    }

    @Test fun fileAccessGateReportsFailedPersist() {
        assertFalse(CirqaFileAccessAttemptGate.consume(prefs(commitResult = false)))
    }

    @Test fun itemListGateIsBoundedAndClampsTamperedCounter() {
        assertEquals(40, CirqaItemListRepeatabilityAttemptGate.MAX_ATTEMPTS)
        val p = prefs()
        repeat(CirqaItemListRepeatabilityAttemptGate.MAX_ATTEMPTS) { assertTrue(CirqaItemListRepeatabilityAttemptGate.consume(p)) }
        assertFalse(CirqaItemListRepeatabilityAttemptGate.hasAttemptsRemaining(p))
        assertFalse(CirqaItemListRepeatabilityAttemptGate.consume(p))
        val tampered = prefs(mapOf(CirqaItemListRepeatabilityAttemptGate.ATTEMPT_COUNT_KEY to 9999))
        assertEquals(CirqaItemListRepeatabilityAttemptGate.MAX_ATTEMPTS, CirqaItemListRepeatabilityAttemptGate.attemptsUsed(tampered))
        val negative = prefs(mapOf(CirqaItemListRepeatabilityAttemptGate.ATTEMPT_COUNT_KEY to -5))
        assertEquals(0, CirqaItemListRepeatabilityAttemptGate.attemptsUsed(negative))
    }

    @Test fun raisingLimitPreservesPreviouslyConsumedAttempts() {
        val stored = prefs(mapOf(
            CirqaItemListRepeatabilityAttemptGate.ATTEMPT_COUNT_KEY to 15
        ))
        assertEquals(15, CirqaItemListRepeatabilityAttemptGate.attemptsUsed(stored))
        assertTrue(CirqaItemListRepeatabilityAttemptGate.hasAttemptsRemaining(stored))
    }
}
