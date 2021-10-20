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

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import dev.chromeos.videocompositionsample.presentation.R
import dev.chromeos.videocompositionsample.presentation.media.exoplayer.CustomMediaCodecVideoRenderer.Companion.BEST_EFFORT_PRIORITY
import dev.chromeos.videocompositionsample.presentation.media.exoplayer.CustomMediaCodecVideoRenderer.Companion.REAL_TIME_PRIORITY
import dev.chromeos.videocompositionsample.domain.entity.Clip
import dev.chromeos.videocompositionsample.domain.entity.Effect
import dev.chromeos.videocompositionsample.domain.entity.Track
import java.io.IOException
import javax.inject.Inject

class MediaHelper @Inject constructor(private val context: Context) {

    fun getOptimalMediaCodecParams(trackList: List<Track>, isExport: Boolean): List<MediaCodecParams> {
        val playTrackList = trackList
                .map { track ->
                    val trackParams = getTrackParams(track)
                    PlayTrack(
                            rawResId = getRawResId(track.clip),
                            playSpeed = trackParams.videoDuration.toFloat() / trackParams.duration.toFloat()
                    )
                }
        val videoInfoList = playTrackList.map { getVideoInfo(it) }
        val weightList = videoInfoList
                .map {
                    val sizeWeight = if (it.width > 3000 || it.height > 3000) {
                        5
                    } else if (it.width > 1600 || it.height > 1600) {
                        1
                    } else {
                        0
                    }
                    val speedWeight = when {
                        it.playSpeed > 1.1f -> {
                            4
                        }
                        it.playSpeed < 0.9f -> {
                            1
                        }
                        else -> {
                            0
                        }
                    }
                    val frameRateWeight = when {
                        it.frameRate > 100 -> {
                            3
                        }
                        it.frameRate > 40 -> {
                            2
                        }
                        else -> {
                            0
                        }
                    }
                    sizeWeight + speedWeight + frameRateWeight
                }
        val mostHeavyIndex = weightList.indexOf(weightList.max())

        // only one Player can has RealTimePriority during encoding otherwise possible codec exception
        return playTrackList.mapIndexed { index: Int, _: PlayTrack ->
            val operatingRate = if (Build.MANUFACTURER == "blackshark" && playTrackList.size == 1) { //TODO investigate this strange behavior
                null
            } else {
                -1
            }
            val priority = if (isExport) {
                BEST_EFFORT_PRIORITY
            } else {
                REAL_TIME_PRIORITY
            }
            if (index == mostHeavyIndex) {
                MediaCodecParams(
                        priority = REAL_TIME_PRIORITY,
                        frameRate = -1,
                        operatingRate = operatingRate
                )
            } else {
                MediaCodecParams(
                        priority = priority,
                        frameRate = -1,
                        operatingRate = -1
                )
            }
        }
    }

    fun getTrackParams(track: Track): TrackParams {
        return when (track.effect) {
            Effect.Effect1 -> MediaCompositionConfig.trackParamsList[0]
            Effect.Effect2 -> MediaCompositionConfig.trackParamsList[1]
            Effect.Effect3 -> MediaCompositionConfig.trackParamsList[2]
            Effect.Effect4 -> MediaCompositionConfig.trackParamsList[3]
        }
    }

    fun getRawResId(clip: Clip): Int {
        return when (clip) {
            Clip.Video_3840x2160_30fps -> R.raw.video_3840x2160_30fps
            Clip.Video_1080x1920_30fps -> R.raw.video_1080x1920_30fps
            Clip.Video_3840x2160_60fps -> R.raw.video_3840x2160_60fps
            Clip.Video_1920x1080_120fps -> R.raw.video_1920x1080_120fps
        }
    }

    /**
     * Note: this assumes that video files have a framerate set
     */
    private fun getVideoInfo(playTrack: PlayTrack): VideoInfo {
        val mediaExtractor = MediaExtractor()
        val assetFileDescriptor: AssetFileDescriptor = context.resources.openRawResourceFd(playTrack.rawResId)
        return try {
            mediaExtractor.setDataSource(assetFileDescriptor)
            val format = mediaExtractor.getTrackFormat(0)
            val frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE)
            val width = format.getInteger(MediaFormat.KEY_WIDTH)
            val height = format.getInteger(MediaFormat.KEY_HEIGHT)
            VideoInfo(
                    width = width,
                    height = height,
                    frameRate = frameRate,
                    playSpeed = playTrack.playSpeed
            )
        } catch (e: IOException) {
            throw IllegalArgumentException("Can't find video file")
        } finally {
            assetFileDescriptor.close()
            mediaExtractor.release()
        }
    }

    inner class PlayTrack(
            val rawResId: Int,
            val playSpeed: Float
    )

    inner class VideoInfo(
            val width: Int,
            val height: Int,
            val frameRate: Int,
            val playSpeed: Float
    )

    data class MediaCodecParams(
            val priority: Int,
            val frameRate: Int?,
            val operatingRate: Int?
    )
}

