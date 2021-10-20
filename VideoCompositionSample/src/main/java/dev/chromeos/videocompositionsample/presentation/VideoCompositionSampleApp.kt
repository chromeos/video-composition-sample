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

package dev.chromeos.videocompositionsample.presentation

import android.content.Context
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import dev.chromeos.videocompositionsample.presentation.di.AppComponent
import dev.chromeos.videocompositionsample.presentation.di.modules.CommonModule

class VideoCompositionSampleApp : MultiDexApplication() {

    val appComponent: AppComponent by lazy {
        dev.chromeos.videocompositionsample.presentation.di.DaggerAppComponent
                .builder()
                .commonModule(CommonModule(this))
                .build()
    }

    override fun attachBaseContext(context: Context) {
        super.attachBaseContext(context)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
    }
}