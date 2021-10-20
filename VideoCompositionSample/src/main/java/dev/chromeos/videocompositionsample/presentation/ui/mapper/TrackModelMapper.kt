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

import dev.chromeos.videocompositionsample.presentation.ui.model.TrackModel
import dev.chromeos.videocompositionsample.domain.entity.Clip
import dev.chromeos.videocompositionsample.domain.entity.Effect
import dev.chromeos.videocompositionsample.domain.entity.Track
import dev.chromeos.videocompositionsample.presentation.R
import java.util.*

class TrackModelMapper {

    fun map(entity: Track): TrackModel {
        val nameResId = when (entity.clip) {
            Clip.Video_3840x2160_30fps -> R.string.settings__clip1
            Clip.Video_1080x1920_30fps -> R.string.settings__clip2
            Clip.Video_3840x2160_60fps -> R.string.settings__clip3
            Clip.Video_1920x1080_120fps -> R.string.settings__clip4
        }
        return TrackModel(
                uuid = UUID.randomUUID().toString(),
                clipId = entity.clip.clipId,
                nameResId = nameResId,
                effectId = entity.effect.effectId
        )
    }

    fun mapToModel(entityList: List<Track>): List<TrackModel> {
        return entityList.map { map(it) }
    }

    private fun map(model: TrackModel): Track {
        val clip = Clip.fromClipId(model.clipId) ?: throw IllegalArgumentException("Not implemented")
        val effect = Effect.fromEffectId(model.effectId) ?: throw IllegalArgumentException("Not implemented")
        return Track(clip = clip, effect = effect)
    }

    fun mapFromModel(modelList: List<TrackModel>): List<Track> {
        return modelList.map { map(it) }
    }
}