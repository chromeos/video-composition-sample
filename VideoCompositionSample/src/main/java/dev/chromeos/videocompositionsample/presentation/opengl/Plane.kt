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

import java.nio.ByteBuffer
import java.nio.ByteOrder

class Plane {

    companion object {
        const val STARTING_INDEX = 0
        const val NUMBER_OF_INDICES = 4
        private const val FLOAT_SIZE_BYTES = 4
        const val TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES
        const val TRIANGLE_VERTICES_DATA_POS_OFFSET = 0
        const val TRIANGLE_VERTICES_DATA_UV_OFFSET = 3
    }

    private val positionArr = floatArrayOf(
            // X, Y, Z, U, V
            -1.0f, -1.0f, 0f, 0f, 1f,
            1.0f, -1.0f, 0f, 1f, 1f,
            -1.0f, 1.0f, 0f, 0f, 0f,
            1.0f, 1.0f, 0f, 1f, 0f
    )

    val positionBuffer = ByteBuffer
            .allocateDirect(positionArr.size * FLOAT_SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(positionArr)
            .position(0)
}