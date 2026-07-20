package com.medipro.manager.data.invoice

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QrCodeGenerator @Inject constructor() {

    fun encode(content: String, sizePx: Int = 120): Bitmap? {
        if (content.isBlank()) return null
        return runCatching {
            val hints = mapOf(EncodeHintType.MARGIN to 1)
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565).apply {
                for (x in 0 until sizePx) {
                    for (y in 0 until sizePx) {
                        setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
                    }
                }
            }
        }.getOrNull()
    }
}
