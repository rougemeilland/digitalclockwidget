package com.palmtreesoftware.digitalclock

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.RemoteViews
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// TODO("リポジトリにコミットする")
// TODO("AppWidgetGlobalSetting を設定するアクティビティを作る")
// TODO("widget のタップ時にgoogle時計が起動できるようにする")
// TODO("minSdkVersion を下げる。サポートライブラリが使えるか調べる")
// TODO("参考：https://teratail.com/questions/94099")
// TODO("参考：https://developer.android.com/studio/publish/versioning?hl=ja")

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
            Log.d(
                javaClass.canonicalName + ".onUpdate()",
                "Start: appWidgetId=" + appWidgetId
            )
            val state = AppWidgetState.get(context, appWidgetId)
            val views = getRemoteViews(context, appWidgetManager, appWidgetId)

            // 第4パラメタを POST_DELAY_TASK にするのは onCreate での1回だけ
            updateAppWidget(context, appWidgetManager, views, state, UpdateAppWidgetFlags.POST_DELAY_TASK)
        }
    }

    // 【重要】 onDeleted は、「新規に作られた」 AppWidgetProvider オブジェクトに対して呼び出されている
    // そのため、 onDeleted では非 static なプロパティはすべて初期値に化けてしまっており、 onUpdate などで設定されたプロパティは onDeleted では正しく参照することができない
    override fun onDeleted(
        context: Context,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            Log.d(
                javaClass.canonicalName + ".onDeleted()",
                "Start: appWidgetId=" + appWidgetId
            )
            val appWidgetState = AppWidgetState.load(context, appWidgetId)
            if (appWidgetState != null) {
                appWidgetState.delete(context)
                Log.d(
                    javaClass.canonicalName + ".onDeleted()",
                    "appWidgetState deleted: appWidgetId=" + appWidgetId
                )
            }
            AppWidgetSetting.load(context, appWidgetId).also {
                it.delete(context)
            }
            Log.d(
                javaClass.canonicalName + ".onDeleted()",
                "appWidgetSetting deleted: appWidgetId=" + appWidgetId
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
            updateAppWidget(context, appWidgetManager, views, appWidgetState, UpdateAppWidgetFlags.REDRAW_FORCELY)
        }

        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)

        if (context == null)
            return

        when (intent?.action) {
            AppWidgetAction.REFRESH_WIDGET.actionName -> {
                Log.d(
                    javaClass.canonicalName + ".onReceive()",
                    "Received REFRESH_WIDGET"
                )

                // ウィジェットの再表示が指示された場合
                val appWidgetManager = AppWidgetManager.getInstance(context)

                // アクションの対象の appWidgetId を取得する
                val appWidgetId = AppWidget.parseExterasAsAppWidgetId(intent.extras)
                if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                    Log.d(javaClass.canonicalName + ".onReceive()", "Bad appWidgetId")
                    return
                }
                val appWidgetState = AppWidgetState.load(context, appWidgetId)
                if (appWidgetState == null) {
                    Log.d(
                        javaClass.canonicalName + ".onReceive()",
                        "appWidgetState not exists: appWidgetId=" + appWidgetId
                    )
                    return
                }
                Log.d(
                    javaClass.canonicalName + ".onReceive()",
                    "Update updateAppWidget: appWidgetId=" + appWidgetState.appWidgetId
                )
                val views = getRemoteViews(context, appWidgetManager, appWidgetId)

                // 第4パラメタを POST_DELAY_TASK にするのは onCreate での1回だけ
                updateAppWidget(context, appWidgetManager, views, appWidgetState, UpdateAppWidgetFlags.REDRAW_FORCELY)
            }
            AppWidgetAction.ONCLICKED_WIDGET.actionName -> {
                // ウィジェットがクリックされた場合

                Log.d(
                    javaClass.canonicalName + ".onReceive()",
                    "Received ONCLICKED_WIDGET"
                )

                // アクションの対象の appWidgetId を取得する
                val appWidgetId = parseExterasAsAppWidgetId(intent.extras)
                if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                    Log.d(javaClass.canonicalName + ".onReceive()", "Bad appWidgetId")
                    return
                }
                Log.d(
                    javaClass.canonicalName + ".onReceive()",
                    "Received ONCLICKED_WIDGET: appWidgetId=" + appWidgetId
                )
                onTouchWidget(context, appWidgetId)
            }
            else -> {
                Log.d(
                    javaClass.canonicalName + ".onReceive()",
                    "Received Unknown action:" + intent?.action
                )
            }
        }
    }

    private fun onTouchWidget(context: Context, appWidgetId: Int)
    {
        val globalSetting = AppWidgetGlobalSetting.load(context)
        when (globalSetting.appWidgetClickAction)
        {
            AppWidgetClickAction.ACTION_LAUNCH_GOOGLE_CLOCK_APPLICATION-> {}
            else -> launchConfigure(context, appWidgetId)
        }
    }

    private fun launchConfigure(context: Context, appWidgetId: Int) {
        Log.d(
            javaClass.canonicalName + ".launchConfigure()",
            "Posting intent for launching configure activity: appWidgetId=" + appWidgetId
        )
        val intent = Intent(context, AppWidgetConfigureActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(AppWidgetExtrasKey.ON_CLICKED.keyName, true)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId) // これを設定しないと、 AppWidgetConfigureActivity での intent.extras が null になってしまう
        PendingIntent.getActivity(context, 0, intent, 0).send() // 第2パラメタが appWidgetId でなくても AppWidgetConfigureActivity は正常に表示される
    }


