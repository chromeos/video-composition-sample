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

package dev.chromeos.videocompositionsample.presentation.media.encoder.base

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import dev.chromeos.videocompositionsample.presentation.media.encoder.Muxer
import java.io.IOException
import java.nio.ByteBuffer

class BaseEncoder(private val mimeType: String, format: MediaFormat, private val muxer: Muxer) {

    companion object {
        private const val TIMEOUT_USEC = 1000
    }

    var encoder: MediaCodec
    private var bufferInfo: MediaCodec.BufferInfo
    private var trackIndex = 0
    private var isEndOfStream = false

    init {
        try {
            encoder = MediaCodec.createEncoderByType(mimeType)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            bufferInfo = MediaCodec.BufferInfo()
        } catch (e: IOException) {
            throw IllegalStateException("Error init encoder of mimeType=${mimeType}", e)
        }
    }

    fun drain(endOfStream: Boolean) {
        if (endOfStream) {
            isEndOfStream = true
            encoder.signalEndOfInputStream()
        }
        while (true) {
            val encoderBufferId = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC.toLong())
            if (encoderBufferId == MediaCodec.INFO_TRY_AGAIN_LATER && !endOfStream) {
                break

            } else if (encoderBufferId == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                Log.w("BaseEncoder:", "encoder output buffers changed")
            } else if (encoderBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED && !muxer.isStarted) {
                trackIndex = muxer.addTrack(encoder.outputFormat)

            } else if (encoderBufferId < 0) {
                // let's ignore it
                Log.w("BaseEncoder:", "something occured during encoding")

            } else {
                val encodedData: ByteBuffer? = encoder.getOutputBuffer(encoderBufferId)
                if (encodedData != null) {
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        bufferInfo.size = 0
                    }
                    if (bufferInfo.size != 0 && muxer.isStarted) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(encoderBufferId, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        if (!endOfStream) {
                            Log.w("BaseEncoder:", "reached end of stream unexpectedly")
                        } else {
                            release()
                        }
                        break
                    }
                }
            }
        }
    }

    fun release() {
        try {
            encoder.stop()
        } catch (e: Exception) {
            throw IllegalStateException("Error stop encoder of mimeType=${mimeType}")
        }
        encoder.release()
    }
}