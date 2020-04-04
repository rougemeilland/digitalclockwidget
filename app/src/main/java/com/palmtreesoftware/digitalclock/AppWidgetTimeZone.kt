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
import java.time.ZoneId
import java.util.*

class AppWidgetTimeZone private constructor(
    val id: String,
    val idOnForm: String,
    val hourOnForm: Int,
    val minuteOnForm: Int
) {
    init {
        if (id == timeZoneIdOfTimeDifferenceExpression) {
            if (Log.isLoggable("AppWidgetTimeZone", Log.ERROR))
                Log.e(
                    "AppWidgetTimeZone",
                    "init: Detected bad id: id=$id"
                )
        }
    }

    companion object {
        private val numericTimeZonePattern = Regex("^GMT([+-][0-9][0-9]):([0-9][0-9])$")

        // gmtFormat の桁指定子 %+03d の '3' は、符号を含めた桁数であることに注意すること
        private const val gmtFormat: String = "GMT%+03d:%02d"

        // android の NumberPicker は負数は扱えない模様
        // ※ minValue に負の数を入れてみたら、例外 (java.lang.IllegalArgumentException: minValue must be >= 0) が発生した
        // この問題を回避するため、以下の対策を行った
        // 1) NumberPicker に与える value, minValue, maxValue に innerOffsetOfHourAndMinute だけのゲタをはかせる
        // 2) 分と秒の数値をタイムゾーンIDに変換するときはゲタを元に戻す。逆にタイムゾーンIDから分と秒を取得するときは再びゲタをはかせる
        private const val innerOffsetOfHourAndMinute = 100

        const val maxTimeZoneHour: Int = 14 + innerOffsetOfHourAndMinute
        const val minTimeZoneHour: Int = -12 + innerOffsetOfHourAndMinute
        const val maxTimeZoneMinute: Int = 59 + innerOffsetOfHourAndMinute
        const val minTimeZoneMinute: Int = 0 + innerOffsetOfHourAndMinute
        const val timeZoneIdOfSystemDefault: String = "-"
        const val timeZoneIdOfTimeDifferenceExpression: String = "*"

        fun getDefault(): AppWidgetTimeZone {
            return AppWidgetTimeZone(
                timeZoneIdOfSystemDefault,
                timeZoneIdOfSystemDefault,
                innerOffsetOfHourAndMinute,
                innerOffsetOfHourAndMinute
            )
        }

        fun parseFromTimeZoneId(timeZoneId: String): AppWidgetTimeZone {
            if (!validateTimeZoneId(timeZoneId))
                return getDefault()
            val m = numericTimeZonePattern.matchEntire(timeZoneId)
            if (m == null)
                return AppWidgetTimeZone(
                    timeZoneId,
                    timeZoneId,
                    innerOffsetOfHourAndMinute,
                    innerOffsetOfHourAndMinute
                )
            else {
                val hour = try {
                    m.destructured.component1().toInt() + innerOffsetOfHourAndMinute
                } catch (ex: Exception) {
                    return getDefault()
                }
                if (hour < minTimeZoneHour || hour > maxTimeZoneHour)
                    return getDefault()
                val minute = try {
                    m.destructured.component2().toInt() + innerOffsetOfHourAndMinute
                } catch (ex: Exception) {
                    return getDefault()
                }
                if (minute < minTimeZoneMinute || minute > maxTimeZoneMinute)
                    return getDefault()
                return AppWidgetTimeZone(
                    timeZoneId,
                    timeZoneIdOfTimeDifferenceExpression,
                    hour,
                    minute
                )
            }
        }

        fun fromFormValues(
            timeZoneIdOnForm: String,
            hourOnForm: Int,
            minuteOnForm: Int
        ): AppWidgetTimeZone {
            when (timeZoneIdOnForm) {
                timeZoneIdOfSystemDefault -> {
                    return getDefault()
                }
                timeZoneIdOfTimeDifferenceExpression -> {
                    if (hourOnForm < minTimeZoneHour || hourOnForm > maxTimeZoneHour)
                        return getDefault()
                    if (minuteOnForm < minTimeZoneMinute || minuteOnForm > maxTimeZoneMinute)
                        return getDefault()
                    val timeZoneId = gmtFormat.format(
                        hourOnForm - innerOffsetOfHourAndMinute,
                        minuteOnForm - innerOffsetOfHourAndMinute
                    )
                    if (!validateTimeZoneId(timeZoneId))
                        return getDefault()
                    return AppWidgetTimeZone(
                        timeZoneId,
                        timeZoneIdOnForm,
                        hourOnForm,
                        minuteOnForm
                    )
                }
                else -> {
                    if (!validateTimeZoneId(timeZoneIdOnForm))
                        return getDefault()
                    return AppWidgetTimeZone(
                        timeZoneIdOnForm,
                        timeZoneIdOnForm,
                        innerOffsetOfHourAndMinute,
                        innerOffsetOfHourAndMinute
                    )
                }
            }
        }

        fun getHourStringOnForm(format: String, hourOnForm: Int): String =
            String.format(format, hourOnForm - innerOffsetOfHourAndMinute)

        fun getMinuteStringOnForm(format: String, minuteOnForm: Int): String =
            String.format(format, minuteOnForm - innerOffsetOfHourAndMinute)

        private fun validateTimeZoneId(timeZoneId: String): Boolean {
            if (timeZoneId == timeZoneIdOfSystemDefault)
                return true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 例外が発生したら不当なタイムゾーンIDだと判断する
                return try {
                    ZoneId.of(timeZoneId)
                    true
                } catch (ex: Exception) {
                    false
                }
            } else {
                // 例外が発生したら不当なタイムゾーンIDだと判断する
                return try {
                    // 与えられたタイムゾーンIDが GMT ではなく、かつ、 与えられたタイムゾーンiDから作ってみた TimeZone オブジェクトの id プロパティが GMT だった場合は不当なタイムゾーンIDであると判断する
                    // ※ TimeZone.getTimeZone() が未知のタイムゾーンを全部 GMT とみなしてしまうため
                    timeZoneId == "GMT" || TimeZone.getTimeZone(timeZoneId).id != "GMT"
                } catch (ex: Exception) {
                    false
                }
            }
        }
    }
}