/* Added for AEmulator X, 2026-10-23: firmware badges. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

/** Little tags on a library tile that tell you something at a glance. */
object FirmwareTags {
    enum class Tag(val emoji: String) { NEW("✨"), NEVER_BOOTED("💤"), TOP("🥇"), BIT64("💪"), FAST("⚡"), CLASSIC("🏛️"), CONTAINER("🧩") }

    const val NEW_DAYS = 3
    const val FAST_BOOT_MS = 30_000L
    /** at least this much play before a firmware can be "most played" */
    const val TOP_MIN_MS = 10 * 60_000L

    /**
     * Tags for one firmware, most useful first, at most [max]:
     * NEW (added in the last few days), NEVER_BOOTED, TOP (most played of the library), BIT64, FAST (boots in under
     * half a minute), CLASSIC (Android 2.3 or older), CONTAINER (made from another firmware).
     */
    fun of(createdAt: Long, bootCount: Int, lastBootMs: Long, abi: String, api: Int, container: Boolean, isTop: Boolean,
           now: Long = System.currentTimeMillis(), max: Int = 3): List<Tag> {
        val tags = ArrayList<Tag>()
        val booted = bootCount > 0 || lastBootMs > 0
        if (createdAt > 0 && now - createdAt in 0..NEW_DAYS * 86_400_000L) tags += Tag.NEW
        if (!booted) tags += Tag.NEVER_BOOTED
        if (isTop) tags += Tag.TOP
        if (abi.startsWith("arm64")) tags += Tag.BIT64
        if (lastBootMs in 1 until FAST_BOOT_MS) tags += Tag.FAST
        if (api in 1..10) tags += Tag.CLASSIC
        if (container) tags += Tag.CONTAINER
        return tags.take(max)
    }

    fun of(img: GuestImage, isTop: Boolean, now: Long = System.currentTimeMillis(), max: Int = 3) =
        of(img.createdAt, img.bootCount, img.lastBootMs, img.abi, img.api, img.baseId.isNotEmpty(), isTop, now, max)

    /** The most played firmware id, when it has been played enough to count. */
    fun topPlayed(playMs: Map<String, Long>): String? = playMs.filterValues { it >= TOP_MIN_MS }.maxByOrNull { it.value }?.key
}
