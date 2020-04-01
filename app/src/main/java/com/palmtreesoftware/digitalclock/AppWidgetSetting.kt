/*!
MIT License

Copyright (c) 2020 Palmtree Software

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

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