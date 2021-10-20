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
import android.opengl.GLES30
import android.util.Log
import java.io.IOException
import java.util.*

class Shader(private val context: Context, vertexShaderFileName: String, fragmentShaderFileName: String) {

    private var vertexShaderId: Int = 0
    private var fragmentShaderId: Int = 0
    var programId: Int = 0

    private var errorCode: IntArray? = null

    init {
        errorCode = IntArray(1)
        try {
            vertexShaderId = loadShaderCode(GLES30.GL_VERTEX_SHADER, vertexShaderFileName)
            fragmentShaderId = loadShaderCode(GLES30.GL_FRAGMENT_SHADER, fragmentShaderFileName)
            programId = GLES30.glCreateProgram()
            if (programId != 0) {
                GLES30.glAttachShader(programId, vertexShaderId)
                GLES30.glAttachShader(programId, fragmentShaderId)
                GLES30.glLinkProgram(programId)
                GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, errorCode, 0)
                if (errorCode !![0] != GLES30.GL_TRUE) {
                    GLES30.glDeleteProgram(programId)
                    throw IllegalStateException("Shader link error: " + GLES30.glGetProgramInfoLog(programId))
                }
            } else
                throw IllegalStateException("Can't create a shader program")
        } catch (e: Exception) {
            Log.e(javaClass.name, "Caught exception: " + e.message)
        }
    }

    private fun loadShaderCode(type: Int, fileName: String): Int {
        val handle: Int
        try {
            val inp = Scanner(context.assets.open(fileName))
            val code = StringBuffer()
            while (inp.hasNextLine())
                code.append(inp.nextLine() + "\n")
            inp.close()
            handle = GLES30.glCreateShader(type)
            if (handle != 0) {
                GLES30.glShaderSource(handle, code.toString())
                GLES30.glCompileShader(handle)

                GLES30.glGetShaderiv(handle, GLES30.GL_COMPILE_STATUS, errorCode, 0)
                if (errorCode !![0] != 0)
                    return handle
                else {
                    Log.e("Shader", "Shader compile error: " + GLES30.glGetShaderInfoLog(handle))
                    GLES30.glDeleteShader(handle)
                    throw IllegalStateException("Shader compile error in $fileName")
                }
            } else {
                throw IllegalStateException("Failed to create a shader for $fileName")
            }
        } catch (e: IOException) {
            throw IllegalArgumentException(
                    "Shader I/O exception while loading " + fileName + ": "
                            + e.message)
        }
    }

}