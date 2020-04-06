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

import android.os.Build
import android.util.Log
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*


class AppWidgetDateTime private constructor(
    dateString: String,
    hour: Int,
    minute: Int,
    second: Int,
    totalSecond: Long
) {

    var dateString: String = dateString
        private set


    var hour: Int = hour
        private set


    var minute: Int = minute
        private set


    var second: Int = second
        private set


    var totalSecond: Long = totalSecond
        private set

    companion object {
        fun now(dateFormatString: String, timeZone: AppWidgetTimeZone): AppWidgetDateTime {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return LocalDateTime.now(
                    when (timeZone.id) {
                        AppWidgetTimeZone.timeZoneIdOfSystemDefault -> {
                            ZoneId.systemDefault()
                        }
                        AppWidgetTimeZone.timeZoneIdOfTimeDifferenceExpression -> {
                            throw Exception("AppWidgetDateTime.now: Detected bad timeZone.id: timeZone.id=${timeZone.id}")
                        }
                        else -> {
                            try {
                                ZoneId.of(timeZone.id)
                            } catch (ex: Exception) {
                                if (Log.isLoggable("AppWidgetDateTime", Log.ERROR))
                                    Log.e(
                                        "AppWidgetDateTime",
                                        "now: Detected exeption at 'ZoneId.of(timeZone.id)', bat used system default time zone: timeZone.id=${timeZone.id}",
                                        ex
                                    )
                                ZoneId.systemDefault()
                            }
                        }
                    }
                ).let {
                    AppWidgetDateTime(
                        it.format(DateTimeFormatter.ofPattern(dateFormatString)),
                        it.hour,
                        it.minute,
                        it.second,
                        it.toEpochSecond(ZoneOffset.UTC)
                    )
                }
            } else {
                return Calendar.getInstance(
                    when (timeZone.id) {
                        AppWidgetTimeZone.timeZoneIdOfSystemDefault -> {
                            TimeZone.getDefault()
                        }
                        AppWidgetTimeZone.timeZoneIdOfTimeDifferenceExpression -> {
                            throw Exception("AppWidgetDateTime.now: Detected bad timeZone.id: timeZone.id=${timeZone.id}")
                        }
                        else -> {
                            try {
                                TimeZone.getTimeZone(timeZone.id)
                            } catch (ex: Exception) {
                                if (Log.isLoggable("AppWidgetDateTime", Log.ERROR))
                                    Log.e(
                                        "AppWidgetDateTime",
                                        "now: Detected exeption at 'TimeZone.getTimeZone(timeZone.id)', bat used system default time zone: timeZone.id=${timeZone.id}",
                                        ex
                                    )
                                TimeZone.getDefault()
                            }
                        }
                    }
                ).let {
                    AppWidgetDateTime(
                        android.text.format.DateFormat.format(dateFormatString, it).toString(),
                        it.get(Calendar.HOUR_OF_DAY),
                        it.get(Calendar.MINUTE),
                        it.get(Calendar.SECOND),
                        it.timeInMillis / 1000
                    )
                }
            }
        }
    }
}