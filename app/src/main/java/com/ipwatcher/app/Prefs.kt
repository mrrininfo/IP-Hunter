package com.ipwatcher.app

import android.content.Context

object Prefs {
    private const val NAME = "ip_watcher_prefs"
    private const val KEY_PREFIX = "desired_prefix"
    private const val KEY_INTERVAL = "check_interval_sec"
    private const val KEY_TOGGLE_ON_MS = "toggle_on_ms"
    private const val KEY_RECONNECT_WAIT_MS = "reconnect_wait_ms"
    private const val KEY_RUNNING = "is_running"

    private fun prefs(context: Context) =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun getDesiredPrefix(context: Context): String =
        prefs(context).getString(KEY_PREFIX, "") ?: ""

    fun setDesiredPrefix(context: Context, value: String) {
        prefs(context).edit().putString(KEY_PREFIX, value).apply()
    }

    fun getCheckIntervalSec(context: Context): Int =
        prefs(context).getInt(KEY_INTERVAL, 5)

    fun setCheckIntervalSec(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_INTERVAL, value).apply()
    }

    // Thời gian giữ Airplane Mode ở trạng thái ON trước khi tắt lại (ms)
    fun getToggleOnMs(context: Context): Long =
        prefs(context).getLong(KEY_TOGGLE_ON_MS, 4000L)

    // Thời gian chờ mạng kết nối lại sau khi tắt Airplane Mode (ms)
    fun getReconnectWaitMs(context: Context): Long =
        prefs(context).getLong(KEY_RECONNECT_WAIT_MS, 10000L)

    fun setRunning(context: Context, running: Boolean) {
        prefs(context).edit().putBoolean(KEY_RUNNING, running).apply()
    }

    fun isRunning(context: Context): Boolean =
        prefs(context).getBoolean(KEY_RUNNING, false)
}
