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

package dev.chromeos.videocompositionsample.presentation.tools.extensions

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * @return destination absolute file path
 */
@Suppress("DEPRECATION")
fun Context.saveToVideoGallery(sourceFilePath: String, fileName: String): String {
    val folderName = "Composition"
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val context = this.applicationContext
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + File.separator + folderName)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val fileUri = context.contentResolver.insert(collection, values)

        fileUri?.let {
            context.contentResolver.openFileDescriptor(fileUri, "w").use { descriptor ->
                descriptor?.let {
                    FileOutputStream(descriptor.fileDescriptor).use { out ->
                        val videoFile = File(sourceFilePath)
                        FileInputStream(videoFile).use { inputStream ->
                            val buf = ByteArray(8192)
                            while (true) {
                                val sz = inputStream.read(buf)
                                if (sz <= 0) break
                                out.write(buf, 0, sz)
                            }
                        }
                    }
                }
            }
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            context.contentResolver.update(fileUri, values, null, null)
        }
        fileUri?.path?: ""
    } else {
        val mediaDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).absolutePath
        val fileDir = File(mediaDir)
        if (!fileDir.exists()) fileDir.mkdirs()
        val absoluteFilePath = mediaDir + File.separator + fileName

        val dstFile = File(absoluteFilePath)
        File(sourceFilePath).copyTo(dstFile, true)

        try {
            val values = ContentValues(6)
            values.put(MediaStore.Video.Media.TITLE, dstFile.name)
            values.put(MediaStore.Video.Media.DISPLAY_NAME, dstFile.name)
            values.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis())
            values.put(MediaStore.Video.Media.DATA, dstFile.absolutePath)
            contentResolver?.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        absoluteFilePath
    }
}

fun Context.getCacheDirFilePath(fileName: String): String {
    return cacheDir.absolutePath.toString() + File.separator + fileName
}

fun Context.getExternalStorageFilePath(fileName: String): String {
    return getExternalFilesDir(null)?.absolutePath.toString() + File.separator + fileName
}