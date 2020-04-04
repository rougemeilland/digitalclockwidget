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

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.RemoteViews

// TODO("ウィジェット毎に採用しているタイムゾーンの名前を簡略名でもいいから常時表示できないか？ レイアウト依存でもOK")

open class AppWidget : AppWidgetProvider() {
    // 【重要】 AppWidgetProvider のコンストラクタが開発者が通常意図しないタイミングで呼ばれることがある
    // コンストラクタにログを仕込んで実行してみると onDeleted などのタイミングでオブジェクトが作成されている
    // 非 static なプロパティは意図せず初期値に化けている可能性があるので、使用しない方がいいかもしれない

    // 【重要】 異常系にて static なプロパティも初期化されてしまうことがある
    // 例： ウィジェットをタップして表示された Configure Activity をスワイプ操作により強制終了した後、再度ウィジェットをタップした場合
    // static かどうかにかかわらず、プロパティが予期しないタイミングで初期化されることを考慮するべき

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            if (Log.isLoggable(javaClass.simpleName, Log.DEBUG)) {
                Log.d(
                    javaClass.simpleName,
                    "onUpdate: appWidgetId=$appWidgetId"
                )
            }
            val state = AppWidgetState.get(context, appWidgetId)
            val views = this.getRemoteViews(context, appWidgetManager, appWidgetId)

