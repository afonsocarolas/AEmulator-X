package app.aemu.core

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.InputDevice
import java.io.DataInputStream
import java.io.File

/**
 * Guest patterns stay in VibratorService; each on/off reaches the host motor.
 * Modified for AEmulator X, 2026-10-19: and the rumble motors of connected game controllers, so a tablet without a
 * motor still feels the game.
 */
class VibrationBridge(ctx: Context, paths: VmPaths, enabled: () -> Boolean, private val log: (String) -> Unit) {
    private val vibrator = ctx.getSystemService(Vibrator::class.java)
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()

    /** The phone's own motor and every connected controller's, looked up at each buzz (controllers come and go). */
    private fun motors(): List<Vibrator> {
        val pads = runCatching {
            InputDevice.getDeviceIds().toList().mapNotNull { InputDevice.getDevice(it) }
                .filter { d -> d.sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
                    d.sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK }
                .mapNotNull { d -> if (Build.VERSION.SDK_INT >= 31) d.vibratorManager.defaultVibrator else @Suppress("DEPRECATION") d.vibrator }
                .filter { it.hasVibrator() }
        }.getOrDefault(emptyList())
        return listOfNotNull(vibrator?.takeIf { it.hasVibrator() }) + pads
    }

    private val controller = VibrationController(enabled,
        { motors().isNotEmpty() },
        { duration -> motors().forEach { m -> runCatching { m.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE), attributes) } } },
        { motors().forEach { m -> runCatching { m.cancel() } } })
    private val server = UnixServer(File(paths.root, "dev/aemu_vibrator"), "vibration") { client ->
        client.soTimeout = 500
        val frame = ByteArray(VibrationProtocol.FRAME_SIZE)
        DataInputStream(client.inputStream).readFully(frame)
        client.outputStream.write(VibrationProtocol.reply(controller.request(frame)))
        client.outputStream.flush()
    }
    fun serve(): Boolean {
        controller.start()
        val ok = server.start(log)
        if (!ok) controller.stop()
        val pads = runCatching { motors().size - (if (vibrator?.hasVibrator() == true) 1 else 0) }.getOrDefault(0)
        log("vibration: bridge ${if (ok) "ready" else "unavailable"}; host motor ${runCatching { vibrator?.hasVibrator() == true }.getOrDefault(false)}" +
            if (pads > 0) ", controller motors $pads" else "")
        return ok
    }
    fun stop() { controller.stop(); server.stop() }
}
