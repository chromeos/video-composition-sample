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

package dev.chromeos.videocompositionsample.presentation.media.encoder.utils

import java.nio.ByteBuffer

enum class VideoMimeType(val mimeType: String) {
    h264("video/avc"),
    h265("video/hevc")
}

fun getVideoOptimalBitrate(videoMimeType: VideoMimeType, width: Int, height: Int): Int {
    val size = width * height
    val bitrate = when {
        size <= 640 * 480 -> 1000000
        size <= 1280 * 720 -> 2000000
        size <= 1920 * 1080 -> 7000000
        else -> 10000000
    }
    return when (videoMimeType) {
        VideoMimeType.h264 -> bitrate
        VideoMimeType.h265 -> bitrate / 2
    }
}

fun deepCopy(source: ByteBuffer): ByteBuffer {
    val sourceP = source.position()
    val sourceL = source.limit()
    val target = ByteBuffer.allocate(source.remaining())
    target.put(source)
    target.position(sourceP)
    target.limit(sourceL)


    source.position(sourceP)
    source.limit(sourceL)
    return target
}

/**
 * @param size range [0..size] to remove from ByteBuffer
 * @param byteBuffer ByteBuffer where all data over size position shift to the begin
 * @return ByteBuffer of collapsed data
 */
fun collapseBufferBeginRange(size: Int, byteBuffer: ByteBuffer): ByteBuffer {
    val position = byteBuffer.position()
    val limit = byteBuffer.limit()
//    if (position < size) {
//        position = size
//    }
    val outRangeBuffer = ByteBuffer.allocate(size)
    byteBuffer.position(0)
    byteBuffer.limit(size)
    outRangeBuffer.put(byteBuffer)
    outRangeBuffer.flip()
    byteBuffer.limit(limit)

    val data = byteBuffer.duplicate()
    data.limit(byteBuffer.limit())
    data.position(position)
    byteBuffer.position(0)
    byteBuffer.put(data)
    byteBuffer.position(position - size)

    return outRangeBuffer
}