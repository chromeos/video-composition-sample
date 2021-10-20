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

package dev.chromeos.videocompositionsample.presentation.media.mapper

import android.content.Context
import androidx.annotation.RawRes
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SeekParameters
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.RawResourceDataSource
import dev.chromeos.videocompositionsample.domain.entity.Track
import dev.chromeos.videocompositionsample.presentation.media.MediaHelper
import dev.chromeos.videocompositionsample.presentation.media.MediaTrack
import dev.chromeos.videocompositionsample.presentation.media.exoplayer.CustomRenderersFactory
import dev.chromeos.videocompositionsample.presentation.media.exoplayer.CustomSimpleExoPlayer
import javax.inject.Inject

class MediaTrackMapper
@Inject constructor(private val context: Context,
                    private val mediaHelper: MediaHelper
) {

    private fun map(track: Track, mediaCodecParams: MediaHelper.MediaCodecParams, isEnableDecoderFallback: Boolean): MediaTrack {

        val rawResId = mediaHelper.getRawResId(track.clip)
        val player = getPlayer(rawResId, mediaCodecParams, isEnableDecoderFallback)
        val trackParams = mediaHelper.getTrackParams(track)

        return MediaTrack(
                player = player,
                position = trackParams.position,
                duration = trackParams.duration,
                startVideoFrom = trackParams.startVideoFrom,
                videoDuration = trackParams.videoDuration,
                hasSepiaPositionAnimation = trackParams.hasSepiaPositionAnimation,
                effects = trackParams.effects
        )
    }

    fun map(trackList: List<Track>, isEnableDecoderFallback: Boolean, isExport: Boolean): List<MediaTrack> {
        val optimalMediaCodecParams = mediaHelper.getOptimalMediaCodecParams(trackList, isExport)
        return trackList.mapIndexed { index: Int, track: Track ->
            map(track, optimalMediaCodecParams[index], isEnableDecoderFallback)
        }
    }

    private fun getPlayer(@RawRes rawResId: Int, mediaCodecParams: MediaHelper.MediaCodecParams, isEnableDecoderFallback: Boolean): CustomSimpleExoPlayer {
        val trackSelector = DefaultTrackSelector(context)
        trackSelector.experimental_allowMultipleAdaptiveSelections()
        trackSelector.setParameters(
                DefaultTrackSelector.ParametersBuilder(context)
                        .setTunnelingAudioSessionId(C.generateAudioSessionIdV21(context))
        )

        val renderersFactory = CustomRenderersFactory(
                context = context,
                priority = mediaCodecParams.priority,
                frameRate = mediaCodecParams.frameRate,
                operatingFrameRate = mediaCodecParams.operatingRate
        )
                .apply {
                    //     setExtensionRendererMode(EXTENSION_RENDERER_MODE_ON)
                    setEnableDecoderFallback(isEnableDecoderFallback)
                }

        val dataSpec = DataSpec(RawResourceDataSource.buildRawResourceUri(rawResId))
        val rawResourceDataSource = RawResourceDataSource(context)
                .apply { open(dataSpec) }

        val dataSourceFactory = DataSource.Factory { rawResourceDataSource }

        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(rawResourceDataSource.uri)

        val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(1500, 5000, 1500, 1500)
                .setBackBuffer(0, false)
                .createDefaultLoadControl()

        return CustomSimpleExoPlayer.Builder(context, renderersFactory)
                //     .setTrackSelector(trackSelector)
                     .setLoadControl(loadControl)
                .build()
                .apply {
                    prepare(mediaSource)
                    setSeekParameters(SeekParameters.CLOSEST_SYNC)
                    repeatMode = Player.REPEAT_MODE_OFF
                }
    }
}