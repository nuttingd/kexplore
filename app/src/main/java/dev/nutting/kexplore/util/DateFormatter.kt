package dev.nutting.kexplore.util

import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeParseException

object DateFormatter {

    fun age(timestamp: String?): String {
        if (timestamp == null) return "Unknown"
        return try {
            val created = Instant.parse(timestamp)
            val duration = Duration.between(created, Instant.now())
            formatDuration(duration)
        } catch (_: DateTimeParseException) {
            "Unknown"
        }
    }

    private fun formatDuration(duration: Duration): String {
        val days = duration.toDays()
        val hours = duration.toHours() % 24
        val minutes = duration.toMinutes() % 60

        return when {
            days > 365 -> "${days / 365}y"
            days > 0 -> "${days}d"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "${duration.seconds}s"
        }
    }
}
