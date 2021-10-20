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

package dev.chromeos.videocompositionsample.data.repository

import android.content.Context
import dev.chromeos.videocompositionsample.data.repository.base.BasePrefRepo
import dev.chromeos.videocompositionsample.domain.entity.*
import dev.chromeos.videocompositionsample.domain.repository.ISettingsRepo
import javax.inject.Inject

class SettingsRepo
@Inject constructor(context: Context)
    : BasePrefRepo<Settings>(context, "pref_settings", Settings::class.java), ISettingsRepo {

    override fun get(): Settings {
        return super.get() ?: Settings(
                trackList = listOf(
                        Track(Clip.Video_3840x2160_30fps, Effect.Effect1),
                        Track(Clip.Video_1080x1920_30fps, Effect.Effect2),
                        Track(Clip.Video_3840x2160_60fps, Effect.Effect3),
                        Track(Clip.Video_1920x1080_120fps, Effect.Effect4)
                ),
                encoderCodecType = EncoderCodecType.h264,
                isEnableDecoderFallback = true,
                isSummaryVisible = false,
                isTestCaseMode = false
        )
    }
}