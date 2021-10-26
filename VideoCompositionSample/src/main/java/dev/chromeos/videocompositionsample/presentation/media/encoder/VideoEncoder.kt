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
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.opengl.EGLContext
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.Matrix
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import dev.chromeos.videocompositionsample.presentation.media.MediaTrack
import dev.chromeos.videocompositionsample.presentation.media.TrackEffect
import dev.chromeos.videocompositionsample.presentation.media.encoder.base.BaseEncoder
import dev.chromeos.videocompositionsample.presentation.media.encoder.surface.EglCore
import dev.chromeos.videocompositionsample.presentation.media.encoder.surface.WindowSurface
import dev.chromeos.videocompositionsample.presentation.media.encoder.utils.VideoMimeType
import dev.chromeos.videocompositionsample.presentation.media.encoder.utils.getVideoOptimalBitrate
import dev.chromeos.videocompositionsample.presentation.opengl.*
import java.lang.ref.WeakReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class VideoEncoder: Runnable {

    companion object {
        private const val TAG = "VideoEncoder"
        private const val MSG_START_RECORDING = 0
        private const val MSG_STOP_RECORDING = 1
        private const val MSG_FRAME_AVAILABLE = 2
        private const val MSG_UPDATE_SHARED_CONTEXT = 3
        private const val MSG_QUIT = 4

        private const val FRAME_RATE = 30
        private const val I_FRAME_INTERVAL = 4
    }

    private lateinit var handler: VideoEncoderHandler
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private var isReady = false
    private var isRunning = false
    private var onError: ((e: Exception) -> Unit)? = null

    private lateinit var shader: Shader
    private lateinit var glMediaData: GlMediaData

    private lateinit var videoEncoder: BaseEncoder
    private lateinit var inputWindowSurface: WindowSurface
    private lateinit var eglCore: EglCore
    private var isVideoEncoderInitialized = false

    private val plane = Plane()
    private val vpMatrix = FloatArray(16)

    private var mainThreadHandler: Handler = Handler(Looper.getMainLooper())

    private class VideoEncoderHandler(videoEncoder: VideoEncoder) : Handler() {

        private val weakVideoEncoder: WeakReference<VideoEncoder> = WeakReference(videoEncoder)

        override fun handleMessage(inputMessage: Message) {
            val what = inputMessage.what
            val obj = inputMessage.obj
            val videoThread = weakVideoEncoder.get()
            if (videoThread == null) {
                Log.w(TAG, "Handler.handleMessage: videoRecorder is null")
                return
            }
            when (what) {
                MSG_START_RECORDING -> videoThread.handleStartRecording(obj as VideoStartData)
                MSG_STOP_RECORDING -> videoThread.handleStopRecording()
                MSG_FRAME_AVAILABLE -> videoThread.handleFrameAvailable(obj as EncoderFrameData)
                MSG_UPDATE_SHARED_CONTEXT -> {
                    val width = inputMessage.arg1
                    val height = inputMessage.arg2
                    videoThread.handleUpdateSharedContext(width, height, inputMessage.obj as EGLContext)
                }
                MSG_QUIT -> Looper.myLooper()?.quit()
                else -> throw RuntimeException("Unhandled msg what=$what")
            }
        }
    }

    fun startRecording(
            context: Context,
            eglContext: EGLContext,
            width: Int,
            height: Int,
            videoMimeType: VideoMimeType,
            encodeSpeedFactor: Float,
            glMediaData: GlMediaData,
            muxer: Muxer,
            onError: (e: Exception) -> Unit
    ) {
        this.shader = Shader(context, "vertex_shader.glsl", "fragment_shader.glsl")
        this.glMediaData = glMediaData
        this.onError = onError

        lock.withLock {
            if (isRunning) {
                return
            }
            isRunning = true
            Thread(this, TAG).start()
            while (!isReady) {
                try {
                    condition.await()
                } catch (ie: InterruptedException) {
                    // ignore
                }
            }
        }
        val startData = VideoStartData(
                sharedContext = eglContext,
                width = width,
                height = height,
                muxer = muxer,
                videoMimeType = videoMimeType,
                encodeSpeedFactor = encodeSpeedFactor,
                trackCount = glMediaData.mediaTrackList.size
        )
        handler.sendMessage(handler.obtainMessage(MSG_START_RECORDING, startData))
    }

    fun frameAvailable(encoderFrameData: EncoderFrameData) {
        lock.withLock {
            if (!isReady) {
                return
            }
        }

        handler.removeMessages(MSG_FRAME_AVAILABLE)
        handler.sendMessage(handler.obtainMessage(MSG_FRAME_AVAILABLE, encoderFrameData))
    }

    fun updateSharedContext(width: Int, height: Int, sharedContext: EGLContext) {
        handler.sendMessage(handler.obtainMessage(MSG_UPDATE_SHARED_CONTEXT, width, height, sharedContext))
    }

    fun stopRecording() {

        handler.sendMessage(handler.obtainMessage(MSG_STOP_RECORDING))
        handler.sendMessage(handler.obtainMessage(MSG_QUIT))
    }

    val isRecording: Boolean
        get() {
            lock.withLock { return isRunning }
        }

    override fun run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
        Looper.prepare()
        lock.withLock {
            handler = VideoEncoderHandler(this)
            isReady = true
            condition.signal()
        }
        Looper.loop()
        lock.withLock {
            isRunning = false
            isReady = false
        }
    }

    private var encodeSpeedFactor = 0.5f

    private fun handleStartRecording(videoStartData: VideoStartData) {
        startWhenNs = 0L
        encodeSpeedFactor = videoStartData.encodeSpeedFactor
        try {
            val format = MediaFormat.createVideoFormat(videoStartData.videoMimeType.mimeType, videoStartData.width, videoStartData.height)
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            format.setInteger(MediaFormat.KEY_BIT_RATE, getVideoOptimalBitrate(videoStartData.videoMimeType, videoStartData.width, videoStartData.height)/2)
            format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)

            videoEncoder = BaseEncoder(videoStartData.videoMimeType.mimeType, format, videoStartData.muxer)
            val inputSurface = videoEncoder.encoder.createInputSurface()
            videoEncoder.encoder.start()

            eglCore = EglCore(videoStartData.sharedContext, EglCore.FLAG_RECORDABLE)
            inputWindowSurface = WindowSurface(eglCore, inputSurface, true)
            inputWindowSurface.makeCurrent()

            initGl(videoStartData.width, videoStartData.height)
            isVideoEncoderInitialized = true

        } catch (e: java.lang.RuntimeException) {
            mainThreadHandler.post {
                onError?.invoke(e)
            }
        }
    }

    private fun initGl(width: Int, height: Int) {
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)

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

        GLES30.glBindTexture(Sprite.GL_TEXTURE_EXTERNAL_OES, 1)
        GLES30.glTexParameterf(Sprite.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST.toFloat())
        GLES30.glTexParameterf(Sprite.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR.toFloat())

        GLES30.glTexParameterf(Sprite.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE.toFloat())
        GLES30.glTexParameterf(Sprite.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE.toFloat())
    }

    private var startWhenNs = 0L
    private var previousPresentationTimeNs = 0L
    private var previousAdjustedPresentationTimeNs = 0L
    private var encodeIntervalNs = 0L
    private var adaptiveIntervalFactor = 0.8f
    private var frameCount = 0L

    @Synchronized
    private fun handleFrameAvailable(encoderFrameData: EncoderFrameData) {

        if (!isVideoEncoderInitialized) return

        if (startWhenNs == 0L) {
            startWhenNs = System.nanoTime()
            encodeIntervalNs = (1000000000 / FRAME_RATE).toLong()
            previousPresentationTimeNs = 0L
            previousAdjustedPresentationTimeNs = 0L
            frameCount = 0L
        }

        val presentationTimeNs =  ((System.nanoTime() - startWhenNs) * encodeSpeedFactor).toLong()
        val adjustedPresentationTimeNs = (presentationTimeNs / encodeIntervalNs) * encodeIntervalNs

        var isNeedRecord = previousPresentationTimeNs == 0L
                || (presentationTimeNs - previousPresentationTimeNs) > encodeIntervalNs * adaptiveIntervalFactor

        if (isNeedRecord) {
            previousPresentationTimeNs = presentationTimeNs
            frameCount++
        }

        if (isNeedRecord && previousAdjustedPresentationTimeNs > 0L && previousAdjustedPresentationTimeNs == adjustedPresentationTimeNs) {
            isNeedRecord = false
        }

        if (isNeedRecord) {
            try {
                videoEncoder.drain(false)
            } catch (e: Exception) {
                startWhenNs = 0L
                releaseEncoder()
                mainThreadHandler.post {
                    onError?.invoke(e)
                    onError = null
                }
            }

            val spriteTracksData = encoderFrameData.spriteTracksData

            val positionsHandle = GLES30.glGetAttribLocation(shader.programId, "a_Position")
            val textureHandle = GLES30.glGetAttribLocation(shader.programId, "a_TexCoordinate")

            val mvpMatrixHandle = GLES30.glGetUniformLocation(shader.programId, "uMVPMatrix")
            val transformMatrixHandle = GLES30.glGetUniformLocation(shader.programId, "uTransformMatrix")

            val opacityHandle = GLES30.glGetUniformLocation(shader.programId, "opacity")
            val sepiaHandle = GLES30.glGetUniformLocation(shader.programId, "sepia")
            val sepiaCenterPositionHandle = GLES30.glGetUniformLocation(shader.programId, "sepiaCenterPosition")

            GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
            GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT or GLES30.GL_COLOR_BUFFER_BIT)

            glMediaData.mediaTrackList.forEachIndexed { index, mediaTrack ->
                val spriteTrackData = spriteTracksData[index]
                if (spriteTrackData.isPlayWhenReady) {
                    val frameData = getFrameData(encoderFrameData.compositionTimePosition, mediaTrack, spriteTrackData)
                    GLES30.glDisable(GLES30.GL_DITHER)
                    GLES30.glDisable(GLES30.GL_DEPTH_TEST)
                    GLES30.glEnable(GLES30.GL_BLEND)
                    GLES30.glBlendEquation(GLES30.GL_FUNC_ADD)
                    GLES30.glBlendFuncSeparate(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA, GLES30.GL_ONE, GLES30.GL_ONE)

                    GLES30.glUseProgram(shader.programId)
                    GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
                    GLES30.glBindTexture(Sprite.GL_TEXTURE_EXTERNAL_OES, glMediaData.textures[index])

                    plane.positionBuffer.position(Plane.TRIANGLE_VERTICES_DATA_POS_OFFSET)
                    GLES30.glVertexAttribPointer(positionsHandle, 3, GLES30.GL_FLOAT, false, Plane.TRIANGLE_VERTICES_DATA_STRIDE_BYTES, plane.positionBuffer)
                    GLES30.glEnableVertexAttribArray(positionsHandle)

                    plane.positionBuffer.position(Plane.TRIANGLE_VERTICES_DATA_UV_OFFSET)
                    GLES30.glVertexAttribPointer(textureHandle, 2, GLES30.GL_FLOAT, false, Plane.TRIANGLE_VERTICES_DATA_STRIDE_BYTES, plane.positionBuffer)
                    GLES30.glEnableVertexAttribArray(textureHandle)

                    GLES30.glUniform1f(opacityHandle, frameData.opacity)
                    GLES30.glUniform1f(sepiaHandle, frameData.sepia)
                    GLES30.glUniform1f(sepiaCenterPositionHandle, frameData.sepiaCenterPosition)

                    GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, vpMatrix, 0)
                    GLES30.glUniformMatrix4fv(transformMatrixHandle, 1, false, frameData.transformMatrix, 0)

                    GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, Plane.STARTING_INDEX, Plane.NUMBER_OF_INDICES)

                    GLES20.glDisableVertexAttribArray(positionsHandle)
                    GLES20.glDisableVertexAttribArray(textureHandle)
                    GLES30.glBindTexture(Sprite.GL_TEXTURE_EXTERNAL_OES, 0)
                    GLES30.glUseProgram(0)
                }
            }

            inputWindowSurface.setPresentationTime(adjustedPresentationTimeNs)
            previousAdjustedPresentationTimeNs = adjustedPresentationTimeNs
        }

        inputWindowSurface.swapBuffers()
    }

    private fun getFrameData(spriteTimePosition: Long, mediaTrack: MediaTrack, spriteTrackData: SpriteTrackData): FrameData {

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

        Matrix.translateM(transformMatrix, 0, x * spriteTrackData.videoTranslateFactor, y, 0f)
        Matrix.rotateM(transformMatrix, 0, rotation, 0f, 0f, 1f)
        Matrix.scaleM(transformMatrix, 0, spriteTrackData.videoScaleFactor, 1f, 1f)
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
                volume = 1f
        )
    }

    private fun handleUpdateSharedContext(width: Int, height: Int, newSharedContext: EGLContext) {

        inputWindowSurface.releaseEglSurface()
        eglCore.release()

        eglCore = EglCore(newSharedContext, EglCore.FLAG_RECORDABLE)
        inputWindowSurface.recreate(eglCore)
        inputWindowSurface.makeCurrent()

        initGl(width, height)
    }

    private fun handleStopRecording() {
        isVideoEncoderInitialized = false
        startWhenNs = 0L
        try {
            videoEncoder.drain(true)
            releaseEncoder()

        } catch (e: Exception) {
            mainThreadHandler.post {
                onError?.invoke(e)
                onError = null
            }
        }
    }

    private fun releaseEncoder() {
        try {
            videoEncoder.release()
            inputWindowSurface.release()
            eglCore.release()
        } catch (e: Exception) {
            try {
                inputWindowSurface.release()
                eglCore.release()
            } catch (e: Exception) {
                eglCore.release()
            }
        }
    }
}

data class VideoStartData(
        val sharedContext: EGLContext,
        val width: Int,
        val height: Int,
        val muxer: Muxer,
        val videoMimeType: VideoMimeType,
        val encodeSpeedFactor: Float,
        val trackCount: Int
)