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

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import dev.chromeos.videocompositionsample.presentation.media.MediaTrack
import dev.chromeos.videocompositionsample.presentation.media.TrackEffect
import dev.chromeos.videocompositionsample.presentation.media.encoder.base.BaseEncoder
import dev.chromeos.videocompositionsample.presentation.media.encoder.utils.deepCopy
import dev.chromeos.videocompositionsample.presentation.opengl.GlMediaData
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.max
import kotlin.math.min

class AudioEncoder: Runnable {

    companion object {
        private const val TAG = "AudioEncoder"
        private const val MSG_START_RECORDING = 0
        private const val MSG_STOP_RECORDING = 1
        private const val MSG_AUDIO_BUFFER_AVAILABLE = 2
        private const val MSG_MEDIA_TRACK_STARTED = 3
        private const val MSG_MEDIA_TRACK_PAUSED = 4
        private const val MSG_QUIT = 5

        private const val AUDIO_MIME_TYPE = "audio/mp4a-latm"
        private const val ENCODER_SAMPLE_RATE = 48000
        private const val BIT_RATE = 320000
        private const val BUFFER_SIZE = 4096
        private const val CHANNEL_COUNT = 2
        private const val MAX_BUFFER_AMPLITUDE = 32767
    }

    private lateinit var handler: AudioEncoderHandler
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private var isReady = false
    private var isRunning = false
    private var onError: ((e: Exception) -> Unit)? = null
    private var isAudioEncoderInitialized = false
    private var startWhenNs = 0L
    private var encodeSpeedFactor = 1.0f

    private lateinit var glMediaData: GlMediaData
    private lateinit var audioEncoder: BaseEncoder

    private var mainThreadHandler: Handler = Handler(Looper.getMainLooper())

    private lateinit var audioBufferPoolArray: Array<AudioBufferPool>
    private val activeMediaTrackIds: MutableSet<Int> = mutableSetOf()

    private class AudioEncoderHandler(audioThread: AudioEncoder) : Handler() {

        private val weakAudioEncoder: WeakReference<AudioEncoder> = WeakReference(audioThread)

        override fun handleMessage(inputMessage: Message) {
            val what = inputMessage.what
            val obj = inputMessage.obj
            val audioThread = weakAudioEncoder.get()
            if (audioThread == null) {
                Log.w(TAG, "ThreadHandler.handleMessage: audioRecorder is null")
                return
            }
            when (what) {
                MSG_START_RECORDING -> audioThread.handleStartRecording(obj as AudioStartData)
                MSG_STOP_RECORDING -> audioThread.handleStopRecording()
                MSG_AUDIO_BUFFER_AVAILABLE -> {
                    val index = inputMessage.arg1
                    val sampleRateHz = inputMessage.arg2
                    audioThread.handleAudioBuffer(index, sampleRateHz, obj as AudioData)
                }
                MSG_MEDIA_TRACK_STARTED -> audioThread.handleMediaTrackStart(inputMessage.arg1)
                MSG_MEDIA_TRACK_PAUSED -> audioThread.handleMediaTrackPause(inputMessage.arg1)

                MSG_QUIT -> Looper.myLooper()?.quit()
                else -> throw RuntimeException("Unhandled msg what=$what")
            }
        }
    }

    fun startRecording(
            glMediaData: GlMediaData,
            muxer: Muxer,
            encodeSpeedFactor: Float,
            onError: (e: Exception) -> Unit
    ) {

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

        val startData = AudioStartData(
                muxer = muxer,
                encodeSpeedFactor = encodeSpeedFactor,
                mediaTrackList = glMediaData.mediaTrackList
        )
        handler.sendMessage(handler.obtainMessage(MSG_START_RECORDING, startData))

        glMediaData.mediaTrackList.forEachIndexed { index, mediaTrack ->
            mediaTrack.player?.onAudioQueueInputListener = { byteBuffer, sampleRateHz, channelCount ->
                if (isAudioEncoderInitialized) {
                    val speed = mediaTrack.videoDuration.toFloat() / mediaTrack.duration.toFloat()
                    val audioData = AudioData(
                            System.nanoTime(),
                            deepCopy(byteBuffer),
                            channelCount,
                            speed
                    )
                    handler.sendMessage(handler.obtainMessage(MSG_AUDIO_BUFFER_AVAILABLE, index, sampleRateHz, audioData))
                }
            }
        }
    }

