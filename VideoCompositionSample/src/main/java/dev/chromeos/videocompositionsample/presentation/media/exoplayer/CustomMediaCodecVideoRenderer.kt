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

package dev.chromeos.videocompositionsample.presentation.media.exoplayer

import android.content.Context
import android.media.MediaFormat
import android.os.Handler
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.video.MediaCodecVideoRenderer
import com.google.android.exoplayer2.video.VideoRendererEventListener

@Suppress("DEPRECATION")
class CustomMediaCodecVideoRenderer @JvmOverloads constructor(
        context: Context,
        mediaCodecSelector: MediaCodecSelector,
        allowedJoiningTimeMs: Long,
        drmSessionManager: DrmSessionManager<FrameworkMediaCrypto>? = null,
        playClearSamplesWithoutKeys: Boolean = false,
        enableDecoderFallback: Boolean = false,
        eventHandler: Handler? = null,
        eventListener: VideoRendererEventListener? = null,
        maxDroppedFramesToNotify: Int,
        private val priority: Int,
        private val frameRate: Int?,
        private val operatingFrameRate: Int?
) : MediaCodecVideoRenderer(
        context,
        mediaCodecSelector,
        allowedJoiningTimeMs,
        drmSessionManager,
        playClearSamplesWithoutKeys,
        enableDecoderFallback,
        eventHandler,
        eventListener,
        maxDroppedFramesToNotify) {

    companion object {
        const val REAL_TIME_PRIORITY = 0
        const val BEST_EFFORT_PRIORITY = 1
    }

    override fun getMediaFormat(
            format: Format,
            codecMimeType: String,
            codecMaxValues: CodecMaxValues,
            codecOperatingRate: Float,
            deviceNeedsNoPostProcessWorkaround: Boolean,
            tunnelingAudioSessionId: Int
    ): MediaFormat {

        return super.getMediaFormat(
                format,
                codecMimeType,
                codecMaxValues,
                codecOperatingRate,
                deviceNeedsNoPostProcessWorkaround,
                tunnelingAudioSessionId
        ).apply {

            setInteger(MediaFormat.KEY_PRIORITY, priority)
            frameRate?.let { setInteger(MediaFormat.KEY_FRAME_RATE, it) }
            operatingFrameRate?.let { setInteger(MediaFormat.KEY_OPERATING_RATE, it) }
            // Log.d("----> ", "width=${format.width} height=${format.height} frameRate=${format.frameRate} (priority=$priority frameRate=$frameRate operatingFrameRate=$operatingFrameRate)")
            // setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0)
        }
    }
}