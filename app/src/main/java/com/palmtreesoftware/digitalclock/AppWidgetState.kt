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