    fun notifyStartMediaTrack(index: Int) {
        handler.sendMessage(handler.obtainMessage(MSG_MEDIA_TRACK_STARTED, index, 0))
    }

    fun notifyPauseMediaTrack(index: Int) {
        handler.sendMessage(handler.obtainMessage(MSG_MEDIA_TRACK_PAUSED, index, 0))
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
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY)
        Looper.prepare()
        lock.withLock {
            handler = AudioEncoderHandler(this)
            isReady = true
            condition.signal()
        }
        Looper.loop()
        lock.withLock {
            isRunning = false
            isReady = false
        }
    }

    private fun handleMediaTrackStart(index: Int) {
        activeMediaTrackIds.add(index)
    }

    private fun handleMediaTrackPause(index: Int) {
        activeMediaTrackIds.remove(index)
    }

    private var mediaTrackList: List<MediaTrack> = listOf()

    private fun handleStartRecording(audioStartData: AudioStartData) {

        try {
            mediaTrackList = audioStartData.mediaTrackList

//            Record Audio from mic
//            val min_buffer_size = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
//            var buffer_size: Int = SAMPLES_PER_FRAME * 10
//            if (buffer_size < min_buffer_size) buffer_size = (min_buffer_size / SAMPLES_PER_FRAME + 1) * SAMPLES_PER_FRAME * 2
//
//
//            audioRecord = AudioRecord(
//                    MediaRecorder.AudioSource.DEFAULT,  // source
//                    SAMPLE_RATE,  // sample rate, hz
//                    CHANNEL_CONFIG,  // channels
//                    AUDIO_FORMAT,  // audio format
//                    buffer_size) // buffer size (bytes)
//
//
//            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
//                Log.d("->>>>>>","AudioRecord instance failed to initialize, state is (" + audioRecord.state + ")")
//            }

            val format = MediaFormat.createAudioFormat(AUDIO_MIME_TYPE, ENCODER_SAMPLE_RATE, CHANNEL_COUNT)
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            audioEncoder = BaseEncoder(AUDIO_MIME_TYPE, format, audioStartData.muxer)
            audioEncoder.encoder.start()
            audioFrameCount = 0L
            startWhenNs = System.nanoTime()
            isAudioEncoderInitialized = true

            encodeSpeedFactor = audioStartData.encodeSpeedFactor

            val speedList = glMediaData.mediaTrackList.map { mediaTrack ->
                mediaTrack.videoDuration.toFloat() / mediaTrack.duration.toFloat()
            }
            audioBufferPoolArray = Array(glMediaData.mediaTrackList.size) {
                AudioBufferPool((ENCODER_SAMPLE_RATE / speedList[it]).toInt(), BUFFER_SIZE, speedList[it])
            }

        } catch (e: java.lang.RuntimeException) {
            mainThreadHandler.post {
                onError?.invoke(e)
            }
        }
    }

    private var audioFrameCount = 0L
    private var startTimeShiftUs = 0L
    private val bufferDurationUs = (1000000 * (BUFFER_SIZE.toFloat() / 4f) / (ENCODER_SAMPLE_RATE).toFloat()).toLong()
    private val bufferDurationWeightMs = bufferDurationUs / 1000 / BUFFER_SIZE

    @Synchronized
    fun handleAudioBuffer(index: Int, sampleRateHz: Int, audioData: AudioData) {
        if (!isAudioEncoderInitialized) return

        audioBufferPoolArray[index].queue(
                AudioBufferPool.AudioBuffer(
                        byteBuffer = audioData.byteBuffer,
                        timeStampNs = audioData.presentationTimeNs,
                        sampleRateHz = sampleRateHz,
                        channelCount = audioData.channelCount,
                        isReady = true
                )
        )

        while (isActiveBuffersReady()) {
            val resultBuffer = ByteBuffer.allocate(BUFFER_SIZE)

            val trackBufferMap: MutableMap<Int, ByteBuffer?> = mutableMapOf()
            activeMediaTrackIds.forEach {
                val buffer = audioBufferPoolArray[it].dequeue()?.byteBuffer
                buffer?.position(0)
                trackBufferMap[it] = buffer
            }

            if (audioFrameCount == 0L) {
                startTimeShiftUs = (System.nanoTime() - startWhenNs) / 1000 + bufferDurationUs
            }
            val presentationTimeUs = audioFrameCount * bufferDurationUs + startTimeShiftUs
            val presentationTimeMs = presentationTimeUs / 1000

            audioFrameCount++

            for (i in 0 until BUFFER_SIZE) {
                val tracksByteSum = trackBufferMap
                        .map { entry ->
                            val byte = entry.value?.get()?: 0
                            val volume = getVolume(
                                    spriteTimePosition = presentationTimeMs + bufferDurationWeightMs * i,
                                    mediaTrack = mediaTrackList[entry.key])
                            byte * volume
                        }
                        .sum()

                val mixFactor = when (trackBufferMap.size) {
                    1 -> 1.0f
                    2 -> 0.9f
                    3 -> 0.8f
                    4 -> 0.7f
                    else -> 0.6f
                }
                val mix = max(min((tracksByteSum * mixFactor).toInt(), MAX_BUFFER_AMPLITUDE), - MAX_BUFFER_AMPLITUDE)
                resultBuffer.put(mix.toByte())
            }

            resultBuffer.flip()

            encodeAudio(resultBuffer, presentationTimeUs)
            audioEncoder.drain(false)
        }
    }

    private fun isActiveBuffersReady(): Boolean {
        activeMediaTrackIds.forEach { index ->
            if (!audioBufferPoolArray[index].isReady) {
                return false
            }
        }
        return true
    }

    private fun getVolume(spriteTimePosition: Long, mediaTrack: MediaTrack): Float {

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

        return fromEffect.volume + (toEffect.volume - fromEffect.volume) * transformPercent
    }

