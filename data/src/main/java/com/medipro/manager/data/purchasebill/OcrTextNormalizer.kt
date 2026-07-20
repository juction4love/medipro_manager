package com.medipro.manager.data.purchasebill

import java.util.Locale

object OcrTextNormalizer {
    fun normalize(text: String): String =
        text.uppercase(Locale.getDefault())
            .replace(Regex("[^A-Z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}
