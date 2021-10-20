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
import android.opengl.EGL14
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.os.Handler
import android.os.Looper
import dev.chromeos.videocompositionsample.presentation.media.MediaTrack
import dev.chromeos.videocompositionsample.presentation.media.encoder.MediaEncoder
import dev.chromeos.videocompositionsample.presentation.media.encoder.surface.EncoderState
import dev.chromeos.videocompositionsample.presentation.media.encoder.utils.VideoMimeType
import java.io.File
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class CompositionRenderer(private val context: Context,
                          private val onSurfaceChangedListener: () -> Unit,
                          private val onStopExport: () -> Unit,
                          private val onEncodeError: (Exception) -> Unit
) : GLSurfaceView.Renderer {

    companion object {
        const val ENCODE_SPEED_FACTOR = 0.5f
    }

    val timePosition: Long
        get() = spriteList.firstOrNull()?.spriteTimePosition ?: 0

    val fpsAverageList: List<Float>
        get() = spriteList.map { it.fpsAverage }

    private lateinit var shader: Shader
    var width = 0
    var height = 0
    private var spriteList: List<Sprite> = emptyList()

    private var absoluteFilePath: String = ""
    private var exportWidth = 0
    private var exportHeight = 0
    private var exportVideoMimeType = VideoMimeType.h264
    private var isTestCaseMode = false

    private var mainThreadHandler: Handler = Handler(Looper.getMainLooper())

    @Volatile
    private var opacity = 1f
    private var mutableOpacityTrackIndex = 0

    private var mediaEncoder: MediaEncoder? = null
    private var outputFile: File? = null
    private var glMediaData: GlMediaData? = null
    private var isRecordingEnabled = false
    private var recordingStatus = EncoderState.OFF

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig) {

        shader = Shader(context, "vertex_shader.shdr", "fragment_shader.shdr")

        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {

        this.width = width
        this.height = height

        onSurfaceChangedListener.invoke()
    }

    fun initMediaTrackList(mediaTrackList: List<MediaTrack>) {
        val textures = IntArray(mediaTrackList.size) { i -> i + 1 }
        glMediaData = GlMediaData(mediaTrackList, textures)
        GLES30.glGenTextures(mediaTrackList.size, textures, 0)
        val playedMediaTracks: MutableSet<Int> = mutableSetOf()
        spriteList = mediaTrackList
                .mapIndexed { index: Int, mediaTrack: MediaTrack ->
                    Sprite(
                            width = width,
                            height = height,
                            programId = shader.programId,
                            textureId = textures[index],
                            mediaTrack = mediaTrack,
                            onActiveExportStateChangeListener = { activeIndex, isActive ->
                                if (isActive) {
                                    mediaEncoder?.notifyStartMediaTrack(activeIndex)
                                } else {
                                    playedMediaTracks.add(index)
                                    if (playedMediaTracks.size < mediaTrackList.size) {
                                        mediaEncoder?.notifyPauseMediaTrack(activeIndex)
                                    } else {
                                        mainThreadHandler.post { onStopExport.invoke() }
                                    }
                                }
                            }
                    ).apply {
                        pause()
                        isPlayWhenReady = false
                        mediaTrack.player?.playWhenReady = false
                    }
                }

        isRecordingEnabled = false
    }

    fun playerStateChanged(index: Int, playWhenReady: Boolean) {
        if (playWhenReady != spriteList[index].isPlayWhenReady) {
            spriteList[index].isPlayWhenReady = playWhenReady
        }
    }

    fun play() {
        spriteList.forEach { it.play() }
    }

    fun pause() {
        isRecordingEnabled = false
        when (recordingStatus) {
            EncoderState.ON, EncoderState.RESUMED -> {
                mediaEncoder?.stop()
                recordingStatus = EncoderState.OFF
            }
            EncoderState.OFF -> {
            }
        }
        spriteList.forEach { it.pause() }
    }

    fun setBeginTimePosition() {
        spriteList.forEach { it.setBeginTimePosition() }
    }

    fun seek(position: Long) {
        spriteList.forEach { it.seek(position) }
    }

    fun setOpacity(trackIndex: Int, opacity: Float) {
        this.opacity = opacity
        mutableOpacityTrackIndex = trackIndex
    }

    fun export(absoluteFilePath: String, width: Int, height: Int, videoMimeType: VideoMimeType, isTestCaseMode: Boolean) {
        this.absoluteFilePath = absoluteFilePath
        this.isTestCaseMode = isTestCaseMode
        exportWidth = width
        exportHeight = height
        exportVideoMimeType = videoMimeType
        isRecordingEnabled = true
        spriteList.forEach { it.export(ENCODE_SPEED_FACTOR) }
    }

    private fun createNewOutputFile() {
        outputFile = File(absoluteFilePath)
    }

    override fun onDrawFrame(gl: GL10) {
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT or GLES30.GL_COLOR_BUFFER_BIT)

        if (spriteList.isEmpty()) return

        if (isRecordingEnabled) {
            when (recordingStatus) {
                EncoderState.OFF -> {
                    glMediaData?.let { glMediaData ->
                        createNewOutputFile()
                        outputFile?.let { outputFile ->
                            mediaEncoder = MediaEncoder(onStopped = { mediaEncoder = null })
                            mediaEncoder?.start(
                                    context = context,
                                    eglContext = EGL14.eglGetCurrentContext(),
                                    outputFile = outputFile,
                                    width = exportWidth,
                                    height = exportHeight,
                                    videoMimeType = exportVideoMimeType,
                                    encodeSpeedFactor = ENCODE_SPEED_FACTOR,
                                    glMediaData = glMediaData,
                                    onError = {
                                        isRecordingEnabled = false
                                        outputFile.delete()
                                        mainThreadHandler.post { onEncodeError.invoke(it) }
                                    }
                            )
                            recordingStatus = EncoderState.ON
                        } ?: mainThreadHandler.post { onEncodeError.invoke(Exception("Error output file creation")) }
                    }
                }
                EncoderState.RESUMED -> {
                    mediaEncoder?.updateSharedContext(
                            width = exportWidth,
                            height = exportHeight,
                            sharedContext = EGL14.eglGetCurrentContext()
                    )
                    recordingStatus = EncoderState.ON
                }
                EncoderState.ON -> {
                    val spriteTracksData = spriteList.map { sprite ->
                        SpriteTrackData(
                                isPlayWhenReady = sprite.isPlayWhenReady,
                                videoScaleFactor = sprite.videoScaleFactor,
                                videoTranslateFactor = sprite.videoTranslateFactor
                        )
                    }
                    val timePosition = (ENCODE_SPEED_FACTOR * (System.currentTimeMillis() - spriteList[0].systemStartPlayTime)).toLong()
                    val encoderFrameData = EncoderFrameData(
                            compositionTimePosition = timePosition,
                            timeStampNs = System.nanoTime(),
                            spriteTracksData = spriteTracksData
                    )
                    mediaEncoder?.frameAvailable(encoderFrameData)

                    spriteList.forEachIndexed { index, sprite ->
                        val alpha = if (index == mutableOpacityTrackIndex) {
                            opacity
                        } else {
                            1f
                        }
                        sprite.drawClip(alpha)
                    }
                }
            }
        } else {
            spriteList.forEachIndexed { index, sprite ->
                val alpha = if (index == mutableOpacityTrackIndex) {
                    opacity
                } else {
                    1f
                }
                sprite.drawClip(alpha)
            }

            when (recordingStatus) {
                EncoderState.ON, EncoderState.RESUMED -> {
                    mediaEncoder?.stop()
                    recordingStatus = EncoderState.OFF
                }
                EncoderState.OFF -> {
                }
            }
        }
    }

}