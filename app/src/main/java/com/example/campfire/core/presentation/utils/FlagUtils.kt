package com.example.campfire.core.presentation.utils

import java.util.Locale


fun getFlagEmojiForRegionCode(regionCode: String): String {
    if (regionCode.isBlank() || regionCode.length != 2) return "🏳️"
    val codePoints = regionCode.uppercase(Locale.ROOT).map { char ->
        0x1F1E6 + (char.code - 'A'.code)
    }
    return String(codePoints.toIntArray(), 0, codePoints.size)
}