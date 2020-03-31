package com.palmtreesoftware.digitalclock

class UpdateAppWidgetFlags private constructor(
    private val bits: Int
) {
    companion object {
        val NONE = UpdateAppWidgetFlags(0b0000)

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