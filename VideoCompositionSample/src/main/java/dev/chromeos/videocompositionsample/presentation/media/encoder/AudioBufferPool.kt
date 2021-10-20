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

import dev.chromeos.videocompositionsample.presentation.media.encoder.utils.collapseBufferBeginRange
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

class AudioBufferPool(private val encoderSampleRateHz: Int, private val bufferSize: Int, speed: Float) {

    companion object {
        private const val STACK_SIZE = 30
        private const val READY_LIMIT = 3
    }

    private val shortBufferSize = bufferSize / 2
    private val stackArray: MutableList<AudioBuffer> = mutableListOf()
    private var audioFrameNumber: Long = 0L
    private var startFullingLimit = 0

    private val outputPool = ShortArray((shortBufferSize * 50)) //TODO should set adaptive size
    private var outputPoolPosition = 0

    private var inputBufferPool = ByteBuffer.allocate(7 * bufferSize)
    private var inputBufferPoolPoolPosition = 0
    private var maxInputBufferSize = 0

    private var resampler: Resampler? = null

    private var lastDequeueTimeStamp = 0L

    val isReady: Boolean
        get() = stackArray.isNotEmpty()
                && startFullingLimit >= READY_LIMIT
                && stackArray[0].isReady
                && stackArray[0].timeStampNs != lastDequeueTimeStamp

    fun queue(audioBuffer: AudioBuffer) {

        // check for matching buffer size
        if (audioBuffer.byteBuffer.limit() < bufferSize) {
            inputBufferPool.put(audioBuffer.byteBuffer)
            maxInputBufferSize = max(maxInputBufferSize, audioBuffer.byteBuffer.limit())
            inputBufferPoolPoolPosition += audioBuffer.byteBuffer.limit()
            if (inputBufferPoolPoolPosition < bufferSize) {
                return
            } else {
                audioBuffer.byteBuffer = collapseBufferBeginRange(bufferSize, inputBufferPool)
                inputBufferPoolPoolPosition = inputBufferPool.position()
                val sampleRateFactor = bufferSize / maxInputBufferSize
                maxInputBufferSize = 0
                audioBuffer.sampleRateHz /= sampleRateFactor
            }
        } else if (audioBuffer.byteBuffer.limit() > bufferSize) { // TODO now this is just stub to prevent crash
            inputBufferPool.put(audioBuffer.byteBuffer)
            audioBuffer.byteBuffer = collapseBufferBeginRange(bufferSize, inputBufferPool)
            inputBufferPoolPoolPosition = inputBufferPool.position()
        }

        val audioBufferList: MutableList<AudioBuffer> = mutableListOf()

        if (audioBuffer.sampleRateHz != encoderSampleRateHz && resampler == null) {
            resampler = Resampler(audioBuffer.sampleRateHz, encoderSampleRateHz, audioBuffer.channelCount, shortBufferSize)
        }

        when {
            audioBuffer.sampleRateHz == encoderSampleRateHz -> {
                audioBufferList.add(audioBuffer)
            }
            audioBuffer.sampleRateHz < encoderSampleRateHz -> {
                audioBufferList.addAll(getLowSampleRateAudioBufferList(audioBuffer))
            }
            audioBuffer.sampleRateHz > encoderSampleRateHz -> {
                audioBufferList.addAll(getHighSampleRateAudioBuffer(audioBuffer))
            }
        }

        if (stackArray.size >= STACK_SIZE) {
            for (i in 0 until audioBufferList.size) {
                if (stackArray.isNotEmpty()) {
                    stackArray.removeAt(0)
                }
            }
        }

        audioBufferList.forEach {
            stackArray.add(it)
            audioFrameNumber++
            if (startFullingLimit < READY_LIMIT) {
                startFullingLimit++
            }
        }
    }

    fun dequeue(): AudioBuffer? {
        return if (stackArray.isNotEmpty()) {
            if (isReady) {
                lastDequeueTimeStamp = stackArray[0].timeStampNs
                val stack = AudioBuffer(
                        stackArray[0].byteBuffer,
                        lastDequeueTimeStamp,
                        stackArray[0].sampleRateHz,
                        stackArray[0].channelCount,
                        true
                )
                stackArray.removeAt(0)
                stack
            } else {
                null
            }
        } else {
            null
        }
    }

