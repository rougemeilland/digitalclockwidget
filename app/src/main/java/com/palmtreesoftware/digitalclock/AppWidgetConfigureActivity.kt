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
    private val tagOfLog: String = "CONFIGURE"
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private val exampleViewState = object : ExampleViewState() {
        override fun drawView(appWidgetSetting: AppWidgetSetting) {
            drawExampleView(appWidgetSetting)
        }
    }

    private val timeZoneNameSelectionItems: TimeZoneSelectionItems by lazy {
        TimeZoneSelectionItems.createInstance(this)
    }

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        setResult(RESULT_CANCELED)
        setContentView(R.layout.app_widget_configure)

        if (Log.isLoggable(tagOfLog, Log.DEBUG))
            Log.d(
                tagOfLog,
                "onCreate"
            )

        startUpActivity(intent)
    }

    override fun onDestroy() {
        if (Log.isLoggable(tagOfLog, Log.DEBUG))
            Log.d(
                tagOfLog,
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

        if (Log.isLoggable(tagOfLog, Log.DEBUG))
            Log.d(
                tagOfLog,
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

        if (Log.isLoggable(tagOfLog, Log.DEBUG))
            Log.d(
                tagOfLog,
                "onStart"
            )
    }

    private fun startUpActivity(intent: Intent?) {
        if (Log.isLoggable(tagOfLog, Log.DEBUG))
            Log.d(
                tagOfLog,
                "startUpActivity"
            )

        // intent.extras から appWidgetId を取得する
        val extrasContainer = IntentExtrasContainer.parse(intent?.extras)

        // appWidgetId が正しくない場合は Activity を終了する
        if (extrasContainer.appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            if (Log.isLoggable(tagOfLog, Log.DEBUG))
                Log.d(
                    tagOfLog,
                    "startUpActivity: Detected bad appWidgetId"
                )
            // Avtivity の終了とともに、タスク履歴から自身を消去する
            // (誤ってタスク履歴から再実行されることを防ぐため。finish() を使用するとタスク履歴に残ったままになってしまう。)
            finishAndRemoveTask()
            return
        }

        appWidgetId = extrasContainer.appWidgetId
        if (Log.isLoggable(tagOfLog, Log.DEBUG))
            Log.d(
                tagOfLog,
                "startUpActivity: appWidgetId=$appWidgetId"
            )

        // この Activity が Widget のクリックにより表示された場合は、一部の View の表示を変える
        if (extrasContainer.launchedOnWidgetClicked) {
            // "ADD_WIDGET" ボタンの名前を "OK" に変更する
            config_ok.text = getString(R.string.config_ok_button)
        }

        // Activity の表示内容を初期化する
        AppWidgetSetting.load(this, appWidgetId).let {
            initializeForm(it)
            exampleViewState.setSetting(it)
            drawExampleView(it)
        }

        // タイムゾーン選択 spinner のハンドラを登録する
        config_timeszne_name.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    config_timesone_bynumber.visibility =
                        when (timeZoneNameSelectionItems[position].id) {
                            AppWidgetTimeZone.timeZoneIdOfTimeDifferenceExpression -> {
                                View.VISIBLE
                            }
                            else -> {
                                View.GONE
                            }
                        }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    config_timeszne_name.setSelection(0)
                }
            }

        // ”PREVIEW" ボタンのクリックハンドラを登録する
        config_preview.setOnClickListener {
            validateForm()?.let {
                exampleViewState.setSetting(it)
                drawExampleView(it)
            }
        }

        // "RESET" ボタンのクリックハンドラを登録する
        config_reset.setOnClickListener {
            config_date_format.setText(getString(R.string.default_date_format))
            config_foreground_color.setText(getString(R.string.default_foreground_color))
            AppWidgetSetting.initialValue(this, appWidgetId).let {
                exampleViewState.setSetting(it)
                drawExampleView(it)
            }
        }

        // "OK" ボタンのクリックハンドラを登録する
        config_ok.setOnClickListener {
            if (Log.isLoggable(tagOfLog, Log.DEBUG))
                Log.d(
                    tagOfLog,
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
        config_cancel.setOnClickListener {
            if (Log.isLoggable(tagOfLog, Log.DEBUG))
                Log.d(
                    tagOfLog,
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
        if (Log.isLoggable(tagOfLog, Log.DEBUG))
            Log.d(
                tagOfLog,
                "CleanUpActivity: appWidgetId=$appWidgetId"
            )

        // サンプル表示の自動再描画を停止する
        exampleViewState.stop()

        // プロパティを初期化する
        appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    }

    private fun initializeForm(appWidgetSetting: AppWidgetSetting) {
        // タイムゾーン名選択 spinner の選択項目を設定する
        config_timeszne_name.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            timeZoneNameSelectionItems.getTextArray()
        )
            .also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

        // タイムゾーンの時差(時)の選択項目を設定する
        config_timesone_bynumber_hour.maxValue = AppWidgetTimeZone.maxTimeZoneHour
        config_timesone_bynumber_hour.minValue = AppWidgetTimeZone.minTimeZoneHour
        config_timesone_bynumber_hour.setFormatter { value ->
            AppWidgetTimeZone.getHourStringOnForm(
                "%+03d",
                value
            )
        }

        // タイムゾーンの時差(分)の選択項目を設定する
        config_timesone_bynumber_minute.maxValue = AppWidgetTimeZone.maxTimeZoneMinute
        config_timesone_bynumber_minute.minValue = AppWidgetTimeZone.minTimeZoneMinute
        config_timesone_bynumber_minute.setFormatter { value ->
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
                    config_preview_hour_minute,
                    config_preview_second,
                    config_preview_date
                ).forEach { it.setTextColor(color) }
            }

        // フォームの入力値を初期化する
        config_foreground_color.filters = arrayOf(colorCodeFilter)
        config_date_format.setText(appWidgetSetting.dateFormat)
        config_foreground_color.setText(appWidgetSetting.foregroundColorName)
        config_timeszne_name.setSelection(
            timeZoneNameSelectionItems[appWidgetSetting.timeZone.idOnForm].index
        )
        config_timesone_bynumber_hour.value = appWidgetSetting.timeZone.hourOnForm
        config_timesone_bynumber_minute.value = appWidgetSetting.timeZone.minuteOnForm
    }

    private fun validateForm(): AppWidgetSetting? {
        val dateFormat = config_date_format.text.toString().trim()
        val dateFormatValidity =
            when {
                dateFormat.isNotEmpty() -> {
                    true.also { config_date_format.error = null }
                }
                else -> {
                    false.also {
                        config_date_format.error =
                            getString(R.string.config_date_format_error)
                    }
                }
            }

        val foregroundColorName = config_foreground_color.text.toString().trim()
        val parsedForegroundColorCode = ParsedColorCode.fromString(foregroundColorName)
        val foregroundColorValidity =
            when {
                parsedForegroundColorCode.success -> {
                    true.also { config_foreground_color.error = null }
                }
                else -> {
                    false.also {
                        config_foreground_color.error =
                            getString(R.string.config_foreground_color_error)
                    }
                }
            }
        val selectedTimeZoneItem =
            timeZoneNameSelectionItems[config_timeszne_name.selectedItemPosition]
        return when {
            !dateFormatValidity -> {
                null.also {
                    if (Log.isLoggable(tagOfLog, Log.DEBUG))
                        Log.d(
                            tagOfLog,
                            "validateForm: Bad date format"
                        )
                    config_date_format.requestFocus()
                }
            }
            !foregroundColorValidity -> {
                null.also {
                    if (Log.isLoggable(tagOfLog, Log.DEBUG))
                        Log.d(
                            tagOfLog,
                            "validateForm: Bad foreground color"
                        )
                    config_foreground_color.requestFocus()
                }
            }
            else -> {
                if (Log.isLoggable(tagOfLog, Log.DEBUG))
                    Log.d(
                        tagOfLog,
                        "validateForm: Validating: selectedTimeZone.id=${selectedTimeZoneItem.id}, config_timesone_bynumber_hour.Value=${config_timesone_bynumber_hour.value}, config_timesone_bynumber_minute.value=${config_timesone_bynumber_minute.value}"
                    )
                AppWidgetSetting(
                    appWidgetId,
                    dateFormat,
                    foregroundColorName,
                    parsedForegroundColorCode.colorCode,
                    AppWidgetTimeZone.fromFormValues(
                        selectedTimeZoneItem.id,
                        config_timesone_bynumber_hour.value,
                        config_timesone_bynumber_minute.value
                    ).also {
                        if (Log.isLoggable(tagOfLog, Log.DEBUG))
                            Log.d(
                                tagOfLog,
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
            config_preview_time_zone_short_name,
            config_preview_hour_minute,
            config_preview_second,
            config_preview_date
        ).forEach {
            it.setTextColor(appWidgetSetting.foregroundColorCode)
        }

        // 背景色の決定と設定
        config_previewregion.setBackgroundColor(
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
        config_preview_hour_minute.text =
            getString(R.string.config_previewregion_hour_minute_format).format(
                nowDateTime.hour,
                nowDateTime.minute
            )
        config_preview_second.text =
            getString(R.string.config_previewregion_second_format).format(nowDateTime.second)
        config_preview_date.text = nowDateTime.dateString
        config_preview_time_zone_short_name.text =
            appWidgetSetting.timeZone.getTimeZoneShortName(this)
    }

    // appWidgetId の ID を持つ Widget のインスタンスに REFRESH_WIDGET メッセージを送信する
    private fun requestToRefreshWidgetInstance(appWidgetId: Int) {
        Intent(this, AppWidget::class.java).also { intent ->
            if (Log.isLoggable(tagOfLog, Log.DEBUG))
                Log.d(
                    tagOfLog,
                    "requestToRefreshWidgetInstance: appWidgetId=$appWidgetId"
                )

            intent.action = AppWidgetAction.REFRESH_WIDGET.actionName
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

            // intent を送信する
            sendBroadcast(intent)
        }
    }

    private class TimeZoneSelectionItems private constructor(sourceItems: Iterable<SelectionItem>) {
        private val tagOfLog: String = "CONFIGURE_TZ"
        private val itemsIndexedByArrayIndex: MutableMap<Int, SelectionItem> = mutableMapOf()
        private val itemsIndexedById: MutableMap<String, SelectionItem> = mutableMapOf()

        init {
            sourceItems.forEach {
                itemsIndexedByArrayIndex[it.index] = it
                itemsIndexedById[it.id] = it
            }
        }

        operator fun get(index: Int): SelectionItem {
            return itemsIndexedByArrayIndex[index] ?: getDefault().also {
                if (Log.isLoggable(tagOfLog, Log.DEBUG))
                    Log.d(
                        tagOfLog,
                        "get: Out of range index=$index"
                    )
            }
        }

        operator fun get(id: String): SelectionItem {
            return itemsIndexedById[id] ?: getDefault().also {
                if (Log.isLoggable(tagOfLog, Log.DEBUG))
                    Log.d(
                        tagOfLog,
                        "get: Unknown id=$id"
                    )
            }
        }

        fun isEmpty(): Boolean = itemsIndexedById.isEmpty()

        fun contains(index: Int): Boolean = itemsIndexedByArrayIndex.contains(index)

        fun contains(id: String): Boolean = itemsIndexedById.contains(id)

        fun getTextArray(): Array<String> =
            itemsIndexedByArrayIndex.values.sortedBy { it.index }.map { it.text }.toTypedArray()

        private fun getDefault(): SelectionItem {
            return itemsIndexedByArrayIndex[0] ?: throw Exception("source array is empty")
        }

        companion object {
            private val selectionItemPattern = Regex("^([^!]+)!([^!]+)$")

            fun createInstance(context: Context): TimeZoneSelectionItems {
                return TimeZoneSelectionItems(
                    context.resources.getStringArray(R.array.config_time_zone_selection_items)
                        .mapIndexed { index, sourceText ->
                            val m = selectionItemPattern.matchEntire(sourceText)
                            if (m != null) {
                                SelectionItem(
                                    index,
                                    m.destructured.component1(),
                                    m.destructured.component2()
                                )
                            } else
                                SelectionItem(-1, "", "")
                        }.filter { it.index >= 0 }
                ).also {
                    if (it.isEmpty())
                        throw Exception("TimeZoneSelectionItems.constructor: Resource is empty")
                    if (!it.contains(AppWidgetTimeZone.timeZoneIdOfSystemDefault))
                        throw Exception("TimeZoneSelectionItems.constructor: Resource is not contains system defau1t time zone")
                    if (!it.contains(AppWidgetTimeZone.timeZoneIdOfTimeDifferenceExpression))
                        throw Exception("TimeZoneSelectionItems.constructor: Resource is not contains time difference expression")
                    if (!it.contains(0))
                        throw Exception("TimeZoneSelectionItems.constructor: Resource is not contains first item")
                }
            }
        }
    }

    private class SelectionItem(val index: Int, val id: String, val text: String)

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