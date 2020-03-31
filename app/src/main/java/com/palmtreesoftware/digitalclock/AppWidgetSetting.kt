package com.palmtreesoftware.digitalclock

import android.content.Context

class AppWidgetSetting(
    var appWidgetId: Int,
    var dateFormat: String,
    var foregroundColorName: String,
    var foregroundColorCode: Int
)
{
    fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
        prefs.putString(PREF_KEY_DATE_FORMAT + appWidgetId, dateFormat)
        prefs.putString(PREF_KEY_FOREGROUND_COLOR + appWidgetId, foregroundColorName)
        prefs.apply()
    }

    fun delete(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
        prefs.remove(PREF_KEY_DATE_FORMAT + appWidgetId)
        prefs.remove(PREF_KEY_FOREGROUND_COLOR + appWidgetId)
        prefs.apply()
    }

    companion object
    {
        private const val PREFS_NAME = "com.palmtreesoftware.digitalclock.AppWidgetSetting"
        private const val PREF_KEY_DATE_FORMAT = "dateformat-"
        private const val PREF_KEY_FOREGROUND_COLOR = "foreground-"

        fun initialValue(
            context: Context,
            appWidgetId: Int
        ): AppWidgetSetting = AppWidgetSetting(
            appWidgetId,
            context.getString(R.string.default_date_format),
            context.getString(R.string.default_foreground_color),
            context.getColor(R.color.appWidgetDefaultForegroundColor)
        )

        fun load(
            context: Context,
            appWidgetId: Int
        ): AppWidgetSetting {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
            val dateFormat = prefs.getString(PREF_KEY_DATE_FORMAT + appWidgetId, null)
            val foregroundColor = prefs.getString(PREF_KEY_FOREGROUND_COLOR + appWidgetId, null)
            val parsedColor = ParsedColorCode.fromString(foregroundColor ?: context.getString(R.string.default_foreground_color))
            return AppWidgetSetting(
                appWidgetId,
                dateFormat ?: context.getString(R.string.default_date_format),
                when {
                    parsedColor.success -> parsedColor.colorName
                    else -> context.getString(R.string.default_foreground_color)
                },
                when {
                    parsedColor.success -> parsedColor.colorCode
                    else -> context.getColor(R.color.appWidgetDefaultForegroundColor)
                }
            )
        }
    }
}