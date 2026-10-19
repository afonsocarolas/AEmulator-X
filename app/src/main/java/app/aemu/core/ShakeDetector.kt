/* Added for AEmulator X, 2026-10-19: shake the phone for a screenshot. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import kotlin.math.sqrt

/**
 * A deliberate shake: two jolts above [threshold] g within [windowMs], then nothing for [cooldownMs]. Tilting, walking
 * or a single bump never count. Feed it accelerometer samples (m/s²); [sample] returns true once per shake.
 */
class ShakeDetector(
    private val threshold: Float = 2.5f,
    private val windowMs: Long = 600,
    private val cooldownMs: Long = 2000,
) {
    private var firstJoltAt = 0L
    private var lastShakeAt = -1L

    fun sample(x: Float, y: Float, z: Float, nowMs: Long): Boolean {
        val g = sqrt(x * x + y * y + z * z) / 9.81f
        if (g < threshold) return false
        if (lastShakeAt >= 0 && nowMs - lastShakeAt < cooldownMs) return false
        if (firstJoltAt == 0L || nowMs - firstJoltAt > windowMs) { firstJoltAt = nowMs; return false }
        if (nowMs - firstJoltAt < 80) return false   // the same jolt, still above the threshold
        firstJoltAt = 0L
        lastShakeAt = nowMs
        return true
    }
}
