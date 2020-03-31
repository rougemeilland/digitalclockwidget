package com.palmtreesoftware.digitalclock

import android.graphics.Color

class ParsedColorCode(val success: Boolean, val colorName: String, val colorCode: Int) {
    companion object {
        fun fromString(colorExpression: String): ParsedColorCode =
            try {
                val colorCode = Color.parseColor(colorExpression)
                ParsedColorCode(true, colorExpression, colorCode)
            } catch (ex: IllegalArgumentException) {
                ParsedColorCode(false, "", 0)
            } catch (ex: Exception) {
                ParsedColorCode(false, "", 0)
            }
    }
}

