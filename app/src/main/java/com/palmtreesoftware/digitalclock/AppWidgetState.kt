package com.palmtreesoftware.digitalclock

import android.content.Context

class AppWidgetState(
    val appWidgetId: Int,
    var previousSecond: Long
) {
    fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
        prefs.putLong(
            PREF_KEY_PREVIOUS_SECOND + appWidgetId,
            previousSecond
        )
        prefs.apply()
    }

    fun delete(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
        prefs.remove(PREF_KEY_PREVIOUS_SECOND + appWidgetId)
        prefs.apply()
    }

    companion object {
        private const val PREFS_NAME = "com.palmtreesoftware.digitalclock.AppWidgetState"
        private const val PREF_KEY_PREVIOUS_SECOND = "previoussecond-"

        fun load(
            context: Context,
            appWidgetId: Int
        ): AppWidgetState? {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
            val previousSecond =
                prefs.getLong(PREF_KEY_PREVIOUS_SECOND + appWidgetId, 0)
            return AppWidgetState(appWidgetId, previousSecond)
        }

        fun get(
            context: Context,
            appWidgetId: Int
        ): AppWidgetState {
            return load(context, appWidgetId)
                ?: AppWidgetState(appWidgetId, 0).also {
                    it.save(context)
                }
        }
    }
}