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

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.app_widget_configure.*


class AppWidgetConfigureActivity : AppCompatActivity() {
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private val exampleViewState = object : ExampleViewState() {
        override fun drawView(appWidgetSetting: AppWidgetSetting) {
            drawExampleView(appWidgetSetting)
        }
    }
    private var timeZoneNameSelectionItems: Array<SelectionItem> = arrayOf()

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        setResult(RESULT_CANCELED)
        setContentView(R.layout.app_widget_configure)

        if (Log.isLoggable(javaClass.simpleName, Log.DEBUG))
            Log.d(
                javaClass.simpleName,
                "onCreate"
            )

        timeZoneNameSelectionItems =
            SelectionItem.fromResource(this, R.array.config_timezone_name_selection_items)

        startUpActivity(intent)
    }

    override fun onDestroy() {
        if (Log.isLoggable(javaClass.simpleName, Log.DEBUG))
            Log.d(
                javaClass.simpleName,
                "onDestroy"
            )
        cleanUpActivity()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        // この Activity のインスタンスが、別の intent のために再利用されたことを検出した

        // 以前の intent.extras から取得した appWidgetId
        val previousAppWidgetId =
            AppWidget.parseExterasAsAppWidgetId(getIntent().extras)

        // 新たな intent.extras から取得した appWidgetId
        val currentAppWidgetId =
            AppWidget.parseExterasAsAppWidgetId(intent?.extras)

        if (Log.isLoggable(javaClass.simpleName, Log.DEBUG))
            Log.d(
                javaClass.simpleName,
                "onNewIntent: previousAppWidgetId=$previousAppWidgetId, currentAppWidgetId=$currentAppWidgetId"
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

        if (Log.isLoggable(javaClass.simpleName, Log.DEBUG))
            Log.d(
                javaClass.simpleName,
                "onStart"
            )
    }

    private fun startUpActivity(intent: Intent?) {
        if (Log.isLoggable(javaClass.simpleName, Log.DEBUG))
            Log.d(
                javaClass.simpleName,
                "startUpActivity"
            )

        // intent.extras から appWidgetId を取得する
        val extrasContainer = IntentExtrasContainer.parse(intent?.extras)

        // appWidgetId が正しくない場合は Activity を終了する
        if (extrasContainer.appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            if (Log.isLoggable(javaClass.simpleName, Log.DEBUG))
                Log.d(
                    javaClass.simpleName,
                    "startUpActivity: Detected bad appWidgetId"
                )
            // Avtivity の終了とともに、タスク履歴から自身を消去する
            // (誤ってタスク履歴から再実行されることを防ぐため。finish() を使用するとタスク履歴に残ったままになってしまう。)
            finishAndRemoveTask()
            return
        }

        appWidgetId = extrasContainer.appWidgetId
        if (Log.isLoggable(javaClass.simpleName, Log.DEBUG))
            Log.d(
                javaClass.simpleName,
                "startUpActivity: appWidgetId=$appWidgetId"
            )

        // この Activity が Widget のクリックにより表示された場合は、一部の View の表示を変える
        if (extrasContainer.launchedOnWidgetClicked) {
            // "ADD_WIDGET" ボタンの名前を "OK" に変更する
            config_ok_button.text = getString(R.string.config_ok_button_text)
        }

        // Activity の表示内容を初期化する
        AppWidgetSetting.load(this, appWidgetId).let {
            initializeForm(it)
            exampleViewState.setSetting(it)
            drawExampleView(it)
        }

        // タイムゾーン選択 spinner のハンドラを登録する
        config_timesone_name.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedItem = getSelectedItem(timeZoneNameSelectionItems, position)
                    config_timesone_by_number_box.visibility =
                        if (selectedItem.id == AppWidgetTimeZone.timeZoneIdOfTimeDifferenceExpression) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    config_timesone_name.setSelection(0)
                }
            }

        // ”PREVIEW" ボタンのクリックハンドラを登録する
        config_preview_button.setOnClickListener {
            validateForm()?.let {
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
            if (Log.isLoggable(javaClass.simpleName, Log.DEBUG))
                Log.d(
                    javaClass.simpleName,
                    "StartUpActivity: Clicked OK button: appWidgetId=$appWidgetId"
                )

            // 入力値の正当性を検証する
            val appWidgetSetting = validateForm()
            if (appWidgetSetting != null) {
                // 入力値がすべて正しかった場合

                // 入力値を保存する
                appWidgetSetting.save(this)

                // 本来の AppWidget のサンプルプログラムであればここで直接 Widget の画面の更新を行うが、
                // 以下の理由から、ここでは行わない
                // ・Widget の layout が複数あり、 RemoteViews の第2パラメタを解決できないため
                // ・以降に実行する requestToRefreshWidgetInstance(appWidgetId) で別途 Widget の再描画の指示を出しているため

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
            if (Log.isLoggable(javaClass.simpleName, Log.DEBUG))
                Log.d(
                    javaClass.simpleName,
                    "StartUpActivity: Clicked Cancel button: appWidgetId=$appWidgetId"
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
        if (Log.isLoggable(javaClass.simpleName, Log.DEBUG))
            Log.d(
                javaClass.simpleName,
                "CleanUpActivity: appWidgetId=$appWidgetId"
            )

        // サンプル表示の自動再描画を停止する
        exampleViewState.stop()

        // プロパティを初期化する
        appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    }

    private fun initializeForm(appWidgetSetting: AppWidgetSetting) {
        // タイムゾーン名選択 spinner の選択項目を設定する
        config_timesone_name.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            timeZoneNameSelectionItems.map { it.text }
        )
            .also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

        // タイムゾーンの時差(時)の選択項目を設定する
        config_timesone_hour.maxValue = AppWidgetTimeZone.maxTimeZoneHour
        config_timesone_hour.minValue = AppWidgetTimeZone.minTimeZoneHour
        config_timesone_hour.setFormatter { value ->
            AppWidgetTimeZone.getHourStringOnForm(
                "%+03d",
                value
            )
        }

        // タイムゾーンの時差(分)の選択項目を設定する
        config_timesone_minute.maxValue = AppWidgetTimeZone.maxTimeZoneMinute
        config_timesone_minute.minValue = AppWidgetTimeZone.minTimeZoneMinute
        config_timesone_minute.setFormatter { value ->
            AppWidgetTimeZone.getMinuteStringOnForm(
                "%02d",
                value
            )
        }

        // 表示サンプルのデータを初期化する
        exampleViewState.setSetting(appWidgetSetting)

        // 表示サンプルの文字色を初期化する
        appWidgetSetting.foregroundColorCode
            .let { color ->
                arrayOf(
                    config_preview_hourminute_view,
                    config_preview_second_view,
                    config_preview_date_view
                ).forEach { it.setTextColor(color) }
            }

        // フォームの入力値を初期化する
        config_foreground_color_view.filters = arrayOf(colorCodeFilter)
        config_date_format_view.setText(appWidgetSetting.dateFormat)
        config_foreground_color_view.setText(appWidgetSetting.foregroundColorName)
        config_timesone_name.setSelection(
            timeZoneNameSelectionItems.mapIndexed { index, selectionItem ->
                Pair(index, selectionItem.id)
            }
                .filter {
                    it.second == appWidgetSetting.timeZone.idOnForm
                }.map {
                    it.first
                }.firstOrNull()
                ?: 0
        )
        config_timesone_hour.value = appWidgetSetting.timeZone.hourOnForm
        config_timesone_minute.value = appWidgetSetting.timeZone.minuteOnForm
    }

    private fun validateForm(): AppWidgetSetting? {
        val dateFormat = config_date_format_view.text.toString().trim()
        val dateFormatValidity =
            when {
                dateFormat.isNotEmpty() -> {
                    true.also { config_date_format_view.error = null }
                }
                else -> {
                    false.also {
                        config_date_format_view.error =
                            getString(R.string.config_date_format_error_text)
                    }
                }
            }

        val foregroundColorName = config_foreground_color_view.text.toString().trim()
        val parsedForegroundColorCode = ParsedColorCode.fromString(foregroundColorName)
        val foregroundColorValidity =
            when {
                parsedForegroundColorCode.success -> {
                    true.also { config_foreground_color_view.error = null }
                }
                else -> {
                    false.also {
                        config_foreground_color_view.error =
                            getString(R.string.config_foreground_color_error_text)
                    }
                }
            }
        val selectedTimeZoneItem =
            getSelectedItem(timeZoneNameSelectionItems, config_timesone_name.selectedItemPosition)
        return when {
            !dateFormatValidity -> {
                null.also {
                    if (Log.isLoggable(javaClass.simpleName, Log.DEBUG))
                        Log.d(
                            javaClass.simpleName,
                            "validateForm: Bad date format"
                        )
                    config_date_format_view.requestFocus()
                }
            }
            !foregroundColorValidity -> {
                null.also {
                    if (Log.isLoggable(javaClass.simpleName, Log.DEBUG))
                        Log.d(
                            javaClass.simpleName,
                            "validateForm: Bad foreground color"
                        )
                    config_foreground_color_view.requestFocus()
                }
            }
            else -> {
                if (Log.isLoggable(javaClass.simpleName, Log.DEBUG))
                    Log.d(
                        javaClass.simpleName,
                        "validateForm: Validating: selectedTimeZone.id=${selectedTimeZoneItem.id}, config_timesone_hour.Value=${config_timesone_hour.value}, config_timesone_minute.value=${config_timesone_minute.value}"
                    )
                AppWidgetSetting(
                    appWidgetId,
                    dateFormat,
                    foregroundColorName,
                    parsedForegroundColorCode.colorCode,
                    AppWidgetTimeZone.fromFormValues(
                        selectedTimeZoneItem.id,
                        config_timesone_hour.value,
                        config_timesone_minute.value
                    ).also {
                        if (Log.isLoggable(javaClass.simpleName, Log.DEBUG))
                            Log.d(
                                javaClass.simpleName,
                                "validateForm: Validated (OK): timeZoneId=" + it.id
                            )
                    }
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
            appWidgetSetting.foregroundColorCode.let { foregroundColorCode ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // 文字の色の大体の明るさを計算する
                    // ※ RGB要素を分解して足し合わせ、平均以上かどうかをチェックしているだけ
                    Color.valueOf(foregroundColorCode).let { foregroundColor ->
                        if (foregroundColor.red() + foregroundColor.green() + foregroundColor.blue() >=
                            (foregroundColor.colorSpace.getMinValue(0) + foregroundColor.colorSpace.getMaxValue(
                                0
                            )) / 2 +
                            (foregroundColor.colorSpace.getMinValue(1) + foregroundColor.colorSpace.getMaxValue(
                                1
                            )) / 2 +
                            (foregroundColor.colorSpace.getMinValue(2) + foregroundColor.colorSpace.getMaxValue(
                                2
                            )) / 2
                        ) {
                            // 文字の色が比較的明るい場合

                            // 背景色を暗くする
                            getColor(R.color.configPreviewDarkBackgroundColor)
                        } else {
                            // 文字の色が比較的暗い場合

                            // 背景色を暗くする
                            getColor(R.color.configPreviewLightBackgroundColor)
                        }
                    }
                } else {
                    // 文字の色の大体の明るさを計算する
                    // ※ RGB要素を分解して足し合わせ、平均以上かどうかをチェックしているだけ
                    if ((foregroundColorCode.shr(16) and 0xff) + (foregroundColorCode.shr(8) and 0xff) + (foregroundColorCode and 0xff) >= 255 * 3 / 2) {
                        // 文字の色が比較的明るい場合

                        // 背景色を暗くする
                        // ※ このバージョンの SDK だとカラーリソースが使用できないため、やむを得ずカラーコードをハードコードしている
                        0xff333333.toInt()
                    } else {
                        // 文字の色が比較的暗い場合

                        // 背景色を明るくする
                        // ※ このバージョンの SDK だとカラーリソースが使用できないため、やむを得ずカラーコードをハードコードしている
                        0xffdddddd.toInt()
                    }
                }
            })

        // 現在時刻の設定
        val nowDateTime = AppWidgetDateTime.now(
            appWidgetSetting.dateFormat,
            appWidgetSetting.timeZone
        )
        config_preview_hourminute_view.text =
            getString(R.string.config_exampleview_hourminute_format).format(
                nowDateTime.hour,
                nowDateTime.minute
            )
        config_preview_second_view.text =
            getString(R.string.config_exampleview_minute_format).format(nowDateTime.second)
        config_preview_date_view.text = nowDateTime.dateString
    }

    // appWidgetId の ID を持つ Widget のインスタンスに REFRESH_WIDGET メッセージを送信する
    private fun requestToRefreshWidgetInstance(appWidgetId: Int) {
        Intent(this, AppWidget::class.java).also { intent ->
            if (Log.isLoggable(javaClass.simpleName, Log.DEBUG))
                Log.d(
                    javaClass.simpleName,
                    "requestToRefreshWidgetInstance: appWidgetId=$appWidgetId"
                )

            intent.action = AppWidgetAction.REFRESH_WIDGET.actionName
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

            // intent を送信する
            sendBroadcast(intent)
        }
    }

    private fun getSelectedItem(
        selectionItems: Array<SelectionItem>,
        position: Int
    ): SelectionItem {
        return selectionItems[if (position < 0 || position >= selectionItems.count()) {
            0
        } else {
            position
        }]
    }

    private class SelectionItem(val id: String, val text: String) {
        companion object {
            fun fromResource(context: Context, resourceId: Int): Array<SelectionItem> {
                return context.resources.getStringArray(resourceId).map { source ->
                    val m = selectionItemPattern.matchEntire(source)
                    if (m != null)
                        SelectionItem(m.destructured.component1(), m.destructured.component2())
                    else
                        SelectionItem("", "")
                }.filter { item ->
                    item.id.isNotEmpty() && item.text.isNotEmpty()
                }.toTypedArray()
            }
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
                exampleRefreshingHandler.postDelayed(it, intervalMillisecond)
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
            private const val intervalMillisecond: Long = 1000
        }
    }

    companion object {
        private val colorCodeFilter: RegexInputFilter =
            RegexInputFilter("^|(#[0-9a-fA-F]{0,6})|([a-zA-Z]+)$")
        private val selectionItemPattern = Regex("^([^@]+)@([^@]+)$")

        private class IntentExtrasContainer(
            val appWidgetId: Int,
            val launchedOnWidgetClicked: Boolean
        ) {
            companion object {
                fun parse(extras: Bundle?): IntentExtrasContainer {
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