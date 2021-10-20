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

package dev.chromeos.videocompositionsample.presentation.media.encoder

import android.content.Context
import android.media.MediaMuxer
import android.opengl.EGLContext
import android.os.Handler
import android.os.Looper
import dev.chromeos.videocompositionsample.presentation.media.encoder.utils.VideoMimeType
import dev.chromeos.videocompositionsample.presentation.opengl.EncoderFrameData
import dev.chromeos.videocompositionsample.presentation.opengl.GlMediaData
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class MediaEncoder(private val onStopped: () -> Unit) {

    companion object {
        private const val OUTPUT_FORMAT = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
    }

    enum class MediaType(val muxerChannelsCount: Int) {
        VideoAndAudio(2),
        OnlyVideo(1)
    }

    private lateinit var muxer: Muxer
    private var onError: ((e: Exception) -> Unit)? = null
    private val videoRecorder = VideoEncoder()
    private val audioRecorder = AudioEncoder()

    private var mediaType = MediaType.VideoAndAudio

    private var isRecording = false

    private var mainThreadHandler: Handler = Handler(Looper.getMainLooper())

    fun start(
            context: Context,
            eglContext: EGLContext,
            outputFile: File,
            width: Int,
            height: Int,
            videoMimeType: VideoMimeType,
            encodeSpeedFactor: Float,
            glMediaData: GlMediaData,
            onError: (e: Exception) -> Unit
    ) {
        if (isRecording) return

        try {
            this.onError = onError
            muxer = Muxer(outputFile, OUTPUT_FORMAT, mediaType.muxerChannelsCount)
            isRecording = true

            videoRecorder.startRecording(context, eglContext, width, height, videoMimeType, encodeSpeedFactor, glMediaData, muxer, onError)
            if (mediaType == MediaType.VideoAndAudio) {
                audioRecorder.startRecording(glMediaData, muxer, encodeSpeedFactor, onError)
            }

        } catch (e: IOException) {
            stop()
            throw IllegalStateException("Error start Recorder", e)
        }
    }

    fun notifyStartMediaTrack(index: Int) {
        if (mediaType == MediaType.VideoAndAudio) {
            audioRecorder.notifyStartMediaTrack(index)
        }
    }

    fun notifyPauseMediaTrack(index: Int) {
        if (mediaType == MediaType.VideoAndAudio) {
            audioRecorder.notifyPauseMediaTrack(index)
        }
    }

    fun frameAvailable(encoderFrameData: EncoderFrameData) {
        videoRecorder.frameAvailable(encoderFrameData)
    }

    fun updateSharedContext(width: Int, height: Int, sharedContext: EGLContext) {
        videoRecorder.updateSharedContext(width, height, sharedContext)
    }

    fun stop() {
        videoRecorder.stopRecording()
        if (mediaType == MediaType.VideoAndAudio) {
            audioRecorder.stopRecording()
        }
        releaseMuxer()
    }

    private fun releaseMuxer() {
        Observable.interval(50, TimeUnit.MILLISECONDS)
                .take(10)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                    if (isRecording && !videoRecorder.isRecording && !audioRecorder.isRecording) {
                        muxer.release()
                        isRecording = false
                    }
                }
                .doFinally {
                    if (isRecording) {
                        try {
                            muxer.release()
                        } catch (e: Exception) {
                            mainThreadHandler.post {
                                onError?.invoke(e)
                            }
                        } finally {
                            isRecording = false
                        }
                    }
                    onStopped.invoke()
                }
                .subscribe()
    }

}