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

class AppWidgetGlobalSetting(val appWidgetClickAction: AppWidgetClickAction) {

    fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
        prefs.putString(PREF_KEY_CLICK_ACTION, appWidgetClickAction.id)
        prefs.apply()
    }

    companion object {
        private const val PREFS_NAME = "com.palmtreesoftware.digitalclock.AppWidgetGlobalSetting"
        private const val PREF_KEY_CLICK_ACTION = "clickaction"

        fun load(context: Context): AppWidgetGlobalSetting {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
            prefs.getString(PREF_KEY_CLICK_ACTION, null).let { actionText ->
                return AppWidgetGlobalSetting(if (actionText == null) {
                    AppWidgetClickAction.LAUNCH_CONFIGURE
                } else {
                    AppWidgetClickAction.values().firstOrNull { value -> value.id == actionText }
                        ?: AppWidgetClickAction.LAUNCH_CONFIGURE
                })
            }
        }
    }
}