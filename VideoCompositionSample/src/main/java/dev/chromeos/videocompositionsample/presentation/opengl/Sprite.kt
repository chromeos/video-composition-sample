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

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.Matrix
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import com.google.android.exoplayer2.PlaybackParameters
import dev.chromeos.videocompositionsample.presentation.media.MediaTrack
import dev.chromeos.videocompositionsample.presentation.media.TrackEffect
import dev.chromeos.videocompositionsample.presentation.opengl.Plane.Companion.NUMBER_OF_INDICES
import dev.chromeos.videocompositionsample.presentation.opengl.Plane.Companion.STARTING_INDEX
import dev.chromeos.videocompositionsample.presentation.opengl.Plane.Companion.TRIANGLE_VERTICES_DATA_POS_OFFSET
import dev.chromeos.videocompositionsample.presentation.opengl.Plane.Companion.TRIANGLE_VERTICES_DATA_STRIDE_BYTES
import dev.chromeos.videocompositionsample.presentation.opengl.Plane.Companion.TRIANGLE_VERTICES_DATA_UV_OFFSET
import dev.chromeos.videocompositionsample.presentation.ui.screens.player.core.PlayerPresenter.Companion.DURATION_MILLIS
import kotlin.math.max
import kotlin.math.min

class Sprite(
        private val width: Int,
        private val height: Int,
        private val programId: Int,
        private val textureId: Int,
        private val mediaTrack: MediaTrack,
        val onActiveExportStateChangeListener: (Int, Boolean) -> Unit
) {

    companion object {
        private const val TEST_TAG = "Composition TestCases ---->"
        const val GL_TEXTURE_EXTERNAL_OES = GLES11Ext.GL_TEXTURE_EXTERNAL_OES
    }

    enum class PlayerState {
        Play,
        Pause,
        Seek,
        Export
    }

    private var mainThreadHandler: Handler = Handler(Looper.getMainLooper())

    var isPlayWhenReady: Boolean = false
        set(value) {
            if (!isFinished || playerState != PlayerState.Export || playerState != PlayerState.Play) {
                mainThreadHandler.post {
                    mediaTrack.player?.playWhenReady = value
                }
                field = value
            }

            if (value && playerState == PlayerState.Export && !isFinished) {
                // TODO should create trackId in model and remove dependence of index in all code
                onActiveExportStateChangeListener.invoke(textureId - 1, true)
            }

            if (!value) {
                isFinished = true
                if (playerState == PlayerState.Export) {
                    // TODO should create trackId in model and remove dependence of index in all code
                    fpsAverage = fpsSum / frameCount
                    onActiveExportStateChangeListener.invoke(textureId - 1, false)
                }
            }
        }

    @Volatile
    private var isNewFrameAvailable = false

    var fpsAverage = 0f
    private var fpsSum = 0f
    private var frameCount = 0f
    private var fpsMin = 0f
    private var fpsMax = 0f

    private val playSpeed: Float by lazy { mediaTrack.videoDuration.toFloat() / mediaTrack.duration.toFloat() }
    private var playerState = PlayerState.Pause
    var systemStartPlayTime = 0L
    var spriteTimePosition = 0L
    private var volume = 1f
    private var isFinished = false
    private var isStopped = true

    private val plane = Plane()
    private val surfaceTexture: SurfaceTexture by lazy { SurfaceTexture(textureId) }

    private val positionsHandle: Int by lazy { GLES30.glGetAttribLocation(programId, "a_Position") }
    private val textureHandle: Int by lazy { GLES30.glGetAttribLocation(programId, "a_TexCoordinate") }

    private val mvpMatrixHandle: Int by lazy { GLES30.glGetUniformLocation(programId, "uMVPMatrix") }
    private val transformMatrixHandle: Int by lazy { GLES30.glGetUniformLocation(programId, "uTransformMatrix") }

    private val opacityHandle: Int by lazy { GLES30.glGetUniformLocation(programId, "opacity") }
    private val sepiaHandle: Int by lazy { GLES30.glGetUniformLocation(programId, "sepia") }
    private val sepiaCenterPositionHandle: Int by lazy { GLES30.glGetUniformLocation(programId, "sepiaCenterPosition") }

    private val vpMatrix = FloatArray(16)

    var videoScaleFactor = 0f
    var videoTranslateFactor = 0f
    private var videoHeight = 0f

    private var preTime = 0L

    init {
        init()
    }

    fun init() {
        GLES30.glViewport(0, 0, width, height)

        val vMatrix = FloatArray(16)
        val pMatrix = FloatArray(16)

        val near = 1f
        val far = 100f
        val eyeZ = 1f
        Matrix.setLookAtM(vMatrix, 0,
                0.0f, 0.0f, eyeZ,
                0.0f, 0.0f, 0.0f,
                0.0f, 0.1f, 0.0f)

        val ratio = width.toFloat() / height.toFloat()
        Matrix.frustumM(pMatrix, 0, -ratio, ratio, -1.0f, 1.0f, near, far)
        Matrix.multiplyMM(vpMatrix, 0, pMatrix, 0, vMatrix, 0)

        GLES30.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES30.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST.toFloat())
        GLES30.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR.toFloat())

        GLES30.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE.toFloat())
        GLES30.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE.toFloat())

        val surface = Surface(surfaceTexture)
        surfaceTexture.setOnFrameAvailableListener {
            synchronized(this) {
                isNewFrameAvailable = true
            }
        }

        mainThreadHandler.post {
            mediaTrack.player?.setPlaybackParameters(PlaybackParameters(playSpeed))
            mediaTrack.player?.setVideoSurface(surface)
            mediaTrack.player?.setVideoFrameMetadataListener { presentationTimeUs, releaseTimeNs, format, mediaFormat ->
                videoScaleFactor = format.width.toFloat() / format.height.toFloat()
                videoTranslateFactor = if (format.width > format.height) {
                    videoScaleFactor
                } else {
                    format.height.toFloat() / format.width.toFloat()
                }
                videoHeight = format.height.toFloat()
            }
        }
    }

    fun setBeginTimePosition() {
        spriteTimePosition = 0L
    }

    fun seek(timePosition: Long) {
        playerState = PlayerState.Seek
        val position = max(0, ((timePosition - mediaTrack.position) * playSpeed + mediaTrack.startVideoFrom).toLong())
        mainThreadHandler.post {
            mediaTrack.player?.seekTo(position)
        }
        spriteTimePosition = timePosition
    }

    fun play() {
        isFinished = false
        preStartPrepareTimeShift = 100
        systemStartPlayTime = System.currentTimeMillis() - spriteTimePosition
        playerState = PlayerState.Play
        fpsSum = 0f
        frameCount = 0f
        fpsMin = 0f
        fpsMax = 0f
        isStopped = false
    }

    fun pause() {
        if (playerState == PlayerState.Export || playerState == PlayerState.Play) {
            Log.d(TEST_TAG, "Clip${textureId}:  min fps=${fpsMin};  max fps=${fpsMax};  average fps=${fpsSum/frameCount}")
        }
        preStartPrepareTimeShift = 0L
        if (playerState == PlayerState.Export) {
            preStartPrepareTimeShift = 0L
            mainThreadHandler.post {
                mediaTrack.player?.setPlaybackParameters(PlaybackParameters(playSpeed))
                mediaTrack.player?.audioComponent?.volume = 1f
            }
        }
        fpsAverage = fpsSum / frameCount
        playerState = PlayerState.Pause
        isStopped = true
    }

    private var encoderSpeedFactor = 1.0f
    private var preStartPrepareTimeShift = 0L

    fun export(encoderSpeedFactor: Float) {
        isFinished = false
        this.encoderSpeedFactor = encoderSpeedFactor
        mainThreadHandler.post {
            val adaptivePlaySpeed = if (playSpeed < 0.5f) { // TODO should be investigated Exoplayer low speed param
                playSpeed * 1.2f
            } else {
                playSpeed
            }
            mediaTrack.player?.setPlaybackParameters(PlaybackParameters(encoderSpeedFactor * adaptivePlaySpeed))
            mediaTrack.player?.audioComponent?.volume = 0f
        }
        spriteTimePosition = 0
        systemStartPlayTime = System.currentTimeMillis()
        playerState = PlayerState.Export
        fpsSum = 0f
        frameCount = 0f
        preStartPrepareTimeShift = 100
        isStopped = false
    }

    private val isRendering: Boolean
        get() {
            return if (isFinished && (playerState == PlayerState.Play || playerState == PlayerState.Export)) {
                false
            } else if (spriteTimePosition < mediaTrack.position - preStartPrepareTimeShift
                    || spriteTimePosition > mediaTrack.position + mediaTrack.duration) {
                if (isPlayWhenReady && playerState != PlayerState.Seek) {
                    isPlayWhenReady = false
                }
                false
            } else {
                if (!isPlayWhenReady && playerState != PlayerState.Seek) {
                    if (playerState != PlayerState.Export) {
                        mainThreadHandler.post {
                            mediaTrack.player?.volume = volume
                        }
                    }
                    fpsSum = 0f
                    frameCount = 0f
                    if (!isStopped) {
                        isPlayWhenReady = true
                    }
                } else {
                    mainThreadHandler.post {
                        if (playerState != PlayerState.Export) {
                            mediaTrack.player?.volume = volume
                        }
                    }
                }
                true
            }
        }

    fun drawClip(alpha: Float) {

        if (playerState == PlayerState.Play || playerState == PlayerState.Export) {
            spriteTimePosition = if (playerState == PlayerState.Play) {
                System.currentTimeMillis() - systemStartPlayTime
            } else {
                (encoderSpeedFactor * (System.currentTimeMillis() - systemStartPlayTime)).toLong()
            }
            if (spriteTimePosition > DURATION_MILLIS) {
                spriteTimePosition = DURATION_MILLIS
                fpsAverage = fpsSum / frameCount

            }
        }

        val frameData = getFrameData()
        volume = frameData.volume

        if (!isRendering
                && spriteTimePosition != DURATION_MILLIS || spriteTimePosition > mediaTrack.position + mediaTrack.duration) return

        synchronized(this) {
            if (isNewFrameAvailable) {

                val curTime = System.currentTimeMillis()
                val fps = 1000f / (curTime - preTime)

                fpsMin = if (fpsMin < 1f) {
                    fps
                } else {
                    min(fpsMin, fps)
                }

                fpsMax = if (fpsMax < 1f) {
                    fps
                } else {
                    max(fpsMax, fps)
                }

                fpsSum += fps
                frameCount++
                preTime = curTime

                surfaceTexture.updateTexImage()
                isNewFrameAvailable = false
            }
        }

        if (playerState != PlayerState.Export) {
            GLES30.glDisable(GLES30.GL_DITHER)
            GLES30.glDisable(GLES30.GL_DEPTH_TEST)
            GLES30.glEnable(GLES30.GL_BLEND)
            GLES30.glBlendEquation(GLES30.GL_FUNC_ADD)
            GLES30.glBlendFuncSeparate(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA, GLES30.GL_ONE, GLES30.GL_ONE)

            GLES30.glUseProgram(programId)
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textureId)

            plane.positionBuffer.position(TRIANGLE_VERTICES_DATA_POS_OFFSET)
            GLES30.glVertexAttribPointer(positionsHandle, 3, GLES30.GL_FLOAT, false, TRIANGLE_VERTICES_DATA_STRIDE_BYTES, plane.positionBuffer)
            GLES30.glEnableVertexAttribArray(positionsHandle)

            plane.positionBuffer.position(TRIANGLE_VERTICES_DATA_UV_OFFSET)
            GLES30.glVertexAttribPointer(textureHandle, 2, GLES30.GL_FLOAT, false, TRIANGLE_VERTICES_DATA_STRIDE_BYTES, plane.positionBuffer)
            GLES30.glEnableVertexAttribArray(textureHandle)

            GLES30.glUniform1f(opacityHandle, frameData.opacity * alpha)
            GLES30.glUniform1f(sepiaHandle, frameData.sepia)
            GLES30.glUniform1f(sepiaCenterPositionHandle, frameData.sepiaCenterPosition)

            GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, vpMatrix, 0)
            GLES30.glUniformMatrix4fv(transformMatrixHandle, 1, false, frameData.transformMatrix, 0)

            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, STARTING_INDEX, NUMBER_OF_INDICES)

            GLES30.glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0)
            GLES30.glUseProgram(0)
            GLES30.glFinish()
        }
    }

    private fun getFrameData(): FrameData {
        val transformMatrix = FloatArray(16)
        Matrix.setIdentityM(transformMatrix, 0)

        var fromEffect: TrackEffect = mediaTrack.effects[0]
        var toEffect: TrackEffect = mediaTrack.effects[0]

        run breaker@{
            mediaTrack.effects.forEach { trackEffect ->
                if (spriteTimePosition >= trackEffect.time) {
                    fromEffect = trackEffect
                }
                if (spriteTimePosition <= trackEffect.time) {
                    toEffect = trackEffect
                    return@breaker
                }
            }
        }

        val transformDuration = (toEffect.time - fromEffect.time).toFloat()
        val transformLocalTimePosition = (spriteTimePosition - fromEffect.time).toFloat()
        val transformPercent = if (transformDuration == 0f) {
            0f
        } else {
            transformLocalTimePosition / transformDuration
        }

        val x = fromEffect.posX + (toEffect.posX - fromEffect.posX) * transformPercent
        val y = fromEffect.posY + (toEffect.posY - fromEffect.posY) * transformPercent
        val scale = fromEffect.scale + (toEffect.scale - fromEffect.scale) * transformPercent
        val rotation = fromEffect.rotation + (toEffect.rotation - fromEffect.rotation) * transformPercent
        val opacity = fromEffect.opacity + (toEffect.opacity - fromEffect.opacity) * transformPercent
        val sepia = fromEffect.sepia + (toEffect.sepia - fromEffect.sepia) * transformPercent
        val volume = fromEffect.volume + (toEffect.volume - fromEffect.volume) * transformPercent

        Matrix.translateM(transformMatrix, 0, x * videoTranslateFactor, y, 0f)
        Matrix.rotateM(transformMatrix, 0, rotation, 0f, 0f, 1f)
        Matrix.scaleM(transformMatrix, 0, videoScaleFactor, 1f, 1f)
        Matrix.scaleM(transformMatrix, 0, scale, scale, 1f)

        val sepiaCenterPosition = if (mediaTrack.hasSepiaPositionAnimation) {
            when {
                spriteTimePosition < mediaTrack.position + 5000L -> {
                    0.25f
                }
                spriteTimePosition < mediaTrack.position + 5000L + (mediaTrack.duration - 5000L) / 2f -> {
                    0.25f + 0.25f * (spriteTimePosition - (mediaTrack.position + 5000L)) / ((mediaTrack.duration - 5000L) / 2f)
                }
                else -> {
                    0.5f - 0.25f * (spriteTimePosition - (mediaTrack.position + 5000L + (mediaTrack.duration - 5000L) / 2f)) / ((mediaTrack.duration - 5000L) / 2f)
                }
            }
        } else {
            Float.MAX_VALUE
        }

        return FrameData(
                transformMatrix = transformMatrix,
                opacity = opacity,
                sepia = sepia,
                sepiaCenterPosition = sepiaCenterPosition,
                volume = volume
        )
    }
}

data class FrameData(
        val transformMatrix: FloatArray,
        val opacity: Float,
        val sepia: Float,
        val sepiaCenterPosition: Float,
        val volume: Float
)