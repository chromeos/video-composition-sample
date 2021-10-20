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

package dev.chromeos.videocompositionsample.presentation.media.encoder.surface

import android.opengl.EGL14
import android.util.Log

open class EglSurfaceBase protected constructor(protected var eglCore: EglCore) {

    companion object {
        protected const val TAG = "EglSurfaceBase"
    }

    private var eglSurface = EGL14.EGL_NO_SURFACE
    private var mWidth = -1
    private var mHeight = -1

    fun createWindowSurface(surface: Any) {
        check(!(eglSurface !== EGL14.EGL_NO_SURFACE)) { "surface already created" }
        eglSurface = eglCore.createWindowSurface(surface)
    }

    val width: Int
        get() = if (mWidth < 0) {
            eglCore.querySurface(eglSurface, EGL14.EGL_WIDTH)
        } else {
            mWidth
        }

    val height: Int
        get() = if (mHeight < 0) {
            eglCore.querySurface(eglSurface, EGL14.EGL_HEIGHT)
        } else {
            mHeight
        }

    fun releaseEglSurface() {
        eglCore.releaseSurface(eglSurface)
        eglSurface = EGL14.EGL_NO_SURFACE
        mHeight = -1
        mWidth = -1
    }

    fun makeCurrent() {
        eglCore.makeCurrent(eglSurface)
    }

    fun swapBuffers(): Boolean {
        val result = eglCore.swapBuffers(eglSurface)
        if (!result) {
            Log.d(TAG, "WARNING: swapBuffers() failed")
        }
        return result
    }

    fun setPresentationTime(nanoSecond: Long) {
        eglCore.setPresentationTime(eglSurface, nanoSecond)
    }

}