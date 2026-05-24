package com.example.foldercleaner.util

import java.util.Locale

object SizeFormatter {
    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) {
            return "$bytes B"
        }
        val units = arrayOf("KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var unitIndex = -1
        while (value >= 1024 && unitIndex < units.lastIndex) {
            value /= 1024.0
            unitIndex += 1
        }
        return String.format(Locale.US, "%.1f %s", value, units[unitIndex])
    }
}
