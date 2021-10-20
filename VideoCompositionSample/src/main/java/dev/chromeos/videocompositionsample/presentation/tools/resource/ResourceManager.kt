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

package dev.chromeos.videocompositionsample.presentation.tools.resource

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.content.res.Resources
import android.net.Uri
import androidx.annotation.ColorRes
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResourceManager
@Inject constructor(private val context: Context) : IResourceManager {

    override fun getString(@StringRes resId: Int): String {
        return context.getString(resId)
    }

    override fun getResources(): Resources {
        return context.resources
    }

    override fun getColor(@ColorRes colorId: Int): Int {
        return ContextCompat.getColor(context, colorId)
    }

    override fun getFileUri(absoluteFilePath: String): Uri? {
        val file =  File(absoluteFilePath)
        return if (file.exists()) {
            FileProvider.getUriForFile(
                context,
                context.applicationContext.packageName + ".provider",
                file
            )
        } else {
            null
        }
    }

    override fun getFileDescriptor(@RawRes resId: Int): AssetFileDescriptor? {
        return try {
            context.resources.openRawResourceFd(resId)
        } catch (e: Resources.NotFoundException) {
            e.printStackTrace()
            null
        }
    }

}