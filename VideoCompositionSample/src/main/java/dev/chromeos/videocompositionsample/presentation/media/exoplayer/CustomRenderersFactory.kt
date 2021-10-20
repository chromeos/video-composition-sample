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
import android.os.Handler
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.audio.*
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.util.Log
import com.google.android.exoplayer2.video.VideoRendererEventListener
import java.nio.ByteBuffer

class CustomRenderersFactory(context: Context,
                             private val priority: Int,
                             private val frameRate: Int?,
                             private val operatingFrameRate: Int?) : DefaultRenderersFactory(context) {

    companion object {
        const val TAG = "CustomRenderersFactory"
    }

    var onAudioQueueInputListener: ((byteBuffer: ByteBuffer, sampleRateHz: Int, channelCount: Int) -> Unit)? = null

    private fun onAudioInputBuffer(byteBuffer: ByteBuffer, sampleRateHz: Int, channelCount: Int) {
        onAudioQueueInputListener?.invoke(byteBuffer, sampleRateHz, channelCount)
    }

    override fun buildVideoRenderers(
            context: Context,
            @ExtensionRendererMode extensionRendererMode: Int,
            mediaCodecSelector: MediaCodecSelector,
            drmSessionManager: DrmSessionManager<FrameworkMediaCrypto>?,
            playClearSamplesWithoutKeys: Boolean,
            enableDecoderFallback: Boolean,
            eventHandler: Handler,
            eventListener: VideoRendererEventListener,
            allowedVideoJoiningTimeMs: Long,
            out: ArrayList<Renderer>
    ) {

        out.add(
                CustomMediaCodecVideoRenderer(
                        context,
                        mediaCodecSelector,
                        allowedVideoJoiningTimeMs,
                        drmSessionManager,
                        playClearSamplesWithoutKeys,
                        enableDecoderFallback,
                        eventHandler,
                        eventListener,
                        MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY,
                        priority,
                        frameRate,
                        operatingFrameRate
                )
        )

        if (extensionRendererMode == EXTENSION_RENDERER_MODE_OFF) {
            return
        }
        var extensionRendererIndex = out.size
        if (extensionRendererMode == EXTENSION_RENDERER_MODE_PREFER) {
            extensionRendererIndex--
        }

        try {
            // Full class names used for constructor args so the LINT rule triggers if any of them move.
            // LINT.IfChange
            val clazz = Class.forName("com.google.android.exoplayer2.ext.vp9.LibvpxVideoRenderer")
            val constructor = clazz.getConstructor(
                    Long::class.javaPrimitiveType,
                    Handler::class.java,
                    VideoRendererEventListener::class.java,
                    Int::class.javaPrimitiveType)
            // LINT.ThenChange(../../../../../../../proguard-rules.txt)
            val renderer = constructor.newInstance(
                    allowedVideoJoiningTimeMs,
                    eventHandler,
                    eventListener,
                    MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY) as Renderer
            out.add(extensionRendererIndex++, renderer)
            Log.i(TAG, "Loaded LibvpxVideoRenderer.")
        } catch (e: ClassNotFoundException) {
            // Expected if the app was built without the extension.
        } catch (e: Exception) {
            // The extension is present, but instantiation failed.
            throw RuntimeException("Error instantiating VP9 extension", e)
        }

        try {
            // Full class names used for constructor args so the LINT rule triggers if any of them move.
            // LINT.IfChange
            val clazz = Class.forName("com.google.android.exoplayer2.ext.av1.Libgav1VideoRenderer")
            val constructor = clazz.getConstructor(
                    Long::class.javaPrimitiveType,
                    Handler::class.java,
                    VideoRendererEventListener::class.java,
                    Int::class.javaPrimitiveType)
            // LINT.ThenChange(../../../../../../../proguard-rules.txt)
            val renderer = constructor.newInstance(
                    allowedVideoJoiningTimeMs,
                    eventHandler,
                    eventListener,
                    MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY) as Renderer
            out.add(extensionRendererIndex, renderer)
            Log.i(TAG, "Loaded Libgav1VideoRenderer.")
        } catch (e: ClassNotFoundException) {
            // Expected if the app was built without the extension.
        } catch (e: Exception) {
            // The extension is present, but instantiation failed.
            throw RuntimeException("Error instantiating AV1 extension", e)
        }
    }

    private val teeAudioProcessor: CustomTeeAudioProcessor by lazy {
        CustomTeeAudioProcessor { buffer, sampleRateHz, channelCount ->
            onAudioInputBuffer(buffer, sampleRateHz, channelCount)
        }
    }

    override fun buildAudioRenderers(
            context: Context,
            @ExtensionRendererMode extensionRendererMode: Int,
            mediaCodecSelector: MediaCodecSelector,
            drmSessionManager: DrmSessionManager<FrameworkMediaCrypto>?,
            playClearSamplesWithoutKeys: Boolean,
            enableDecoderFallback: Boolean,
            audioProcessors: Array<out AudioProcessor>,
            eventHandler: Handler,
            eventListener: AudioRendererEventListener,
            out: ArrayList<Renderer>
    ) {

        val audioProcessorList = audioProcessors.toMutableList()
                .apply { add(teeAudioProcessor) }
                .toTypedArray()

        out.add(
                MediaCodecAudioRenderer(
                        context,
                        mediaCodecSelector,
                        drmSessionManager,
                        playClearSamplesWithoutKeys,
                        enableDecoderFallback,
                        eventHandler,
                        eventListener,
                        DefaultAudioSink(AudioCapabilities.getCapabilities(context), audioProcessorList)
                )
        )
    }
}