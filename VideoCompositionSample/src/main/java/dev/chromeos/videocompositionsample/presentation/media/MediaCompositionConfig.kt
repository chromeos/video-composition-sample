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

package dev.chromeos.videocompositionsample.presentation.media

object MediaCompositionConfig {

    private val effects1: Array<TrackEffect> = arrayOf(
            TrackEffect(0, 1f, 1f, 0.4f, -45f, 0f, -0.5f, 0.5f),
            TrackEffect(15000, 0.5f, 0.5f, 0.6f, 0f, 0f, 0f, 0f),
            TrackEffect(25000, 1f, 0.5f, 0.3f, 45f, 0f, 0.5f, -0.5f),
            TrackEffect(30000, 1f, 1f, 0.3f, 45f, 0f, 0.5f, -0.5f)
    )

    private val effects2: Array<TrackEffect> = arrayOf(
            TrackEffect(5000, 0.75f, 0f, 0.45f, 0f, 1f, 0.5f, 0.5f),
            TrackEffect(10000, 0.85f, 0.5f, 0.5f, -10f, 0f, 0f, 0f),
            TrackEffect(20000, 1f, 0f, 0.2f, 10f, 1f, -0.5f, -0.5f)
    )

    private val effects3: Array<TrackEffect> = arrayOf(
            TrackEffect(10000, 1f, 0.25f, 0.35f, 15f, 0f, 0.7f, -0.5f),
            TrackEffect(25000, 1f, 0.25f, 0.35f, -15f, 0f, 0.7f, 0.5f)
    )

    private val effects4: Array<TrackEffect> = arrayOf(
            TrackEffect(5000, 1f, 0.5f, 0.25f, 0f, 0f, -0.5f, 0f),
            TrackEffect(10000, 1f, 0.5f, 0.25f, 0f, 0f, -0.5f, 0f),
            TrackEffect(15000, 0.5f, 0f, 0.5f, 180f, 0f, 0f, 0f),
            TrackEffect(20000, 1f, 0.5f, 0.25f, 360f, 0f, -0.5f, 0f),
            TrackEffect(30000, 1f, 0.5f, 0.25f, 360f, 0f, -0.5f, 0f)
    )

    val trackParamsList: List<TrackParams> = listOf(
            TrackParams(0, 30000, 0, 30000, false, effects1),
            TrackParams(5000, 15000, 1000, 15000, false, effects2),
            TrackParams(10000, 15000, 0, 30000, false, effects3),
            TrackParams(5000, 20000, 5000, 5000, true, effects4)
    )
}