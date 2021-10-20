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

package dev.chromeos.videocompositionsample.presentation.ui.screens.player.core

import dev.chromeos.videocompositionsample.presentation.opengl.CompositionGLSurfaceView
import dev.chromeos.videocompositionsample.presentation.tools.info.MessageModel
import dev.chromeos.videocompositionsample.presentation.ui.base.IBaseView
import dev.chromeos.videocompositionsample.presentation.ui.custom.CustomSliderView
import dev.chromeos.videocompositionsample.presentation.ui.custom.PlayerControlView
import dev.chromeos.videocompositionsample.presentation.ui.custom.ResolutionView
import dev.chromeos.videocompositionsample.domain.entity.Settings

interface IPlayerView : IBaseView {

    fun onGetSettings(settings: Settings)

    fun toggleControlPanel()

    fun hideControlPanel()

    fun showHideSummary(settings: Settings)

    fun onPlayersError(notReadyMediaTrackIds: MutableSet<Int>)

    fun onPlayerPlay()

    fun onPlayerPause()

    fun onPlayerSeek()

    fun onPlayerExport()

    fun onExportSuccessFinished(messageModel: MessageModel)

    fun onTimeLineChanged(value: Float)

    fun getGLSurfaceView(): CompositionGLSurfaceView?

    fun getSliderTime(): CustomSliderView?

    fun getPlayerControlView(): PlayerControlView?

    fun getResolutionView(): ResolutionView?

    fun showFpsAverageList(fpsAverageList: List<Float>)

    fun onTestCasesFinished()

    fun toShare(absoluteFilePath: String)
}