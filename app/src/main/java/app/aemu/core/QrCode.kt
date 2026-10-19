/* Added for AEmulator X, 2026-10-19: QR code of the browser play address. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

/** A QR code (ZXing core, Apache-2.0) of [text]; the bare matrix is kept separate so it can be checked off-device. */
object QrCode {
    /** true = dark module, [size]×[size], with a 2-module quiet zone */
    fun matrix(text: String, size: Int): Array<BooleanArray> {
        val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 2))
        return Array(m.height) { y -> BooleanArray(m.width) { x -> m.get(x, y) } }
    }

    fun bitmap(text: String, size: Int): Bitmap? = runCatching {
        val m = matrix(text, size)
        val h = m.size; val w = m[0].size
        val px = IntArray(w * h) { i -> if (m[i / w][i % w]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt() }
        Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
    }.getOrNull()
}
