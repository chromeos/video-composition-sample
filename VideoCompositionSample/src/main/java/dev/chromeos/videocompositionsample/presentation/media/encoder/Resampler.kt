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

import kotlin.math.ceil
import kotlin.math.floor

class Resampler (private val fromSampleRate: Int, private val toSampleRate: Int, private val channels: Int, inputBufferSize: Int) {

    private var lastWeight = 1f
    private var tailExists = false
    private val ratioWeight = fromSampleRate.toFloat() / toSampleRate.toFloat()
    private val outputBufferSize: Int = (ceil(inputBufferSize * toSampleRate / fromSampleRate / channels * 1.000000476837158203125) * channels).toInt() + channels

    private val outputBuffer = FloatArray(outputBufferSize)
    private val lastOutput = FloatArray(channels)

    fun resample(buffer: FloatArray): FloatArray {
        return when {
            fromSampleRate == toSampleRate -> {
                buffer
            }
            fromSampleRate < toSampleRate -> {
                val resample = linearInterpolation(buffer)
                lastWeight = 1f
                resample
            }
            else -> {
                val resample = multiTap(buffer)
                tailExists = false
                lastWeight = 0f
                resample
            }
        }
    }

    private fun multiTap(buffer: FloatArray): FloatArray {
        val bufferLength = buffer.size
        if ((bufferLength % channels) != 0) {
            throw(IllegalStateException("Buffer was of incorrect sample length."))
        }
        if (bufferLength <= 0) {
            return FloatArray(0)
        }

        var weight: Float
        var actualPosition = 0
        var amountToNext: Float
        var alreadyProcessedTail: Boolean = !tailExists
        tailExists = false
        var outputOffset = 0
        var currentPosition = 0f

        val outputVariableList = FloatArray(channels) { 0f }

        while (actualPosition < bufferLength && outputOffset < outputBufferSize) {
            if (alreadyProcessedTail) {
                weight = ratioWeight
                for (channel in 0 until channels) {
                    outputVariableList[channel] = 0f
                }
            } else {
                weight = lastWeight
                for (channel in 0 until channels) {
                    outputVariableList[channel] = lastOutput[channel]
                }
                alreadyProcessedTail = true
            }

            while (weight > 0 && actualPosition < bufferLength) {
                amountToNext = 1 + actualPosition - currentPosition
                if (weight >= amountToNext) {
                    for (channel in 0 until channels) {
                        outputVariableList[channel] += buffer[actualPosition++] * amountToNext
                    }
                    currentPosition = actualPosition.toFloat()
                    weight -= amountToNext
                } else {
                    for (channel in 0 until channels) {
                        outputVariableList[channel] += buffer[actualPosition + if (channel > 0) channel else 0] * weight
                    }
                    currentPosition += weight
                    weight = 0f
                    break
                }
            }

            if (weight <= 0f) {
                for (channel in 0 until channels) {
                    outputBuffer[outputOffset++] = outputVariableList[channel] / ratioWeight
                }
            } else {
                this.lastWeight = weight
                for (channel in 0 until channels) {
                    this.lastOutput[channel] = outputVariableList[channel]
                }
                this.tailExists = true
                break
            }
        }

        return bufferSlice(outputOffset)
    }

    private fun bufferSlice(sliceAmount: Int): FloatArray {
        return try {
            outputBuffer.copyOfRange(0, sliceAmount)
        }
        catch (e: Exception) {
            outputBuffer.slice(IntRange(0, sliceAmount)).toFloatArray()
        }
    }

    private fun linearInterpolation(buffer: FloatArray): FloatArray {
        var bufferLength = buffer.size

        if ((bufferLength % channels) != 0) {
            throw IllegalStateException("Buffer was of incorrect sample length")
        }
        if (bufferLength <= 0) {
            return FloatArray(0)
        }

        var weight = lastWeight
        var firstWeight: Float
        var secondWeight: Float
        var sourceOffset: Int
        var outputOffset = 0

        while (weight < 1) {
            secondWeight = weight % 1
            firstWeight = 1 - secondWeight
            lastWeight = weight % 1
            for (channel in 0 until this.channels) {
                outputBuffer[outputOffset++] = lastOutput[channel] * firstWeight + buffer[channel] * secondWeight
            }
            weight += ratioWeight
        }
        weight -= 1f

        bufferLength -= channels
        sourceOffset = floor(weight).toInt() * channels
        while (outputOffset < outputBufferSize && sourceOffset < bufferLength) {
            secondWeight = weight % 1
            firstWeight = 1 - secondWeight
            for (channel in 0 until this.channels) {
                val value = buffer[sourceOffset + if (channel > 0) channel else 0] * firstWeight + buffer[sourceOffset + (channels + channel)] * secondWeight
                outputBuffer[outputOffset++] = value
            }

            weight += ratioWeight
            sourceOffset = floor(weight).toInt() * channels
        }

        for (channel in 0 until channels) {
            lastOutput[channel] = buffer[sourceOffset++]
        }
        return bufferSlice(outputOffset)
    }

}