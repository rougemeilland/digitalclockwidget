package com.palmtreesoftware.digitalclock

import android.text.InputFilter
import android.text.Spanned
import java.util.regex.Pattern

class RegexInputFilter(val pattern: Pattern) : InputFilter {
    constructor(patternText: String) : this(Pattern.compile(patternText))

    override fun filter(
        source: CharSequence?,
        start: Int,
        end: Int,
        dest: Spanned?,
        dstart: Int,
        dend: Int
    ): CharSequence {
        if (source.isNullOrEmpty())
            return ""

        val destStr = dest.toString();
        val newValue = destStr.substring(0, dstart) + source + destStr.substring(dend)
        val matcher = pattern.matcher(newValue)
        if (matcher.matches()) {
            return source
        } else {
            return ""
        }
    }
}