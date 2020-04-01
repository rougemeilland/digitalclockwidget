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
    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        setContentView(R.layout.activity_main)

        Log.d(
            javaClass.canonicalName + ".onCreate()",
            "Started"
        )
        startUpActivity()
    }

    override fun onDestroy() {
        Log.d(
            javaClass.canonicalName + ".onDestroy()",
            "Started"
        )
        cleanUpActivity()
        super.onDestroy()
    }

    private fun startUpActivity() {
        Log.d(
            javaClass.canonicalName + ".StartUpActivity()",
            "Started"
        )

        // Activity の表示内容を初期化する
        AppWidgetGlobalSetting.load(this).let {
            initializeForm(it)
        }

        // "OK" ボタンのクリックハンドラを登録する
        global_config_ok_button.setOnClickListener {
            Log.d(
                javaClass.canonicalName + ".StartUpActivity()",
                "Clicked ok button"
            )

            val appWidgetGlobalSetting =  AppWidgetGlobalSetting(if (global_config_onclick_digitalclock_button.isChecked) {
                Log.d(
                    javaClass.canonicalName + ".StartUpActivity()",
                    "LAUNCH_GOOGLE_CLOCK_APPLICATION is checked"
                )
                AppWidgetClickAction.LAUNCH_GOOGLE_CLOCK_APPLICATION
            } else {
                Log.d(
                    javaClass.canonicalName + ".StartUpActivity()",
                    "LAUNCH_CONFIGURE is checked"
                )
                AppWidgetClickAction.LAUNCH_CONFIGURE
            })

            appWidgetGlobalSetting.save(this)
            Log.d(
                javaClass.canonicalName + ".StartUpActivity()",
                "appWidgetGlobalSetting.appWidgetClickAction=" + appWidgetGlobalSetting.appWidgetClickAction
            )

            val resultValue = Intent()
            setResult(RESULT_OK, resultValue)
            finishAndRemoveTask()
        }

        // "CANCEL" ボタンのクリックハンドラを登録する
        global_config_cancel_button.setOnClickListener {
            Log.d(
                javaClass.canonicalName + ".StartUpActivity()",
                "Clicked cancel button"
            )

            val resultValue = Intent()
            setResult(RESULT_CANCELED, resultValue)
            finishAndRemoveTask()
        }
    }

    private fun cleanUpActivity() {
        Log.d(
            javaClass.canonicalName + ".CleanUpActivity()",
            "Started"
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
                Log.d(
                    javaClass.canonicalName + ".initializeForm()",
                    googleClockPackageName + " is not installed"
                )
                global_config_onclick_digitalclock_button.isEnabled = false
            } else if (!googlePackageInfo.enabled) {
                Log.d(
                    javaClass.canonicalName + ".initializeForm()",
                    googleClockPackageName + " is installed, but not enabled"
                )
                global_config_onclick_digitalclock_button.isEnabled = false
            } else {
                Log.d(
                    javaClass.canonicalName + ".initializeForm()",
                    googleClockPackageName + " is installed and enabled"
                )
                global_config_onclick_digitalclock_button.isEnabled = true
            }

            when (appWidgetGlobalSetting.appWidgetClickAction) {
                AppWidgetClickAction.LAUNCH_GOOGLE_CLOCK_APPLICATION -> {
                    if (!global_config_onclick_digitalclock_button.isEnabled)
                    {
                        // 以前に google 時計を起動するように設定されていたが、 google 時計がアンインストールされているか無効化されているため、
                        // 今回から AppWidgetConfigureActivity を表示するように暗黙的に設定を変更する
                        global_config_onclick_configure_button.isChecked = true
                        AppWidgetGlobalSetting(AppWidgetClickAction.LAUNCH_CONFIGURE).save(this)
                        Log.d(
                            javaClass.canonicalName + ".initializeForm()",
                            "global_config_onclick_configure_button is implicitly checked (google clock is not installed ot not enabled)"
                        )
                    }
                    else {
                        global_config_onclick_digitalclock_button.isChecked = true
                        Log.d(
                            javaClass.canonicalName + ".initializeForm()",
                            "global_config_onclick_digitalclock_button is checked"
                        )
                    }
                }
                else -> {
                    global_config_onclick_configure_button.isChecked = true
                    Log.d(
                        javaClass.canonicalName + ".initializeForm()",
                        "global_config_onclick_configure_button is checked"
                    )
                }
            }
        }
    }

    companion object {
        private val googleClockPackageName = "com.google.android.deskclock"
    }
}
