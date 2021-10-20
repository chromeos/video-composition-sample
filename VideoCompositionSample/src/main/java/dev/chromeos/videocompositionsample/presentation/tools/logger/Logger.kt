/*
 * Copyright (c) 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.chromeos.videocompositionsample.presentation.tools.logger

import android.util.Log
import javax.inject.Inject

class Logger
@Inject constructor() : ILogger {

    private val TAG = "Composition"

    override fun e(message: String?) {
        if (dev.chromeos.videocompositionsample.presentation.BuildConfig.DEBUG)
            Log.e(TAG, "${getLocation()} : ${message ?: ""}")
    }

    override fun w(message: String?) {
        if (dev.chromeos.videocompositionsample.presentation.BuildConfig.DEBUG)
            Log.w(TAG, "${getLocation()} : ${message ?: ""}")
    }

    override fun d(message: String?) {
        if (dev.chromeos.videocompositionsample.presentation.BuildConfig.DEBUG)
            Log.d(TAG, "${getLocation()} : ${message ?: ""}")
    }

    override fun i(message: String?) {
        if (dev.chromeos.videocompositionsample.presentation.BuildConfig.DEBUG)
            Log.i(TAG, "${getLocation()} : ${message ?: ""}")
    }

    override fun e(tag: String, message: String?) {
        if (dev.chromeos.videocompositionsample.presentation.BuildConfig.DEBUG)
            Log.e(tag, "${getLocation()} : ${message ?: ""}")
    }

    override fun w(tag: String, message: String?) {
        if (dev.chromeos.videocompositionsample.presentation.BuildConfig.DEBUG)
            Log.w(tag, "${getLocation()} : ${message ?: ""}")
    }

    override fun d(tag: String, message: String?) {
        if (dev.chromeos.videocompositionsample.presentation.BuildConfig.DEBUG)
            Log.d(tag, "${getLocation()} : ${message ?: ""}")
    }

    override fun i(tag: String, message: String?) {
        if (dev.chromeos.videocompositionsample.presentation.BuildConfig.DEBUG)
            Log.i(tag, "${getLocation()} : ${message ?: ""}")
    }

    private fun getLocation(): String {

        val logClassName = Logger::class.java.name
        val traces = Thread.currentThread().stackTrace
        var found = false

        for (trace in traces) {
            try {
                if (found) {
                    if (!trace.className.startsWith(logClassName)) {
                        val clazz = Class.forName(trace.className)
                        var clazzName = clazz.simpleName
                        if (clazzName.isBlank()) {
                            clazzName = clazz.name
                        }
                        return " [$clazzName.${trace.methodName}:${trace.lineNumber}]"
                    }
                } else if (trace.className.startsWith(logClassName)) {
                    found = true
                }
            } catch (e: Throwable) {
                Log.e(TAG, e.message?: "")
            }

        }
        return " []"
    }
}