            // 第4パラメタを POST_DELAY_TASK にするのは onCreate での1回だけ
            this.updateAppWidget(
                context,
                appWidgetManager,
                views,
                state,
                UpdateAppWidgetFlags.POST_DELAY_TASK
            )
        }
    }

    // 【重要】 onDeleted は、「新規に作られた」 AppWidgetProvider オブジェクトに対して呼び出されている
    // そのため、 onDeleted では非 static なプロパティはすべて初期値に化けてしまっており、 onUpdate などで設定されたプロパティは onDeleted では正しく参照することができない
    override fun onDeleted(
        context: Context,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            if (Log.isLoggable(javaClass.simpleName, Log.DEBUG))
                Log.d(
                    javaClass.simpleName,
                    "onDeleted: appWidgetId=$appWidgetId"
                )
            val appWidgetState = AppWidgetState.load(context, appWidgetId)
            if (appWidgetState != null) {
                appWidgetState.delete(context)
                if (Log.isLoggable(javaClass.simpleName, Log.DEBUG))
                    Log.d(
                        javaClass.simpleName,
                        "onDeleted: Deleted appWidgetState: appWidgetId=$appWidgetId"
                    )
            }
            AppWidgetSetting.load(context, appWidgetId).also {
                it.delete(context)
            }
            if (Log.isLoggable(javaClass.simpleName, Log.DEBUG))
                Log.d(
                    javaClass.simpleName,
                    "onDeleted: Deleted appWidgetSetting: appWidgetId=$appWidgetId"
                )
        }
        super.onDeleted(context, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    override fun onAppWidgetOptionsChanged(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        if (context != null && appWidgetManager != null) {
            val appWidgetState = AppWidgetState.get(context, appWidgetId)

            // Widget のサイズを元にレイアウトを選択し、表示に反映する
            val views = getRemoteViews(context, appWidgetManager, appWidgetId)

            // 第4パラメタを POST_DELAY_TASK にするのは onCreate での1回だけ
            updateAppWidget(
                context,
                appWidgetManager,
                views,
                appWidgetState,
                UpdateAppWidgetFlags.REDRAW_FORCELY
            )
        }

        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)

        if (context == null)
            return

        when (intent?.action) {
            AppWidgetAction.REFRESH_WIDGET.actionName -> {
                if (Log.isLoggable(javaClass.simpleName, Log.DEBUG))
                    Log.d(
                        javaClass.simpleName,
                        "onReceive: action=${intent.action}"
                    )

                // ウィジェットの再表示が指示された場合
                val appWidgetManager = AppWidgetManager.getInstance(context)

                // アクションの対象の appWidgetId を取得する
                val appWidgetId = parseExterasAsAppWidgetId(intent.extras)
                if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                    if (Log.isLoggable(javaClass.simpleName, Log.DEBUG))
                        Log.d(
                            javaClass.simpleName,
                            "onReceive: Bad appWidgetId: action=${intent.action}"
                        )
                    return
                }
                val appWidgetState = AppWidgetState.load(context, appWidgetId)
                if (appWidgetState == null) {
                    if (Log.isLoggable(javaClass.simpleName, Log.DEBUG))
                        Log.d(
                            javaClass.simpleName,
                            "onReceive: appWidgetState not exists: action=${intent.action}"
                        )
                    return
                }

                val views = getRemoteViews(context, appWidgetManager, appWidgetId)

                // 第4パラメタを POST_DELAY_TASK にするのは onCreate での1回だけ
                updateAppWidget(
                    context,
                    appWidgetManager,
                    views,
                    appWidgetState,
                    UpdateAppWidgetFlags.REDRAW_FORCELY
                )
            }
            AppWidgetAction.ONCLICKED_WIDGET.actionName -> {
                // ウィジェットがクリックされた場合

                if (Log.isLoggable(javaClass.simpleName, Log.DEBUG))
                    Log.d(

                        javaClass.simpleName,
                        "onReceive: action=${intent.action}"
                    )

                // アクションの対象の appWidgetId を取得する
                val appWidgetId = parseExterasAsAppWidgetId(intent.extras)
                if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                    if (Log.isLoggable(javaClass.simpleName, Log.DEBUG))
                        Log.d(
                            javaClass.simpleName,
                            "onReceive: Bad widget id: action=${intent.action}"
                        )
                    return
                }

                onTouchWidget(context, appWidgetId)
            }
            else -> {
                /*
                if (Log.isLoggable(javaClass.simpleName, Log.DEBUG))
                    Log.d(

                        javaClass.simpleName,
                        "onReceive: Unknown action: action=${intent?.action}"
                    )
                 */
            }
        }
    }

    private fun onTouchWidget(context: Context, appWidgetId: Int) {
        when (AppWidgetGlobalSetting.load(context).appWidgetClickAction.also {
            if (Log.isLoggable(javaClass.simpleName, Log.DEBUG))
                Log.d(

                    javaClass.simpleName,
                    "onTouchWidget: appWidgetId=${appWidgetId}, appWidgetClickAction=${it.id}"
                )
        }) {
            AppWidgetClickAction.LAUNCH_GOOGLE_CLOCK_APPLICATION -> {
                launchGoogleClock(context, appWidgetId)
            }
            else -> {
                launchConfigure(context, appWidgetId)
            }
        }
    }

    private fun launchGoogleClock(context: Context, appWidgetId: Int) {
        Log.isLoggable(javaClass.simpleName + ".launchGoogleClock", Log.DEBUG)
        if (Log.isLoggable(javaClass.simpleName, Log.DEBUG))
            Log.d(

                javaClass.simpleName,
                "launchGoogleClock: appWidgetId=$appWidgetId"
            )
        knownApplicationInfos[googleClockPackageName]?.let { googleClockAppInfo ->
            try {
                val intent = Intent(googleClockAppInfo.actionName)
                googleClockAppInfo.categories.forEach { intent.addCategory(it) }
                intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
                intent.component = ComponentName(
                    googleClockAppInfo.packageName,
                    googleClockAppInfo.className
                )
                PendingIntent.getActivity(context, 0, intent, 0).send()
                if (Log.isLoggable(javaClass.simpleName, Log.DEBUG))
                    Log.d(

                        javaClass.simpleName,
                        "launchGoogleClock: Posted intent: appWidgetId=$appWidgetId"
                    )
            } catch (ex: ActivityNotFoundException) {
                if (Log.isLoggable(javaClass.simpleName, Log.ERROR))
                    Log.e(

                        javaClass.simpleName,
                        "launchGoogleClock: Not installed (or disabled) google clock application: appWidgetId=$appWidgetId",
                        ex
                    )
            }
        }
    }

    private fun launchConfigure(context: Context, appWidgetId: Int) {
        if (Log.isLoggable(javaClass.simpleName, Log.DEBUG))
            Log.d(
                javaClass.simpleName,
                "launchConfigure: Posting intent for launching configure activity: appWidgetId=$appWidgetId"
            )
        val intent = Intent(context, AppWidgetConfigureActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(AppWidgetExtrasKey.ON_CLICKED.keyName, true)

        // これを設定しないと、 AppWidgetConfigureActivity での intent.extras が null になってしまう
        intent.putExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            appWidgetId
        )

        // 第2パラメタが appWidgetId でなくても AppWidgetConfigureActivity は正常に表示される
        PendingIntent.getActivity(context, 0, intent, 0)
            .send()
    }

    // ウィジェットの単一インスタンスの表示の更新
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        views: RemoteViews,
        appWidgetState: AppWidgetState,
        flags: UpdateAppWidgetFlags
    ) {
        try {
            updateDigitalClockWidget(
                context,
                appWidgetManager,
                views,
                AppWidgetSetting.load(context, appWidgetState.appWidgetId),
                appWidgetState,
                flags
            )
            appWidgetManager.updateAppWidget(appWidgetState.appWidgetId, views)
        } catch (ex: Exception) {
            if (Log.isLoggable(javaClass.simpleName, Log.ERROR))
                Log.e(
                    javaClass.simpleName,
                    "updateAppWidget: Caused exception: appWidgetId=${appWidgetState.appWidgetId}",
                    ex
                )
        }
    }

    // ウィジェットのビューの更新と次回タスクの起動
    private fun updateDigitalClockWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        views: RemoteViews,
        appWidgetSetting: AppWidgetSetting,
        appWidgetState: AppWidgetState,
        flags: UpdateAppWidgetFlags
    ) {
        // 現在時刻の取得
        val nowDateTime =
            AppWidgetDateTime.now(appWidgetSetting.dateFormat, appWidgetSetting.timeZone)
        val currentSecond = nowDateTime.totalSecond

        // 前回実行時刻と現在時刻により判定
        val intervalMillisecond: Long
        val doUpdateAppWidget: Boolean
        when {
            appWidgetState.previousSecond <= 0 -> {
                // 前回実行時刻が未設定の場合
                // 100ms後に再試行するようインターバルタイムを設定する
                intervalMillisecond = 100
                // ビューを更新する
                doUpdateAppWidget = true
            }
            currentSecond == appWidgetState.previousSecond -> {
                // 現在時刻が前回実行時刻と比べて秒単位で変化していない場合
                // 100ms後に再試行するようインターバルタイムを設定する
                intervalMillisecond = 100
                // REDRAW_FORCELY フラグが指定されていない限り、ビューは更新しない
                doUpdateAppWidget = flags.contains(UpdateAppWidgetFlags.REDRAW_FORCELY)

            }
            else -> {
                // 現在時刻が前回実行時刻と比べて秒単位で秒数が変化している場合
                // 950ms後に再試行するようインターバルタイムを設定する
                intervalMillisecond = 950
                // ビューを更新する
                doUpdateAppWidget = true
            }
        }

        if (doUpdateAppWidget) {
            // widget のビューを更新する
            updateDigitalClockWidgetViews(
                views,
                appWidgetSetting,
                nowDateTime
            )
        }

        if (flags.contains(UpdateAppWidgetFlags.POST_DELAY_TASK)) {
            // 前記で決定したインターバルタイム後にupdateAppWidgetメソッドを再実行する
            val handler = Handler()
            val updateAppWidgetRunnable = Runnable {
                // 時間が経過した後での実行なので、 appWidgetState と rRemoteViews を再取得する
                val newAppWidgetState = AppWidgetState.load(context, appWidgetState.appWidgetId)
                val newRemoteViews =
                    getRemoteViews(context, appWidgetManager, appWidgetState.appWidgetId)
                if (newAppWidgetState != null) {
                    updateAppWidget(
                        context,
                        appWidgetManager,
                        newRemoteViews,
                        newAppWidgetState,
                        flags.except(UpdateAppWidgetFlags.REDRAW_FORCELY) // REDRAW_FORCELY は引き継がない
                    )
                }
            }
            handler.postDelayed(updateAppWidgetRunnable, intervalMillisecond)
        }

        // 前回実行時刻を現在時刻に置き換える
        appWidgetState.previousSecond = currentSecond

        // appWidgetState を保存する
        appWidgetState.save(context)
    }

    private fun updateDigitalClockWidgetViews(
        views: RemoteViews,
        appWidgetSetting: AppWidgetSetting,
        dateTime: AppWidgetDateTime
    ) {
        // 時の設定
        val formattedHour = "%02d".format(dateTime.hour)
        views.setTextViewText(R.id.HourView, formattedHour)
        views.setTextColor(R.id.HourView, appWidgetSetting.foregroundColorCode)

        // 時と分の区切りの設定
        views.setTextColor(R.id.HourMinuteDelimiterView, appWidgetSetting.foregroundColorCode)

        // 分の設定
        val formattedMinute = "%02d".format(dateTime.minute)
        views.setTextViewText(R.id.MinuteView, formattedMinute)
        views.setTextColor(R.id.MinuteView, appWidgetSetting.foregroundColorCode)

        // 分と秒の区切りの設定
        views.setTextColor(R.id.MinuteSecondDelimiterView, appWidgetSetting.foregroundColorCode)

        // 秒の設定
        val formattedSecond = "%02d".format(dateTime.second)
        views.setTextViewText(R.id.SecondView, formattedSecond)
        views.setTextColor(R.id.SecondView, appWidgetSetting.foregroundColorCode)

        // 日付の設定
        views.setTextViewText(R.id.DateView, dateTime.dateString)
        views.setTextColor(R.id.DateView, appWidgetSetting.foregroundColorCode)
    }

    private fun getRemoteViews(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ): RemoteViews {
        // Widget のサイズ情報を取得する
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)

        // 現在の Widget の現在のサイズを取得する
        val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 0
        val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) ?: 0

        // 現在の Widget が占めるセル数 (列数および行数) を求める
        val rows = getCellsForSize(minHeight)
        val columns = getCellsForSize(minWidth)

        // 行数と列数により適切なレイアウトを決定する
        return RemoteViews(context.packageName, selectLayout(columns, rows)).also { remoteViews ->
            // 作成された RemoteViews のインスタンスに対し、ウィジェットをタッチしたときに intent がブロードキャストで配信されるように登録する
            // (同じアプリ内向けなのが理由か不明だが、 manifest の intent-filter には特に何も書かなくても配信されている)
            // 【重要】 RemoteViews のインスタンスが作られたらその都度 setOnClickPendingIntent を発行しないとアクションは実行されない
            Intent(context, AppWidget::class.java).also { intent ->
                intent.action = AppWidgetAction.ONCLICKED_WIDGET.actionName
                intent.putExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    appWidgetId
                ) // putExtra を呼び出さないと、 onReceive にて intent.extra が null になる
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId,
                    intent,
                    0
                ) // 第2パラメタに appWidgetId を指定しないと onReceive が呼び出されない
                remoteViews.setOnClickPendingIntent(R.id.AppWidgetRootView, pendingIntent)
            }
        }
    }

    companion object {
        private class KnownApplicationInfo(
            val packageName: String,
            val className: String,
            val categories: Array<String>,
            val actionName: String
        )

        private const val googleClockPackageName = "com.google.android.deskclock"
        private val knownApplicationInfos =
            mutableMapOf<String, KnownApplicationInfo>().also { knownAppMap ->
                arrayOf(
                    KnownApplicationInfo(
                        this.googleClockPackageName,
                        "com.android.deskclock.DeskClock",
                        arrayOf("android.intent.category.LAUNCHER"),
                        "android.intent.action.MAIN"
                    )
                ).forEach { knownApp -> knownAppMap[knownApp.packageName] = knownApp }
            }

        // 与えられた表示領域のセル数に対し適切なレイアウトを決定する
        private fun selectLayout(columns: Int, rows: Int): Int {
            return when {
                columns >= 4 && rows >= 2 -> R.layout.app_widget4x2
                else -> R.layout.app_widget3x2
            }
        }

        // 画面上の長さからセル数を求める
        private fun getCellsForSize(size: Int): Int = (size + 30) / 70

        // extras から appWidgetId を取得する
        fun parseExterasAsAppWidgetId(extras: Bundle?): Int {
            return extras?.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
                ?: AppWidgetManager.INVALID_APPWIDGET_ID
        }
    }
}
