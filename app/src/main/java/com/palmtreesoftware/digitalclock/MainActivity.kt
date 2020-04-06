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

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    private val tagOfLog: String = "GLOBALCONFIG"

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        setContentView(R.layout.activity_main)

        if (Log.isLoggable(tagOfLog, Log.DEBUG))
            Log.d(
                tagOfLog,
                "onCreate"
            )
        startUpActivity()
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

    private fun startUpActivity() {
        if (Log.isLoggable(tagOfLog, Log.DEBUG))
            Log.d(
                tagOfLog,
                "startUpActivity"
            )

        // Activity の表示内容を初期化する
        AppWidgetGlobalSetting.load(this).also {
            initializeForm(it)
        }

        // "OK" ボタンのクリックハンドラを登録する
        globalconfig_ok.setOnClickListener {
            if (Log.isLoggable(tagOfLog, Log.DEBUG))
                Log.d(
                    tagOfLog,
                    "StartUpActivity: Clicked OK button"
                )

            val appWidgetGlobalSetting = AppWidgetGlobalSetting(
                if (globalconfig_onclickaction_googleclock.isChecked) {
                    if (Log.isLoggable(tagOfLog, Log.DEBUG))
                        Log.d(
                            tagOfLog,
                            "StartUpActivity: LAUNCH_GOOGLE_CLOCK_APPLICATION is checked"
                        )
                    AppWidgetClickAction.LAUNCH_GOOGLE_CLOCK_APPLICATION
                } else {
                    if (Log.isLoggable(tagOfLog, Log.DEBUG))
                        Log.d(
                            tagOfLog,
                            "StartUpActivity: LAUNCH_CONFIGURE is checked"
                        )
                    AppWidgetClickAction.LAUNCH_CONFIGURE
                }
            )

            appWidgetGlobalSetting.save(this)
            if (Log.isLoggable(tagOfLog, Log.DEBUG))
                Log.d(
                    tagOfLog,
                    "StartUpActivity: appWidgetGlobalSetting.appWidgetClickAction=${appWidgetGlobalSetting.appWidgetClickAction}"
                )

            val resultValue = Intent()
            setResult(RESULT_OK, resultValue)
            finishAndRemoveTask()
        }

        // "CANCEL" ボタンのクリックハンドラを登録する
        globalconfig_cancel.setOnClickListener {
            if (Log.isLoggable(tagOfLog, Log.DEBUG))
                Log.d(
                    tagOfLog,
                    "StartUpActivity: Clicked Cancel button"
                )

            val resultValue = Intent()
            setResult(RESULT_CANCELED, resultValue)
            finishAndRemoveTask()
        }
    }

    private fun cleanUpActivity() {
        if (Log.isLoggable(tagOfLog, Log.DEBUG))
            Log.d(
                tagOfLog,
                "cleanUpActivity"
            )
    }

    private fun initializeForm(appWidgetGlobalSetting: AppWidgetGlobalSetting) {
        val installedApplicationMap =
            mutableMapOf<String, ApplicationInfo>().also { installedAppMap ->
                packageManager.getInstalledApplications(0).forEach { installedApp ->
                    installedAppMap[installedApp.packageName] = installedApp
                }
            }

        installedApplicationMap[googleClockPackageName].let { googlePackageInfo ->
            if (googlePackageInfo == null) {
                if (Log.isLoggable(tagOfLog, Log.DEBUG))
                    Log.d(
                        tagOfLog,
                        "initializeForm: Not installed $googleClockPackageName"
                    )
                globalconfig_onclickaction_googleclock.isEnabled = false
            } else if (!googlePackageInfo.enabled) {
                if (Log.isLoggable(tagOfLog, Log.DEBUG))
                    Log.d(
                        tagOfLog,
                        "initializeForm: Installed, but disabled $googleClockPackageName"
                    )
                globalconfig_onclickaction_googleclock.isEnabled = false
            } else {
                if (Log.isLoggable(tagOfLog, Log.DEBUG))
                    Log.d(
                        tagOfLog,
                        "initializeForm: Installed and enabled $googleClockPackageName"
                    )
                globalconfig_onclickaction_googleclock.isEnabled = true
            }

            when (appWidgetGlobalSetting.appWidgetClickAction) {
                AppWidgetClickAction.LAUNCH_GOOGLE_CLOCK_APPLICATION -> {
                    if (!globalconfig_onclickaction_googleclock.isEnabled) {
                        // 以前に google 時計を起動するように設定されていたが、 google 時計がアンインストールされているか無効化されているため、
                        // 今回から AppWidgetConfigureActivity を表示するように暗黙的に設定を変更する
                        globalconfig_onclickaction_configure.isChecked = true
                        AppWidgetGlobalSetting(AppWidgetClickAction.LAUNCH_CONFIGURE).save(this)
                        if (Log.isLoggable(tagOfLog, Log.DEBUG))
                            Log.d(
                                tagOfLog,
                                "initializeForm: LAUNCH_GOOGLE_CLOCK_APPLICATION is selected, but LAUNCH_CONFIGURE is used, because globalconfig_onclickaction_googleclock is disabled"
                            )
                    } else {
                        globalconfig_onclickaction_googleclock.isChecked = true
                        if (Log.isLoggable(tagOfLog, Log.DEBUG))
                            Log.d(
                                tagOfLog,
                                "initializeForm: globalconfig_onclickaction_googleclock is checked"
                            )
                    }
                }
                else -> {
                    globalconfig_onclickaction_configure.isChecked = true
                    if (Log.isLoggable(tagOfLog, Log.DEBUG))
                        Log.d(
                            tagOfLog,
                            "initializeForm: globalconfig_onclickaction_configure is checked"
                        )
                }
            }
        }
    }

    companion object {
        private const val googleClockPackageName = "com.google.android.deskclock"
    }
}
