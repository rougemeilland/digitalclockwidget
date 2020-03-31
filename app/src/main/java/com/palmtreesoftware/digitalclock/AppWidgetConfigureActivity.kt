package com.palmtreesoftware.digitalclock

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.Log
import kotlinx.android.synthetic.main.app_widget_configure.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AppWidgetConfigureActivity : Activity() {
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private val colorCodeFilter: RegexInputFilter =
        RegexInputFilter("^|(#[0-9a-fA-F]{0,6})|([a-zA-Z]+)$")
    private val exampleViewState = object : ExampleViewState() {
        override fun drawView(appWidgetSetting: AppWidgetSetting) {
            drawExampleView(appWidgetSetting)
        }
    }

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        setResult(RESULT_CANCELED)
        setContentView(R.layout.app_widget_configure)

        Log.d(
            javaClass.canonicalName + ".onCreate()",
            "Started"
        )
        startUpActivity(intent)
    }

    override fun onDestroy() {
        Log.d(
            javaClass.canonicalName + ".onDestroy()",
            "Started"
        )
        cleanUpActivity()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        // この Activity のインスタンスが、別の intent のために再利用されたことを検出した

        val previousAppWidgetId =
            AppWidget.parseExterasAsAppWidgetId(getIntent().extras) // 以前の intent.extras から取得した appWidgetId
        val currentAppWidgetId =
            AppWidget.parseExterasAsAppWidgetId(intent?.extras) // 新たな intent.extras から取得した appWidgetId
        Log.d(
            javaClass.canonicalName + ".onNewIntent()",
            "Started: previousAppWidgetId=" + previousAppWidgetId + ", currentAppWidgetId=" + currentAppWidgetId
        )

        // 以前の intent に関する設定をクリアする
        cleanUpActivity()

        // 以前の intent に対して RESULT_CANCELED と extras を設定する (不要かもしれないが念のため)
        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, previousAppWidgetId)
        }
        setResult(RESULT_CANCELED, resultValue)

        // 新たな intent について Activity の再設定を行う
        startUpActivity(intent)
    }

    override fun onStart() {
        // ある AppWidget の AppWidgetConfigureActivity が表示され終了する前に同じ AppWidget から
        // AppWidgetConfigureActivity を表示しようとすると、
        // 新たな AppWidgetConfigureActivity は起動されず、
        // 既に表示されている AppWidgetConfigureActivity の onStart() が呼び出されて再利用される
        super.onStart()

        Log.d(
            javaClass.canonicalName + ".onStart()",
            "Started"
        )
    }

    private fun startUpActivity(intent: Intent?) {
        // intent.extras から appWidgetId を取得する
        val extrasContainer = IntentExtrasContainer.parse(intent?.extras)

        // appWidgetId が正しくない場合は Activity を終了する
        if (extrasContainer.appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.d(
                javaClass.canonicalName + ".StartUpActivity()",
                "Started (Detected bad appWidgetId)"
            )
            // Avtivity の終了とともに、タスク履歴から自身を消去する
            // (誤ってタスク履歴から再実行されることを防ぐため。finish() を使用するとタスク履歴に残ったままになってしまう。)
            finishAndRemoveTask()
            return
        }

        appWidgetId = extrasContainer.appWidgetId
        Log.d(
            javaClass.canonicalName + ".StartUpActivity()",
            "Started: appWidgetId=" + appWidgetId
        )
        // この Activity が Widget のクリックにより表示された場合は、一部の View の表示を変える
        if (extrasContainer.launchedOnWidgetClicked) {
            // "ADD_WIDGET" ボタンの名前を "OK" に変更する
            config_ok_button.text = getString(R.string.config_ok_button_text)
        }

        // Activity の表示内容を初期化する
        AppWidgetSetting.load(this@AppWidgetConfigureActivity, appWidgetId).let {
            initializeForm(it)
            exampleViewState.setSetting(it)
            drawExampleView(it)
        }

        // ”PREVIEW" ボタンのクリックハンドラを登録する
        config_preview_button.setOnClickListener {
            val appWidgetSetting = validateForm()?.let {
                exampleViewState.setSetting(it)
                drawExampleView(it)
            }
        }

        // "RESET" ボタンのクリックハンドラを登録する
        config_reset_button.setOnClickListener {
            config_date_format_view.setText(getString(R.string.default_date_format))
            config_foreground_color_view.setText(getString(R.string.default_foreground_color))
            AppWidgetSetting.initialValue(this, appWidgetId).let {
                exampleViewState.setSetting(it)
                drawExampleView(it)
            }
        }

        // "OK" ボタンのクリックハンドラを登録する
        config_ok_button.setOnClickListener {
            val context = this@AppWidgetConfigureActivity

            Log.d(
                javaClass.canonicalName + ".StartUpActivity()",
                "Clicked ok button: appWidgetId=" + appWidgetId
            )

            val appWidgetSetting = validateForm()
            if (appWidgetSetting != null) {
                appWidgetSetting.save(context)

                /*
                // 以下の理由から、ここからの描画指示は省略する
                // ・Widget の layout が複数あり、 RemoteViews の第2パラメタを解決できないため
                // ・以降に実行する requestToRefreshWidgetInstance(appWidgetId) で別途 Widget の再描画の指示を出しているため
                val appWidgetManager = AppWidgetManager.getInstance(context)
                RemoteViews(context.packageName, R.layout.????????).also { views ->
                    val nowDateTime = LocalDateTime.now();
                    updateDigitalClockWidgetViews(
                        context,
                        appWidgetId,
                        views,
                        nowDateTime
                    )
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
               */

                // Widget に通知する intent を作成する
                val resultValue = Intent().apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                setResult(RESULT_OK, resultValue)

                // Widget に再描画を指示する
                requestToRefreshWidgetInstance(appWidgetId)

                // Avtivity の終了とともに、タスク履歴から自身を消去する
                // (誤ってタスク履歴から再実行されることを防ぐため。finish() を使用するとタスク履歴に残ったままになってしまう。)
                finishAndRemoveTask()
            }
        }

        // "CANCEL" ボタンのクリックハンドラを登録する
        config_cancel_button.setOnClickListener {
            Log.d(
                javaClass.canonicalName + ".StartUpActivity()",
                "Clicked cancel button: appWidgetId=" + appWidgetId
            )

            // Widget に通知する intent を作成する
            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(RESULT_CANCELED, resultValue)

            // Avtivity の終了とともに、タスク履歴から自身を消去する
            // (誤ってタスク履歴から再実行されることを防ぐため。finish() を使用するとタスク履歴に残ったままになってしまう。)
            finishAndRemoveTask()
        }

        // サンプル表示の自動再描画を開始する
        exampleViewState.start()
    }

    private fun cleanUpActivity() {
        Log.d(
            javaClass.canonicalName + ".CleanUpActivity()",
            "Started: appWidgetId=" + appWidgetId
        )

        // サンプル表示の自動再描画を停止する
        exampleViewState.stop()

        // プロパティを初期化する
        appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    }

    private fun initializeForm(appWidgetSetting: AppWidgetSetting) {
        exampleViewState.setSetting(appWidgetSetting)
        config_foreground_color_view.filters = arrayOf(colorCodeFilter)
        config_date_format_view.setText(appWidgetSetting.dateFormat)
        config_foreground_color_view.setText(appWidgetSetting.foregroundColorName)
        appWidgetSetting.foregroundColorCode
            .let { color ->
                arrayOf(
                    config_preview_hourminute_view,
                    config_preview_second_view,
                    config_preview_date_view
                ).forEach { it.setTextColor(color) }
            }
    }

    private fun validateForm(): AppWidgetSetting? {
        val dateFormat = config_date_format_view.text.toString().trim()
        val dateFormatValidity =
            when {
                !dateFormat.isEmpty() -> {
                    true.also { config_date_format_view.setError(null) }
                }
                else -> {
                    false.also { config_date_format_view.setError(getString(R.string.config_date_format_error_text)) }
                }
            }

        val foregroundColorName = config_foreground_color_view.text.toString().trim()
        val parsedForegroundColorCode = ParsedColorCode.fromString(foregroundColorName)
        val foregroundColorValidity =
            when {
                parsedForegroundColorCode.success -> {
                    true.also { config_foreground_color_view.setError(null) }
                }
                else -> {
                    false.also { config_foreground_color_view.setError(getString(R.string.config_foreground_color_error_text)) }
                }
            }

        return when {
            !dateFormatValidity -> {
                null.also { config_date_format_view.requestFocus() }
            }
            !foregroundColorValidity -> {
                null.also { config_foreground_color_view.requestFocus() }
            }
            else -> {
                assert(parsedForegroundColorCode.success)
                AppWidgetSetting(
                    appWidgetId,
                    dateFormat,
                    foregroundColorName,
                    parsedForegroundColorCode.colorCode
                )
            }
        }
    }

    private fun drawExampleView(appWidgetSetting: AppWidgetSetting) {
        // 文字の色の設定
        arrayOf(
            config_preview_hourminute_view,
            config_preview_second_view,
            config_preview_date_view
        ).forEach {
            it.setTextColor(appWidgetSetting.foregroundColorCode)
        }

        // 背景色の決定と設定
        config_preview_box.setBackgroundColor(
            Color.valueOf(appWidgetSetting.foregroundColorCode).let {
                if (it.red() + it.green() + it.blue() >=
                    (it.colorSpace.getMinValue(0) + it.colorSpace.getMaxValue(0)) / 2 +
                    (it.colorSpace.getMinValue(1) + it.colorSpace.getMaxValue(1)) / 2 +
                    (it.colorSpace.getMinValue(2) + it.colorSpace.getMaxValue(2)) / 2
                ) {
                    // 文字の色が比較的明るい場合

                    // 背景色を暗くする
                    getColor(R.color.configPreviewDarkBackgroundColor)
                } else {
                    // 文字の色が比較的暗い場合

                    // 背景色を暗くする
                    getColor(R.color.configPreviewLightBackgroundColor)
                }
            })

        // 現在時刻の設定
        val nowDateTime = LocalDateTime.now();
        config_preview_hourminute_view.text =
            "%02d:%02d".format(nowDateTime.hour, nowDateTime.minute)
        config_preview_second_view.text = ":%02d".format(nowDateTime.second)
        val dateFormatter = DateTimeFormatter.ofPattern(appWidgetSetting.dateFormat)
        config_preview_date_view.text = nowDateTime.format(dateFormatter)
    }

    // appWidgetId の ID を持つ Widget のインスタンスに REFRESH_WIDGET メッセージを送信する
    private fun requestToRefreshWidgetInstance(appWidgetId: Int) {
        Intent(this, AppWidget::class.java).also { intent ->
            Log.d(
                javaClass.canonicalName + ".requestToRefreshWidgetInstance()",
                "Clicked cancel button: appWidgetId=" + appWidgetId
            )

            intent.action = AppWidgetAction.REFRESH_WIDGET.actionName
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

            // intent を送信する
            sendBroadcast(intent)
        }
    }

    private abstract class ExampleViewState {
        private val exampleRefreshingHandler: Handler = Handler()
        private var exampleViewSetting: AppWidgetSetting? = null
        private var exampleRefreshingRunable: Runnable = Runnable { }

        fun start() {
            // 表示さんプロを一定時間毎に再描画するハンドラを登録する
            Runnable {
                // 表示サンプルを再描画する
                exampleViewSetting?.let { drawView(it) }

                // 登録済みのコールバックを削除する
                stop()

                // exampleRefreshingCallback を 1000ms 後に再実行するようスケジュールする
                start()
            }.also {
                exampleRefreshingRunable = it
                exampleRefreshingHandler.postDelayed(it, Companion.intervalMillisecond)
            }
        }

        fun stop() {
            exampleRefreshingHandler.removeCallbacks(exampleRefreshingRunable)
        }

        fun setSetting(appWidgetSetting: AppWidgetSetting) {
            exampleViewSetting = appWidgetSetting
        }

        abstract fun drawView(appWidgetSetting: AppWidgetSetting)

        companion object {
            private const val intervalMillisecond: Long = 1000;
        }
    }

    companion object {
        private class IntentExtrasContainer(val appWidgetId: Int, val launchedOnWidgetClicked: Boolean)
        {
            companion object{
                fun parse(extras: Bundle?) : IntentExtrasContainer {
                    return if (extras == null) {
                        IntentExtrasContainer(
                            AppWidgetManager.INVALID_APPWIDGET_ID,
                            false
                        )
                    } else {
                        IntentExtrasContainer(
                            extras.getInt(
                                AppWidgetManager.EXTRA_APPWIDGET_ID,
                                AppWidgetManager.INVALID_APPWIDGET_ID
                            ), extras.getBoolean(AppWidgetExtrasKey.ON_CLICKED.keyName, false)
                        )
                    }
                }
            }
        }
    }
}