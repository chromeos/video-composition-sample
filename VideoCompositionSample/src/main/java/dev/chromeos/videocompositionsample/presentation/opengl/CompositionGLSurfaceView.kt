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

package dev.chromeos.videocompositionsample.presentation.opengl

import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import dev.chromeos.videocompositionsample.presentation.media.MediaTrack
import dev.chromeos.videocompositionsample.presentation.media.encoder.utils.VideoMimeType

class CompositionGLSurfaceView
@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : GLSurfaceView(context, attrs) {

    companion object {
        const val SCREEN_RATIO = 16f / 9f
    }

    private var renderer: CompositionRenderer? = null

    init {
        setEGLContextClientVersion(3)
        preserveEGLContextOnPause = true
        setZOrderOnTop(false)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        holder.setFormat(PixelFormat.RGBA_8888)
    }

    fun init(onSurfaceReady: () -> Unit, onStopExport: () -> Unit, onEncodeError: (Exception) -> Unit) {
        renderer = CompositionRenderer(context, { onSurfaceReady.invoke() }, { onStopExport.invoke() }, { onEncodeError.invoke(it) })
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun setMediaTracks(mediaTrackList: List<MediaTrack>) {
        renderer?.initMediaTrackList(mediaTrackList)
    }

    fun playerStateChanged(index: Int, playWhenReady: Boolean) {
        queueEvent {
            renderer?.playerStateChanged(index, playWhenReady)
        }
    }

    fun play() {
        renderMode = RENDERMODE_CONTINUOUSLY
        queueEvent {
            renderer?.play()
        }
    }

    fun pause() {
        renderMode = RENDERMODE_WHEN_DIRTY
        queueEvent {
            renderer?.pause()
        }
    }

    fun setBeginTimePosition() {
        queueEvent {
            renderer?.setBeginTimePosition()
        }
    }

    fun seek(position: Long) {
        queueEvent {
            renderer?.seek(position)
            requestRender()
        }
    }

    fun setOpacity(trackIndex: Int, opacity: Float) {
        queueEvent {
            renderer?.setOpacity(trackIndex, opacity)
            if (renderMode == RENDERMODE_WHEN_DIRTY) {
                requestRender()
            }
        }
    }

    fun export(absoluteFilePath: String, width: Int, height: Int, videoMimeType: VideoMimeType, isTestCaseMode: Boolean) {
        renderMode = RENDERMODE_CONTINUOUSLY
        queueEvent {
            renderer?.export(absoluteFilePath, width, height, videoMimeType, isTestCaseMode)
        }
    }

    fun getTimePosition(): Long {
        return renderer?.timePosition ?: 0
    }

    fun getFpsAverageList(): List<Float> {
        return renderer?.fpsAverageList ?: emptyList()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val height = MeasureSpec.getSize(heightMeasureSpec)
        val width = (height * SCREEN_RATIO).toInt()
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        )
    }
}