    private fun getLowSampleRateAudioBufferList(audioBuffer: AudioBuffer): List<AudioBuffer> {

        val shortBuffer = audioBuffer.byteBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val shortArray = ShortArray(shortBuffer.limit())
        shortBuffer.get(shortArray)

        val floatArray = FloatArray(shortArray.size)
        shortArray.forEachIndexed { index, sh ->
            floatArray[index] = sh.toFloat()
        }

        val resampledFloatArray = resampler!!.resample(floatArray)
        val outShortArray = ShortArray(resampledFloatArray.size)
        resampledFloatArray.forEachIndexed { index, fl ->
            outShortArray[index] = fl.toShort()
        }

        for (i in outShortArray.indices) {
            outputPool[outputPoolPosition++] = outShortArray[i]
        }

        val audioBufferList: MutableList<AudioBuffer> = mutableListOf()

        val count = outputPoolPosition / shortBufferSize

        for (i in 0 until count) {
            val byteArray = ByteArray(bufferSize)
            val outputShortArray = outputPool.copyOfRange(i * shortBufferSize, (i + 1) * shortBufferSize)
            ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(outputShortArray)

            val outBuffer = ByteBuffer.allocate(bufferSize)
            outBuffer.put(byteArray)
            outBuffer.flip()

            val timeStampNs = audioBuffer.timeStampNs + i
            audioBufferList.add(
                    AudioBuffer(
                            byteBuffer = outBuffer,
                            timeStampNs = timeStampNs,
                            sampleRateHz = audioBuffer.sampleRateHz,
                            channelCount = audioBuffer.channelCount,
                            isReady = true
                    )
            )
        }

        // shift remain pool items to the begin
        val startOfRemains = shortBufferSize * count
        for (i in startOfRemains until outputPoolPosition) {
            outputPool[i-startOfRemains] = outputPool[i]
        }
        outputPoolPosition -= shortBufferSize * count

        return audioBufferList
    }

    private fun getHighSampleRateAudioBuffer(audioBuffer: AudioBuffer): List<AudioBuffer> {
        val shortBuffer = audioBuffer.byteBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val shortArray = ShortArray(shortBuffer.limit())
        shortBuffer.get(shortArray)

        val floatArray = FloatArray(shortArray.size)
        shortArray.forEachIndexed { index, sh ->
            floatArray[index] = sh.toFloat()
        }

        val resampledFloatArray = resampler!!.resample(floatArray)
        val outShortArray = ShortArray(resampledFloatArray.size)
        resampledFloatArray.forEachIndexed { index, fl ->
            outShortArray[index] = fl.toShort()
        }

        for (i in outShortArray.indices) {
            outputPool[outputPoolPosition++] = outShortArray[i]
        }

        val audioBufferList: MutableList<AudioBuffer> = mutableListOf()

        if (outputPoolPosition >= shortBufferSize) {
            val byteArray = ByteArray(bufferSize)
            val outputShortArray = outputPool.copyOfRange(0, shortBufferSize)
            ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(outputShortArray)

            val outBuffer = ByteBuffer.allocate(bufferSize)
            outBuffer.put(byteArray)
            outBuffer.flip()

            val timeStampNs = audioBuffer.timeStampNs
            audioBufferList.add(
                    AudioBuffer(
                            byteBuffer = outBuffer,
                            timeStampNs = timeStampNs,
                            sampleRateHz = audioBuffer.sampleRateHz,
                            channelCount = audioBuffer.channelCount,
                            isReady = true
                    )
            )

            // shift remain pool items to the begin
            for (i in shortBufferSize until outputPoolPosition) {
                outputPool[i-shortBufferSize] = outputPool[i]
            }
            outputPoolPosition -= shortBufferSize
        }

        return audioBufferList
    }
    data class AudioBuffer(
            var byteBuffer: ByteBuffer,
            val timeStampNs: Long,
            var sampleRateHz: Int,
            val channelCount: Int,
            var isReady: Boolean
    )
}