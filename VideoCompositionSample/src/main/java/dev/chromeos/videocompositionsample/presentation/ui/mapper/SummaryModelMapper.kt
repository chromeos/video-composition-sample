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

package dev.chromeos.videocompositionsample.presentation.ui.mapper

import dev.chromeos.videocompositionsample.presentation.ui.model.SummaryModel
import dev.chromeos.videocompositionsample.presentation.ui.model.TrackModel

class SummaryModelMapper {

    private fun map(trackModel: TrackModel): SummaryModel {

        return SummaryModel(
                nameResId = trackModel.nameResId,
                effectId = trackModel.effectId,
                isLoadSuccess = true,
                fpsAverage = 0f
        )
    }

    fun map(trackModelList: List<TrackModel>): List<SummaryModel> {
        return trackModelList.map { map(it) }
    }
}