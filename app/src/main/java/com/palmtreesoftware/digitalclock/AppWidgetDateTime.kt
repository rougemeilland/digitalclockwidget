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
                            Log.e(
                                AppWidgetDateTime::class.java.canonicalName + ".now()",
                                "Detected bad timeZone.id: timeZone.id=" + timeZone.id
                            )
                            ZoneId.systemDefault()
                        }
                        else -> {
                            try {
                                ZoneId.of(timeZone.id)
                            } catch (ex: Exception) {
                                Log.d(
                                    AppWidgetDateTime::class.java.canonicalName + ".now()",
                                    "Detected bad timeZone.id: timeZone.id=" + timeZone.id
                                )
                                throw ex
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
                            Log.d(
                                AppWidgetDateTime::class.java.canonicalName + ".now()",
                                "Detected bad timeZone.id: timeZone.id=" + timeZone.id
                            )
                            TimeZone.getDefault()
                        }
                        else -> {
                            try {
                                TimeZone.getTimeZone(timeZone.id)
                            } catch (ex: Exception) {
                                Log.d(
                                    AppWidgetDateTime::class.java.canonicalName + ".now()",
                                    "Detected bad timeZone.id: timeZone.id=" + timeZone.id
                                )
                                throw ex
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