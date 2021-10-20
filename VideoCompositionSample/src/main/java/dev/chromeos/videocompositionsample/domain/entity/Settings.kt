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

package dev.chromeos.videocompositionsample.domain.entity

import java.io.Serializable

data class Settings(
        val trackList: List<Track>,
        val encoderCodecType: EncoderCodecType,
        val isEnableDecoderFallback: Boolean,
        val isSummaryVisible: Boolean,
        val isTestCaseMode: Boolean,
        val isTestExportEnabled: Boolean = true,
        val selectedTestCaseIds: List<Int> = listOf(),
        val selectedExportCaseIds: List<Int> = listOf()
        ) : Serializable

data class Track(
        val clip: Clip,
        val effect: Effect
)

enum class Effect(val effectId: Int) {
    Effect1(1),
    Effect2(2),
    Effect3(3),
    Effect4(4);

    companion object {
        fun fromEffectId(value: Int): Effect? = values().find { it.effectId == value }
    }
}

enum class Clip(val clipId: Int) {
    Video_3840x2160_30fps(1),
    Video_1080x1920_30fps(2),
    Video_3840x2160_60fps(3),
    Video_1920x1080_120fps(4);

    companion object {
        fun fromClipId(value: Int): Clip? = values().find { it.clipId == value }
    }
}

enum class EncoderCodecType {
    h264,
    h265
}