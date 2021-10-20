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

package dev.chromeos.videocompositionsample.data.data

import dev.chromeos.videocompositionsample.domain.entity.*

object TestSettingList {

    val settingList: List<Settings> = listOf(
            Settings(
                    trackList = listOf(
                            Track(
                                    clip = Clip.Video_1080x1920_30fps,
                                    effect = Effect.Effect1
                            )
                    ),
                    encoderCodecType = EncoderCodecType.h264,
                    isEnableDecoderFallback = true,
                    isSummaryVisible = true,
                    isTestCaseMode = true
            ),
            Settings(
                    trackList = listOf(
                            Track(
                                    clip = Clip.Video_1080x1920_30fps,
                                    effect = Effect.Effect1
                            ),
                            Track(
                                    clip = Clip.Video_1080x1920_30fps,
                                    effect = Effect.Effect2
                            )
                    ),
                    encoderCodecType = EncoderCodecType.h264,
                    isEnableDecoderFallback = true,
                    isSummaryVisible = true,
                    isTestCaseMode = true
            ),
            Settings(
                    trackList = listOf(
                            Track(
                                    clip = Clip.Video_1080x1920_30fps,
                                    effect = Effect.Effect1
                            ),
                            Track(
                                    clip = Clip.Video_1080x1920_30fps,
                                    effect = Effect.Effect2
                            ),
                            Track(
                                    clip = Clip.Video_1080x1920_30fps,
                                    effect = Effect.Effect3
                            )
                    ),
                    encoderCodecType = EncoderCodecType.h264,
                    isEnableDecoderFallback = true,
                    isSummaryVisible = true,
                    isTestCaseMode = true
            ),
            Settings(
                    trackList = listOf(
                            Track(
                                    clip = Clip.Video_1080x1920_30fps,
                                    effect = Effect.Effect1
                            ),
                            Track(
                                    clip = Clip.Video_1080x1920_30fps,
                                    effect = Effect.Effect2
                            ),
                            Track(
                                    clip = Clip.Video_1080x1920_30fps,
                                    effect = Effect.Effect3
                            ),
                            Track(
                                    clip = Clip.Video_1080x1920_30fps,
                                    effect = Effect.Effect4
                            )
                    ),
                    encoderCodecType = EncoderCodecType.h264,
                    isEnableDecoderFallback = true,
                    isSummaryVisible = true,
                    isTestCaseMode = true
            ),
            Settings(
                    trackList = listOf(
                            Track(
                                    clip = Clip.Video_1080x1920_30fps,
                                    effect = Effect.Effect1
                            ),
                            Track(
                                    clip = Clip.Video_1080x1920_30fps,
                                    effect = Effect.Effect2
                            ),
                            Track(
                                    clip = Clip.Video_1080x1920_30fps,
                                    effect = Effect.Effect3
                            ),
                            Track(
                                    clip = Clip.Video_1080x1920_30fps,
                                    effect = Effect.Effect4
                            ),
                            Track(
                                    clip = Clip.Video_1080x1920_30fps,
                                    effect = Effect.Effect1
                            ),
                            Track(
                                    clip = Clip.Video_1080x1920_30fps,
                                    effect = Effect.Effect2
                            )
                    ),
                    encoderCodecType = EncoderCodecType.h264,
                    isEnableDecoderFallback = true,
                    isSummaryVisible = true,
                    isTestCaseMode = true
            ),
            Settings(
                    trackList = listOf(
                            Track(
                                    clip = Clip.Video_3840x2160_30fps,
                                    effect = Effect.Effect1
                            ),
                            Track(
                                    clip = Clip.Video_3840x2160_30fps,
                                    effect = Effect.Effect2
                            )
                    ),
                    encoderCodecType = EncoderCodecType.h264,
                    isEnableDecoderFallback = true,
                    isSummaryVisible = true,
                    isTestCaseMode = true
            ),
            Settings(
                    trackList = listOf(
                            Track(
                                    clip = Clip.Video_3840x2160_30fps,
                                    effect = Effect.Effect1
                            ),
                            Track(
                                    clip = Clip.Video_3840x2160_30fps,
                                    effect = Effect.Effect3
                            ),
                            Track(
                                    clip = Clip.Video_1920x1080_120fps,
                                    effect = Effect.Effect2
                            ),
                            Track(
                                    clip = Clip.Video_1920x1080_120fps,
                                    effect = Effect.Effect4
                            )
                    ),
                    encoderCodecType = EncoderCodecType.h264,
                    isEnableDecoderFallback = true,
                    isSummaryVisible = true,
                    isTestCaseMode = true
            ),
            Settings(
                    trackList = listOf(
                            Track(
                                    clip = Clip.Video_3840x2160_30fps,
                                    effect = Effect.Effect1
                            ),
                            Track(
                                    clip = Clip.Video_1080x1920_30fps,
                                    effect = Effect.Effect2
                            ),
                            Track(
                                    clip = Clip.Video_3840x2160_60fps,
                                    effect = Effect.Effect3
                            ),
                            Track(
                                    clip = Clip.Video_1920x1080_120fps,
                                    effect = Effect.Effect4
                            )
                    ),
                    encoderCodecType = EncoderCodecType.h264,
                    isEnableDecoderFallback = true,
                    isSummaryVisible = true,
                    isTestCaseMode = true
            ),
            Settings(
                    trackList = listOf(
                            Track(
                                    clip = Clip.Video_1920x1080_120fps,
                                    effect = Effect.Effect1
                            )
                    ),
                    encoderCodecType = EncoderCodecType.h264,
                    isEnableDecoderFallback = true,
                    isSummaryVisible = true,
                    isTestCaseMode = true
            ),
            Settings(
                    trackList = listOf(
                            Track(
                                    clip = Clip.Video_1920x1080_120fps,
                                    effect = Effect.Effect1
                            ),
                            Track(
                                    clip = Clip.Video_1920x1080_120fps,
                                    effect = Effect.Effect2
                            )
                    ),
                    encoderCodecType = EncoderCodecType.h264,
                    isEnableDecoderFallback = true,
                    isSummaryVisible = true,
                    isTestCaseMode = true
            ),
            Settings(
                    trackList = listOf(
                            Track(
                                    clip = Clip.Video_1920x1080_120fps,
                                    effect = Effect.Effect1
                            ),
                            Track(
                                    clip = Clip.Video_1920x1080_120fps,
                                    effect = Effect.Effect2
                            ),
                            Track(
                                    clip = Clip.Video_1920x1080_120fps,
                                    effect = Effect.Effect3
                            ),
                            Track(
                                    clip = Clip.Video_1920x1080_120fps,
                                    effect = Effect.Effect4
                            )
                    ),
                    encoderCodecType = EncoderCodecType.h264,
                    isEnableDecoderFallback = true,
                    isSummaryVisible = true,
                    isTestCaseMode = true
            ),
            Settings(
                    trackList = listOf(
                            Track(
                                    clip = Clip.Video_3840x2160_30fps,
                                    effect = Effect.Effect1
                            )
                    ),
                    encoderCodecType = EncoderCodecType.h264,
                    isEnableDecoderFallback = true,
                    isSummaryVisible = true,
                    isTestCaseMode = true
            ),
            Settings(
                    trackList = listOf(
                            Track(
                                    clip = Clip.Video_3840x2160_60fps,
                                    effect = Effect.Effect1
                            )
                    ),
                    encoderCodecType = EncoderCodecType.h264,
                    isEnableDecoderFallback = true,
                    isSummaryVisible = true,
                    isTestCaseMode = true
            ),
            Settings(
                    trackList = listOf(
                            Track(
                                    clip = Clip.Video_3840x2160_60fps,
                                    effect = Effect.Effect1
                            ),
                            Track(
                                    clip = Clip.Video_3840x2160_60fps,
                                    effect = Effect.Effect2
                            )
                    ),
                    encoderCodecType = EncoderCodecType.h264,
                    isEnableDecoderFallback = true,
                    isSummaryVisible = true,
                    isTestCaseMode = true
            ),
            Settings(
                    trackList = listOf(
                            Track(
                                    clip = Clip.Video_3840x2160_30fps,
                                    effect = Effect.Effect1
                            ),
                            Track(
                                    clip = Clip.Video_1080x1920_30fps,
                                    effect = Effect.Effect2
                            )
                    ),
                    encoderCodecType = EncoderCodecType.h264,
                    isEnableDecoderFallback = true,
                    isSummaryVisible = true,
                    isTestCaseMode = true
            ),
            Settings(
                    trackList = listOf(
                            Track(
                                    clip = Clip.Video_3840x2160_60fps,
                                    effect = Effect.Effect1
                            ),
                            Track(
                                    clip = Clip.Video_1920x1080_120fps,
                                    effect = Effect.Effect2
                            )
                    ),
                    encoderCodecType = EncoderCodecType.h264,
                    isEnableDecoderFallback = true,
                    isSummaryVisible = true,
                    isTestCaseMode = true
            ),
            Settings(
                    trackList = listOf(
                            Track(
                                    clip = Clip.Video_3840x2160_30fps,
                                    effect = Effect.Effect1
                            ),
                            Track(
                                    clip = Clip.Video_1920x1080_120fps,
                                    effect = Effect.Effect2
                            )
                    ),
                    encoderCodecType = EncoderCodecType.h264,
                    isEnableDecoderFallback = true,
                    isSummaryVisible = true,
                    isTestCaseMode = true
            ),
            Settings(
                    trackList = listOf(
                            Track(
                                    clip = Clip.Video_1080x1920_30fps,
                                    effect = Effect.Effect1
                            ),
                            Track(
                                    clip = Clip.Video_3840x2160_60fps,
                                    effect = Effect.Effect2
                            )
                    ),
                    encoderCodecType = EncoderCodecType.h264,
                    isEnableDecoderFallback = true,
                    isSummaryVisible = true,
                    isTestCaseMode = true
            ),
            Settings(
                    trackList = listOf(
                            Track(
                                    clip = Clip.Video_1080x1920_30fps,
                                    effect = Effect.Effect1
                            ),
                            Track(
                                    clip = Clip.Video_1080x1920_30fps,
                                    effect = Effect.Effect2
                            ),
                            Track(
                                    clip = Clip.Video_3840x2160_60fps,
                                    effect = Effect.Effect3
                            ),
                            Track(
                                    clip = Clip.Video_3840x2160_60fps,
                                    effect = Effect.Effect4
                            )
                    ),
                    encoderCodecType = EncoderCodecType.h264,
                    isEnableDecoderFallback = true,
                    isSummaryVisible = true,
                    isTestCaseMode = true
            ),
            Settings(
                    trackList = listOf(
                            Track(
                                    clip = Clip.Video_3840x2160_60fps,
                                    effect = Effect.Effect1
                            ),
                            Track(
                                    clip = Clip.Video_3840x2160_60fps,
                                    effect = Effect.Effect2
                            ),
                            Track(
                                    clip = Clip.Video_1920x1080_120fps,
                                    effect = Effect.Effect3
                            ),
                            Track(
                                    clip = Clip.Video_1920x1080_120fps,
                                    effect = Effect.Effect4
                            )
                    ),
                    encoderCodecType = EncoderCodecType.h264,
                    isEnableDecoderFallback = true,
                    isSummaryVisible = true,
                    isTestCaseMode = true
            )
    )
}