//    Encode Audio from mic
//    fun sendAudioToEncoder(endOfStream: Boolean) {
//        // send current frame data to encoder
//        val inputBuffers: Array<ByteBuffer> = audioEncoder.encoder.inputBuffers
//        val inputBufferIndex: Int = audioEncoder.encoder.dequeueInputBuffer(-1)
//        if (inputBufferIndex >= 0) {
//            val inputBuffer = inputBuffers[inputBufferIndex]
//            inputBuffer.clear()
//            var presentationTimeNs = System.nanoTime()
//            val inputLength = audioRecord.read(inputBuffer, SAMPLES_PER_FRAME)
//            presentationTimeNs -= inputLength / ENCODER_SAMPLE_RATE / 1000000000.toLong()
//            val presentationTimeUs: Long = (presentationTimeNs - startWhen) / 1000
//            if (endOfStream) {
//                audioEncoder.encoder.queueInputBuffer(inputBufferIndex, 0, inputLength, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
//            } else {
//                audioEncoder.encoder.queueInputBuffer(inputBufferIndex, 0, inputLength, presentationTimeUs, 0)
//            }
//        }
//    }

    private fun encodeAudio(buffer: ByteBuffer?, presentationTimeUs: Long) {
        val timeoutSecUs = 10000L
        val inputBuffers: Array<ByteBuffer> = audioEncoder.encoder.inputBuffers
        while (true) {
            val inputBufferIndex: Int = audioEncoder.encoder.dequeueInputBuffer(timeoutSecUs)
            if (inputBufferIndex >= 0) {
                val inputBuffer = inputBuffers[inputBufferIndex]
                inputBuffer.clear()
                buffer?.let {
                    inputBuffer.put(buffer)
                    buffer.clear()
                }
                audioEncoder.encoder.queueInputBuffer(inputBufferIndex, 0, BUFFER_SIZE,
                        presentationTimeUs, 0)
                break
            } else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // wait for MediaCodec encoder is ready to encode
                // nothing to do here because MediaCodec#dequeueInputBuffer(TIMEOUT_USEC)
                // will wait for maximum TIMEOUT_USEC(10msec) on each call
            }
        }
    }

    private fun handleStopRecording() {
        isAudioEncoderInitialized = false
        try {
          //  audioEncoder.drain(true)
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
            audioEncoder.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

data class AudioStartData(
    val muxer: Muxer,
    val encodeSpeedFactor: Float,
    val mediaTrackList: List<MediaTrack>
)

data class AudioData (
        val presentationTimeNs: Long,
        val byteBuffer: ByteBuffer,
        val channelCount: Int,
        val speed: Float
)