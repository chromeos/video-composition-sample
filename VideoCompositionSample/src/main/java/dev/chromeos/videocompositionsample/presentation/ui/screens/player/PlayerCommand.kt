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

package dev.chromeos.videocompositionsample.presentation.ui.screens.player

import dev.chromeos.videocompositionsample.presentation.ui.custom.ResolutionView

sealed class PlayerCommand {

    object Play : PlayerCommand()
    object Pause : PlayerCommand()
    object SkipForward : PlayerCommand()
    object SkipBackward : PlayerCommand()
    object LongSkipForward : PlayerCommand()
    object LongSkipBackward : PlayerCommand()
    object ToBegin : PlayerCommand()
    object ToEnd : PlayerCommand()
    data class Seek(val time: Long) : PlayerCommand()
    object ExactCurrentPositionSeek : PlayerCommand()
    data class SetOpacity(val trackIndex: Int, val opacity: Float) : PlayerCommand()
    data class Export(val resolution: ResolutionView.Resolution) : PlayerCommand()
}