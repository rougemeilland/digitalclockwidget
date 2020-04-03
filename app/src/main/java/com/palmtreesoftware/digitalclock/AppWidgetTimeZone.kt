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
            Log.e(
                javaClass.canonicalName + ".constructor()",
                "Detected bad id: id=" + id
            )
        }
    }

    companion object {
        private val numericTimeZonePattern = Regex("^GMT([+-][0-9][0-9]):([0-9][0-9])$")

        // gmtFormat の桁指定子 %+03d の '3' は、符号を含めた桁数であることに注意すること
        private val gmtFormat: String = "GMT%+03d:%02d"

        // android の NumberPicker は負数は扱えない模様
        // ※ minValue に負の数を入れてみたら、例外 (java.lang.IllegalArgumentException: minValue must be >= 0) が発生した
        // この問題を回避するため、以下の対策を行った
        // 1) NumberPicker に与える value, minValue, maxValue に innerOffsetOfHourAndMinute だけのゲタをはかせる
        // 2) 分と秒の数値をタイムゾーンIDに変換するときはゲタを元に戻す。逆にタイムゾーンIDから分と秒を取得するときは再びゲタをはかせる
        private val innerOffsetOfHourAndMinute = 100
        val maxTimeZoneHour: Int = 14 + innerOffsetOfHourAndMinute
        val minTimeZoneHour: Int = -12 + innerOffsetOfHourAndMinute
        val maxTimeZoneMinute: Int = 59 + innerOffsetOfHourAndMinute
        val minTimeZoneMinute: Int = 0 + innerOffsetOfHourAndMinute
        val timeZoneIdOfSystemDefault: String = "-"
        val timeZoneIdOfTimeDifferenceExpression: String = "*"

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
                try {
                    ZoneId.of(timeZoneId)
                    return true
                } catch (ex: Exception) {
                    return false
                }
            } else {
                // 例外が発生したら不当なタイムゾーンIDだと判断する
                try {
                    // 与えられたタイムゾーンIDが GMT ではなく、かつ、 与えられたタイムゾーンiDから作ってみた TimeZone オブジェクトの id プロパティが GMT だった場合は不当なタイムゾーンIDであると判断する
                    // ※ TimeZone.getTimeZone() が未知のタイムゾーンを全部 GMT とみなしてしまうため
                    return timeZoneId == "GMT" || TimeZone.getTimeZone(timeZoneId).id != "GMT"
                } catch (ex: Exception) {
                    return false
                }
            }
        }
    }
}