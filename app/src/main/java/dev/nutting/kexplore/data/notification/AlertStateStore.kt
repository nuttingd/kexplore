package dev.nutting.kexplore.data.notification

import android.content.Context
import android.content.SharedPreferences

class AlertStateStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasBeenNotified(connectionId: String, alertId: String): Boolean =
        prefs.contains(key(connectionId, alertId))

    fun markNotified(connectionId: String, alertId: String) {
        prefs.edit().putLong(key(connectionId, alertId), System.currentTimeMillis()).apply()
    }

    fun clearOlderThan(maxAgeMillis: Long) {
        val cutoff = System.currentTimeMillis() - maxAgeMillis
        val editor = prefs.edit()
        prefs.all.forEach { (key, value) ->
            if (key.startsWith(KEY_PREFIX) && value is Long && value < cutoff) {
                editor.remove(key)
            }
        }
        editor.apply()
    }

    private fun key(connectionId: String, alertId: String): String =
        "$KEY_PREFIX$connectionId:$alertId"

    companion object {
        private const val PREFS_NAME = "kexplore_alert_state"
        private const val KEY_PREFIX = "alerted:"
        const val COOLDOWN_MILLIS = 3_600_000L // 1 hour
    }
}
