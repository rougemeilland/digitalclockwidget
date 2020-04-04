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

class UpdateAppWidgetFlags private constructor(
    private val bits: Int
) {
    companion object {
        val POST_DELAY_TASK = UpdateAppWidgetFlags(0b0001)
        val REDRAW_FORCELY = UpdateAppWidgetFlags(0b00010)
    }

    operator fun plus(flags: UpdateAppWidgetFlags): UpdateAppWidgetFlags =
        UpdateAppWidgetFlags(this.bits or flags.bits)

    operator fun minus(flags: UpdateAppWidgetFlags): UpdateAppWidgetFlags =
        UpdateAppWidgetFlags(this.bits and flags.bits.inv())

    operator fun times(flags: UpdateAppWidgetFlags): UpdateAppWidgetFlags =
        UpdateAppWidgetFlags(this.bits and flags.bits)

    operator fun contains(flags: UpdateAppWidgetFlags): Boolean =
        this * flags == flags

    fun except(flags: UpdateAppWidgetFlags): UpdateAppWidgetFlags =
        UpdateAppWidgetFlags(this.bits and flags.bits.inv())

    override fun equals(other: Any?) = when {
        this === other -> true
        other !is UpdateAppWidgetFlags -> false
        else -> this.bits == other.bits
    }

    override fun hashCode() = bits.hashCode()

    override fun toString() =
        "${UpdateAppWidgetFlags::class.java.simpleName}(${::bits.name}=0b${bits.toString(2)})"
}