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
import android.os.Build
import java.time.ZoneId
import java.util.*

class AppWidgetTimeZone private constructor(
    val id: String,
    private val shortNameResourceId: Int,
    val idOnForm: String,
    val hourOnForm: Int,
    val minuteOnForm: Int
) {
    init {
        if (id == timeZoneIdOfTimeDifferenceExpression)
            throw Exception("AppWidgetTimeZone.init: Detected bad id: id=$id")
    }

    fun getTimeZoneShortName(context: Context): String {
        return when (idOnForm) {
            timeZoneIdOfSystemDefault -> {
                ""
            }
            timeZoneIdOfTimeDifferenceExpression -> {
                shortGmtFormat.format(hourOnForm - innerOffsetOfHour, minuteOnForm)
            }
            else -> {
                context.getString(shortNameResourceId)
            }
        }
    }

    companion object {
        private val timeDifferenceExpressionPattern = Regex("^GMT([+-][0-9][0-9]):([0-9][0-9])$")

        // gmtFormat の桁指定子 %+03d の '3' は、符号を含めた桁数であることに注意すること
        private const val gmtFormat: String = "GMT%+03d:%02d"
        private const val shortGmtFormat = "%+03d%02d"

        // android の NumberPicker は負数は扱えない模様
        // ※ minValue に負の数を入れてみたら、例外 (java.lang.IllegalArgumentException: minValue must be >= 0) が発生した
        // この問題を回避するため、以下の対策を行った
        // 1) NumberPicker に与える value, minValue, maxValue に innerOffsetOfHourAndMinute だけのゲタをはかせる
        // 2) 時の数値をタイムゾーンIDに変換するときはゲタを元に戻す。逆にタイムゾーンIDから時を取得するときは再びゲタをはかせる
        private const val innerOffsetOfHour: Int = 100

        const val maxTimeZoneHour: Int = 14 + innerOffsetOfHour
        const val minTimeZoneHour: Int = -12 + innerOffsetOfHour
        const val maxTimeZoneMinute: Int = 59
        const val minTimeZoneMinute: Int = 0
        const val timeZoneIdOfSystemDefault: String = "-"
        const val timeZoneIdOfTimeDifferenceExpression: String = "#"

        fun getDefault(): AppWidgetTimeZone {
            // AppWidgetTimeZone の既定値
            return AppWidgetTimeZone(
                timeZoneIdOfSystemDefault,
                R.string.time_zone_short_name_DEFAULT,
                timeZoneIdOfSystemDefault,
                innerOffsetOfHour,
                0
            )
        }

        fun parseFromTimeZoneId(timeZoneId: String): AppWidgetTimeZone {
            if (!validateTimeZoneId(timeZoneId))
                return getDefault()
            val m = timeDifferenceExpressionPattern.matchEntire(timeZoneId)
            if (m == null) {
                // 時差表現ではない場合 (システムデフォルト、または名前の表現)
                return AppWidgetTimeZone(
                    timeZoneId,
                    mapTimeZoneIdToResourceId(timeZoneId),
                    timeZoneId,
                    innerOffsetOfHour,
                    0
                )
            } else {
                val hour = try {
                    m.destructured.component1().toInt() + innerOffsetOfHour
                } catch (ex: Exception) {
                    return getDefault()
                }
                if (hour < minTimeZoneHour || hour > maxTimeZoneHour)
                    return getDefault()
                val minute = try {
                    m.destructured.component2().toInt()
                } catch (ex: Exception) {
                    return getDefault()
                }
                if (minute < minTimeZoneMinute || minute > maxTimeZoneMinute)
                    return getDefault()
                // 時差表現であり、 hour と minute が正しい値の場合
                return AppWidgetTimeZone(
                    timeZoneId,
                    R.string.time_zone_short_name_DEFAULT,
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
                    // フォームにてシステムデフォルトが選択されていた場合
                    return getDefault()
                }
                timeZoneIdOfTimeDifferenceExpression -> {
                    // フォームにて時差表現が選択されていた場合
                    if (hourOnForm < minTimeZoneHour || hourOnForm > maxTimeZoneHour)
                        return getDefault()
                    if (minuteOnForm < minTimeZoneMinute || minuteOnForm > maxTimeZoneMinute)
                        return getDefault()
                    val timeZoneId = gmtFormat.format(hourOnForm - innerOffsetOfHour, minuteOnForm)
                    if (!validateTimeZoneId(timeZoneId))
                        return getDefault()
                    return AppWidgetTimeZone(
                        timeZoneId,
                        R.string.time_zone_short_name_DEFAULT,
                        timeZoneIdOnForm,
                        hourOnForm,
                        minuteOnForm
                    )
                }
                else -> {
                    // 上記以外の場合 (タイムゾーンの名前が与えられた場合)
                    if (!validateTimeZoneId(timeZoneIdOnForm))
                        return getDefault()
                    return AppWidgetTimeZone(
                        timeZoneIdOnForm,
                        mapTimeZoneIdToResourceId(timeZoneIdOnForm),
                        timeZoneIdOnForm,
                        innerOffsetOfHour,
                        0
                    )
                }
            }
        }

        fun getHourStringOnForm(format: String, hourOnForm: Int): String =
            String.format(format, hourOnForm - innerOffsetOfHour)

        fun getMinuteStringOnForm(format: String, minuteOnForm: Int): String =
            String.format(format, minuteOnForm)

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

        private fun mapTimeZoneIdToResourceId(timeZoneId: String): Int {
            return when (timeZoneId) {
                timeZoneIdOfSystemDefault -> R.string.time_zone_short_name_DEFAULT
                "GMT" -> R.string.time_zone_short_name_GMT
                "Asia/Tokyo" -> R.string.time_zone_short_name_JST
                "America/New_York" -> R.string.time_zone_short_name_EST
                "America/Los_Angeles" -> R.string.time_zone_short_name_PST
                "Europe/Berlin" -> R.string.time_zone_short_name_CET
                "Europe/Lisbon" -> R.string.time_zone_short_name_WET
                else -> {
                    throw Exception("AppWidgetTimeZone.mapTimeZoneIdToResourceId: Cannot resolve time zone id: timeZoneId='$timeZoneId'")
                }
            }
        }
    }
}