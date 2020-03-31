package com.palmtreesoftware.digitalclock

enum class AppWidgetAction(val actionName: String) {
    REFRESH_WIDGET("com.palmtreesoftware.digitalclock.broadcast.REFRESH_WIDGET"),
    ONCLICKED_WIDGET("com.palmtreesoftware.digitalclock.broadcast.ONCLICKED_WIDGET"),
}