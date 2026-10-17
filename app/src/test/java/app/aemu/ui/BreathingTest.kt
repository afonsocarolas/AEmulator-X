/* Added for AEmulator X, 2026-10-17: Relax mode. GPL-3.0; see LICENSE. */
package app.aemu.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class BreathingTest {
    @Test fun inHoldOutCycleOfFourteenSeconds() {
        assertEquals(0, Breathing.at(0f).first); assertEquals(0f, Breathing.at(0f).second, 0.001f)
        assertEquals(1f, Breathing.at(4f).second, 0.001f)                 // fully in after 4 s
        assertEquals(1, Breathing.at(6f).first); assertEquals(1f, Breathing.at(6f).second, 0.001f)
        assertEquals(2, Breathing.at(9f).first)
        assertEquals(0f, Breathing.at(13.999f).second, 0.01f)             // fully out after 14 s
        assertEquals(Breathing.at(1f), Breathing.at(15f))                  // repeats
    }
}