/*
    private fun setOnClickAction(
        context: Context,
        views: RemoteViews,
        appWidgetId: Int
    ) {
        val globalSetting = loadGlobalSetting(context)
        val pendingIntent: PendingIntent
        if (globalSetting.appWidgetClickActionType == AppWidgetClickActionType.ACTION_LAUNCH_CLOCK_APPLICATION1) {
            pendingIntent =
                try {
                    Log.d("ActivityLauncher", "AppWidgetProviderBase.setOnClickAction.1")
                    val intent = Intent("android.intent.action.MAIN");
                    //intent.addCategory("android.intent.category.DEFAULT");
                    intent.addCategory("android.intent.category.LAUNCHER");
                    intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
                    intent.setComponent(
                        ComponentName(
                            "com.google.android.deskclock",
                            "com.android.deskclock.DeskClock"
                        )
                    )
                    PendingIntent.getActivity(context, 0, intent, 0)
                } catch (ex: ActivityNotFoundException) {
                    Log.d("ActivityLauncher", "AppWidgetProviderBase.setOnClickAction.2")
                    setOnClickActionToConfigure(context, appWidgetId)
                }
        } else if (globalSetting.appWidgetClickActionType == AppWidgetClickActionType.ACTION_LAUNCH_CLOCK_APPLICATION2) {
            pendingIntent =
                try {
                    Log.d("ActivityLauncher", "AppWidgetProviderBase.setOnClickAction.3")
                    val intent = Intent("android.intent.action.MAIN");
                    intent.addCategory("android.intent.category.LAUNCHER");
                    intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
                    intent.setComponent(
                        ComponentName(
                            "com.palmtreesoftware.digitalclock",
                            "fully qualified name of main activity of the app"
                        )
                    )
                    PendingIntent.getActivity(context, 0, intent, 0)
                } catch (ex: ActivityNotFoundException) {
                    Log.d("ActivityLauncher", "AppWidgetProviderBase.setOnClickAction.4")
                    setOnClickActionToConfigure(context, appWidgetId)
                }
        } else {
            pendingIntent = setOnClickActionToConfigure(context, appWidgetId)
        }
        views.setOnClickPendingIntent(R.id.AppWidgetRootView, pendingIntent);
    }

    private fun setOnClickActionToConfigure(
        context: Context,
        appWidgetId: Int
    ): PendingIntent {
        //TODO("appWidgetId が AppWidgetConfigureActivity に正しく渡っていない extras が null になっている")
        val intent = getAppWidgetConfigureActivityIntent(context)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        return PendingIntent.getActivity(context, appWidgetId, intent, 0)
    }
*/

    // ウィジェットの単一インスタンスの表示の更新
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        views: RemoteViews,
        appWidgetState: AppWidgetState,
        flags: UpdateAppWidgetFlags
    ) {
        try {
            //setOnClickAction(context, views, appWidgetState.appWidgetId)
            updateDigitalClockWidget(context, appWidgetManager, views, appWidgetState, flags)
            appWidgetManager.updateAppWidget(appWidgetState.appWidgetId, views)
        } catch (ex: Exception) {
            Log.e(javaClass.canonicalName + ".updateAppWidget()", "Caused exception: message=" + ex.message + ", appWidgetId=" + appWidgetState.appWidgetId)
        }

    }

    // ウィジェットのビューの更新と次回タスクの起動
    private fun updateDigitalClockWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        views: RemoteViews,
        appWidgetState: AppWidgetState,
        flags: UpdateAppWidgetFlags
    ) {
        // 現在時刻の取得
        val nowDateTime = LocalDateTime.now();
        val currentSecond = nowDateTime.toEpochSecond(ZoneOffset.UTC)

        // 前回実行時刻と現在時刻により判定
        val intervalMillisecond: Long;
        val doUpdateAppWidget: Boolean
        if (appWidgetState.previousSecond <= 0) {
            // 前回実行時刻が未設定の場合
            // 100ms後に再試行するようインターバルタイムを設定する
            intervalMillisecond = 100;
            // ビューを更新する
            doUpdateAppWidget = true
        } else if (currentSecond == appWidgetState.previousSecond) {
            // 現在時刻が前回実行時刻と比べて秒単位で変化していない場合
            // 100ms後に再試行するようインターバルタイムを設定する
            intervalMillisecond = 100;
            // REDRAW_FORCELY フラグが指定されていない限り、ビューは更新しない
            if (flags.contains(UpdateAppWidgetFlags.REDRAW_FORCELY))
                doUpdateAppWidget = true
            else
                doUpdateAppWidget = false

        } else {
            // 現在時刻が前回実行時刻と比べて秒単位で秒数が変化している場合
            // 950ms後に再試行するようインターバルタイムを設定する
            intervalMillisecond = 950;
            // ビューを更新する
            doUpdateAppWidget = true
        }
        if (doUpdateAppWidget) {
            updateDigitalClockWidgetViews(
                context,
                appWidgetState.appWidgetId,
                views,
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
                /*
                Log.d(
                    javaClass.canonicalName + ".updateAppWidget()",
                    "Continue task: appWidgetSize=" + appWidgetSize + ", appWidgetId=" + appWidgetState.appWidgetId
                )
                */
                }
            }
            handler.postDelayed(updateAppWidgetRunnable, intervalMillisecond)
        }

        // 前回実行時刻を現在時刻に置き換える
        appWidgetState.previousSecond = currentSecond

        // appWidgetState を保存する
        appWidgetState.save(context)
    }

    internal fun updateDigitalClockWidgetViews(
        context: Context,
        appWidgetId: Int,
        views: RemoteViews,
        dateTime: LocalDateTime
    ) {
        // 環境設定値の取得
        val appWidgetSetting = AppWidgetSetting.load(context, appWidgetId)

        // 時の設定
        val formattedHour = "%02d".format(dateTime.hour)
        views.setTextViewText(R.id.HourView, formattedHour)
        views.setTextColor(R.id.HourView,  appWidgetSetting.foregroundColorCode)

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
        val dateFormatter = DateTimeFormatter.ofPattern(appWidgetSetting.dateFormat)
        val formattedDate = dateTime.format(dateFormatter)
        views.setTextViewText(R.id.DateView, formattedDate)
        views.setTextColor(R.id.DateView, appWidgetSetting.foregroundColorCode)
    }

    private fun getRemoteViews(context:Context, appWidgetManager: AppWidgetManager, appWidgetId: Int): RemoteViews {
        // Widget に関する設定を取得する
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId);

        // Widget の現在のサイズを取得する
        val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 0
        val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) ?: 0

        // 現在の Widget が占めるセル数 (列数および行数) を求める
        val rows = getCellsForSize(minHeight)
        val columns = getCellsForSize(minWidth)

        // 行数と列数により適切なレイアウトを決定する
        return RemoteViews(context.packageName, selectLayout(columns, rows)).also {
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
                it.setOnClickPendingIntent(R.id.AppWidgetRootView, pendingIntent)
                Log.d(
                    javaClass.canonicalName + ".getRemoteViews()",
                    "Registered action: appWidgetId=" + appWidgetId + ", packageName=" + intent.component?.packageName + ", className=" + intent.component?.className + ", actionName=" + intent.action
                )
            }
        }
    }

    fun selectLayout(columns: Int, rows: Int): Int {
        return when {
            columns >= 4 && rows >= 2 -> R.layout.app_widget4x2
            else -> R.layout.app_widget3x2
        }
    }

    companion object
    {
        // 画面上の長さからセル数を求める
        private fun getCellsForSize(size:Int):Int {
            return (size + 30) / 70
        }

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
