package com.medipro.manager.data.document.receipt

import com.medipro.manager.domain.model.PrinterSettings
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EscPosEncoder @Inject constructor() {

    fun encode(content: ReceiptContent, settings: PrinterSettings): ByteArray {
        val builder = EscPosBuilder()
            .initialize()
            .alignCenter()

        content.lines.forEach { line ->
            when (line.style) {
                ReceiptLineStyle.BOLD -> builder.boldOn()
                ReceiptLineStyle.DIVIDER -> builder.boldOff()
                ReceiptLineStyle.NORMAL -> builder.boldOff()
            }
            builder.textLine(line.text)
            if (line.style == ReceiptLineStyle.BOLD) builder.boldOff()
        }

        content.qrPayload?.let { payload ->
            builder.feed(1)
            builder.qrCode(payload, moduleSize = if (settings.paperWidthMm >= 80) 6 else 4)
        }

        if (settings.autoCut) {
            builder.feed(3).cut()
        } else {
            builder.feed(5)
        }

        return builder.build()
    }

    fun cashDrawerPulse(): ByteArray = byteArrayOf(0x1B, 0x70, 0x00, 0x19.toByte(), 0xFA.toByte())
}

private class EscPosBuilder {
    private val buffer = ArrayList<Byte>()

    fun initialize() = apply {
        buffer.addAll(listOf(0x1B, 0x40)) // ESC @
    }

    fun alignCenter() = apply {
        buffer.addAll(listOf(0x1B, 0x61, 0x01))
    }

    fun alignLeft() = apply {
        buffer.addAll(listOf(0x1B, 0x61, 0x00))
    }

    fun boldOn() = apply {
        buffer.addAll(listOf(0x1B, 0x45, 0x01))
    }

    fun boldOff() = apply {
        buffer.addAll(listOf(0x1B, 0x45, 0x00))
    }

    fun textLine(text: String) = apply {
        buffer.addAll(text.toByteArray(Charset.forName("ISO-8859-1")).toList())
        buffer.add(0x0A)
    }

    fun feed(lines: Int) = apply {
        repeat(lines) { buffer.add(0x0A) }
    }

    fun cut() = apply {
        buffer.addAll(listOf(0x1D, 0x56, 0x00))
    }

    fun qrCode(data: String, moduleSize: Int = 4) = apply {
        val bytes = data.toByteArray(Charsets.UTF_8)
        // Store QR code data (model 2)
        val storeLen = bytes.size + 3
        buffer.add(0x1D)
        buffer.add(0x28)
        buffer.add(0x6B)
        buffer.add((storeLen % 256).toByte())
        buffer.add((storeLen / 256).toByte())
        buffer.add(0x31)
        buffer.add(0x50)
        buffer.add(0x30)
        bytes.forEach { buffer.add(it) }

        // Set module size
        buffer.addAll(listOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, moduleSize.toByte()))

        // Print QR
        buffer.addAll(listOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30))
    }

    fun build(): ByteArray = buffer.toByteArray()
}
