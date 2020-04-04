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

import android.text.InputFilter
import android.text.Spanned
import java.util.regex.Pattern

class RegexInputFilter(private val pattern: Pattern) : InputFilter {
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

        val destStr = dest.toString()
        val newValue = destStr.substring(0, dstart) + source + destStr.substring(dend)
        val matcher = pattern.matcher(newValue)
        return if (matcher.matches()) {
            source
        } else {
            ""
        }
    }
}