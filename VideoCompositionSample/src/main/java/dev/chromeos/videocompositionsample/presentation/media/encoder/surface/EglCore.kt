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

import android.graphics.SurfaceTexture
import android.opengl.*
import android.util.Log
import android.view.Surface

class EglCore
@JvmOverloads constructor(sharedContext: EGLContext? = null, flags: Int = 0) {

    companion object {
        private const val TAG = "EglCore"
        const val FLAG_RECORDABLE = 0x01
        const val FLAG_TRY_GLES3 = 0x02
        private const val EGL_RECORDABLE_ANDROID = 0x3142
    }

    private var eglDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext = EGL14.EGL_NO_CONTEXT
    private var eglConfig: EGLConfig? = null
    private var glVersion = -1

    init {
        var eglSharedContext = sharedContext
        if (eglDisplay !== EGL14.EGL_NO_DISPLAY) {
            throw RuntimeException("EGL already set up")
        }
        if (eglSharedContext == null) {
            eglSharedContext = EGL14.EGL_NO_CONTEXT
        }
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay === EGL14.EGL_NO_DISPLAY) {
            throw RuntimeException("unable to get EGL14 display")
        }
        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            eglDisplay = null
            throw RuntimeException("unable to initialize EGL14")
        }

        // Try to get a GLES3 context, if requested.
        if (flags and FLAG_TRY_GLES3 != 0) {
            val config = getConfig(flags, 3)
            if (config != null) {
                val attr3List = intArrayOf(
                        EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
                        EGL14.EGL_NONE
                )
                val context = EGL14.eglCreateContext(eglDisplay, config, eglSharedContext, attr3List, 0)
                if (EGL14.eglGetError() == EGL14.EGL_SUCCESS) {
                    eglConfig = config
                    eglContext = context
                    glVersion = 3
                }
            }
        }
        if (eglContext === EGL14.EGL_NO_CONTEXT) {  // GLES 2 only, or GLES 3 attempt failed
            val config = getConfig(flags, 2) ?: throw RuntimeException("Unable to find a suitable EGLConfig")
            val attr2List = intArrayOf(
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                    EGL14.EGL_NONE
            )
            val context = EGL14.eglCreateContext(eglDisplay, config, eglSharedContext, attr2List, 0)
            checkEglError("eglCreateContext")
            eglConfig = config
            eglContext = context
            glVersion = 2
        }

        val values = IntArray(1)
        EGL14.eglQueryContext(eglDisplay, eglContext, EGL14.EGL_CONTEXT_CLIENT_VERSION, values, 0)
    }

    private fun getConfig(flags: Int, version: Int): EGLConfig? {
        var renderType = EGL14.EGL_OPENGL_ES2_BIT
        if (version >= 3) {
            renderType = renderType or EGLExt.EGL_OPENGL_ES3_BIT_KHR
        }
        val attrList = intArrayOf(
//                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
//                EGL14.EGL_RED_SIZE, 8,
//                EGL14.EGL_GREEN_SIZE, 8,
//                EGL14.EGL_BLUE_SIZE, 8,
//                EGL14.EGL_ALPHA_SIZE, 8,
//                EGL14.EGL_DEPTH_SIZE, 0,
//                EGL14.EGL_CONFIG_CAVEAT, EGL14.EGL_NONE,
//                EGL14.EGL_SURFACE_TYPE,  EGL14.EGL_WINDOW_BIT or EGL14.EGL_PBUFFER_BIT,
//                EGL14.EGL_NONE
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,  //EGL14.EGL_DEPTH_SIZE, 16,
                //EGL14.EGL_STENCIL_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, renderType,
                EGL14.EGL_NONE, 0,  // placeholder for recordable [@-3]
                EGL14.EGL_NONE
        )
        if (flags and FLAG_RECORDABLE != 0) {
            attrList[attrList.size - 3] = EGL_RECORDABLE_ANDROID
            attrList[attrList.size - 2] = 1
        }
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(eglDisplay, attrList, 0, configs, 0, configs.size, numConfigs, 0)) {
            return null
        }
        return configs[0]
    }

    fun release() {
        if (eglDisplay !== EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(eglDisplay)
        }
        eglDisplay = EGL14.EGL_NO_DISPLAY
        eglContext = EGL14.EGL_NO_CONTEXT
        eglConfig = null
    }

    @Throws(Throwable::class)
    protected fun finalize() {
        try {
            if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun releaseSurface(eglSurface: EGLSurface?) {
        EGL14.eglDestroySurface(eglDisplay, eglSurface)
    }

    fun createWindowSurface(surface: Any): EGLSurface {
        if (surface !is Surface && surface !is SurfaceTexture) {
            throw RuntimeException("invalid surface: $surface")
        }

        val surfaceAttrs = intArrayOf(
                EGL14.EGL_NONE
        )
        val eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surface, surfaceAttrs, 0)
        checkEglError("eglCreateWindowSurface")
        if (eglSurface == null) {
            throw RuntimeException("surface was null")
        }
        return eglSurface
    }

    fun makeCurrent(eglSurface: EGLSurface?) {
        if (eglDisplay === EGL14.EGL_NO_DISPLAY) {
            Log.d(TAG, "NOTE: makeCurrent w/o display")
        }
        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            throw RuntimeException("eglMakeCurrent failed")
        }
    }

    fun swapBuffers(eglSurface: EGLSurface?): Boolean {
        return EGL14.eglSwapBuffers(eglDisplay, eglSurface)
    }

    fun setPresentationTime(eglSurface: EGLSurface?, nsecs: Long) {
        EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, nsecs)
    }

    fun querySurface(eglSurface: EGLSurface?, what: Int): Int {
        val value = IntArray(1)
        EGL14.eglQuerySurface(eglDisplay, eglSurface, what, value, 0)
        return value[0]
    }

    private fun checkEglError(msg: String) {
        var error: Int
        if (EGL14.eglGetError().also { error = it } != EGL14.EGL_SUCCESS) {
            throw RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error))
        }
    }

}