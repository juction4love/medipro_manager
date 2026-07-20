package com.medipro.manager.data.ocr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import kotlin.math.max

/**
 * Improves OCR accuracy on photographed wholesale bills via contrast boost and sharpening.
 */
object BillImagePreprocessor {

    private const val MAX_EDGE = 2400

    fun preprocess(source: Bitmap): Bitmap {
        val scaled = scaleDown(source, MAX_EDGE)
        if (scaled !== source && !source.isRecycled) {
            source.recycle()
        }
        val enhanced = enhanceContrast(scaled)
        if (enhanced !== scaled && !scaled.isRecycled) {
            scaled.recycle()
        }
        return sharpen(enhanced)
    }

    private fun scaleDown(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val largest = max(width, height)
        if (largest <= maxEdge) return bitmap
        val ratio = maxEdge.toFloat() / largest
        val targetW = (width * ratio).toInt().coerceAtLeast(1)
        val targetH = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    private fun enhanceContrast(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val matrix = ColorMatrix(
            floatArrayOf(
                1.25f, 0f, 0f, 0f, -24f,
                0f, 1.25f, 0f, 0f, -24f,
                0f, 0f, 1.25f, 0f, -24f,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    private fun sharpen(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val kernel = intArrayOf(
            0, -1, 0,
            -1, 5, -1,
            0, -1, 0,
        )
        val output = pixels.copyOf()
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var r = 0
                var g = 0
                var b = 0
                var ki = 0
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val pixel = pixels[(y + ky) * width + (x + kx)]
                        val weight = kernel[ki++]
                        r += android.graphics.Color.red(pixel) * weight
                        g += android.graphics.Color.green(pixel) * weight
                        b += android.graphics.Color.blue(pixel) * weight
                    }
                }
                val alpha = android.graphics.Color.alpha(pixels[y * width + x])
                output[y * width + x] = android.graphics.Color.argb(
                    alpha,
                    r.coerceIn(0, 255),
                    g.coerceIn(0, 255),
                    b.coerceIn(0, 255),
                )
            }
        }
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(output, 0, width, 0, 0, width, height)
        if (!bitmap.isRecycled) bitmap.recycle()
        return result
    }
}
