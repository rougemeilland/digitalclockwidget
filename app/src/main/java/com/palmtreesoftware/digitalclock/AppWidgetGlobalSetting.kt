package com.palmtreesoftware.digitalclock

import android.content.Context

class AppWidgetGlobalSetting(var appWidgetClickAction: AppWidgetClickAction) {

    fun save(context: Context) {
        val prefs = context.getSharedPreferences(Companion.PREFS_NAME, 0).edit()
        prefs.putString(Companion.PREF_KEY_CLICK_ACTION, appWidgetClickAction.id)
        prefs.apply()
    }

    companion object {
        private const val PREFS_NAME = "com.palmtreesoftware.digitalclock.AppWidgetGlobalSetting"
        private const val PREF_KEY_CLICK_ACTION = "clickaction-"

        fun load(context: Context): AppWidgetGlobalSetting {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
            val clickAction = prefs.getString(PREF_KEY_CLICK_ACTION, null)
            val clickActionType =
                if (clickAction == null)
                    AppWidgetClickAction.ACTION_LAUNCH_CONFIGURE
                else
                    try {
                        AppWidgetClickAction.valueOf(clickAction)
                    } catch (ex: IllegalArgumentException) {
                        AppWidgetClickAction.ACTION_LAUNCH_CONFIGURE
                    }
            return AppWidgetGlobalSetting(clickActionType)
        }
    }
}