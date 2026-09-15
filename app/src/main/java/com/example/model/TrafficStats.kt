package com.example.model

data class TrafficStats(
    val uploadSpeedBps: Long = 0L,
    val downloadSpeedBps: Long = 0L,
    val totalUploadBytes: Long = 0L,
    val totalDownloadBytes: Long = 0L,
    val durationSeconds: Long = 0L,
    val currentIp: String = "197.200.14.82",
    val pingMs: Int = 42
) {
    fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec >= 1024 * 1024 -> String.format("%.2f MB/s", bytesPerSec / (1024.0 * 1024.0))
            bytesPerSec >= 1024 -> String.format("%.1f KB/s", bytesPerSec / 1024.0)
            else -> "$bytesPerSec B/s"
        }
    }

    fun formatBytes(totalBytes: Long): String {
        return when {
            totalBytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", totalBytes / (1024.0 * 1024.0 * 1024.0))
            totalBytes >= 1024 * 1024 -> String.format("%.2f MB", totalBytes / (1024.0 * 1024.0))
            totalBytes >= 1024 -> String.format("%.1f KB", totalBytes / 1024.0)
            else -> "$totalBytes B"
        }
    }

    fun formatDuration(): String {
        val hours = durationSeconds / 3600
        val minutes = (durationSeconds % 3600) / 60
        val seconds = durationSeconds % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}
