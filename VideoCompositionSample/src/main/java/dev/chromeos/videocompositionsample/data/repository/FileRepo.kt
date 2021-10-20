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

package dev.chromeos.videocompositionsample.data.repository

import android.content.Context
import dev.chromeos.videocompositionsample.domain.repository.IFileRepo
import io.reactivex.Single
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import javax.inject.Inject

class FileRepo
@Inject constructor(private val context: Context) : IFileRepo {

    override fun save(byteArray: ByteArray, fileName: String): Single<String> {
        return Single.fromCallable {
            val dirPath: String = context.filesDir.absolutePath.toString()
            val fileDir = File(dirPath)
            if (!fileDir.exists()) fileDir.mkdirs()

            val filePath = dirPath + File.separator + fileName
            val file =  File(filePath)
            file.createNewFile()
            if (file.exists()) {
                val fo: OutputStream = FileOutputStream(file)
                fo.write(byteArray)
                fo.close()
                filePath
            } else {
                throw IOException("File creation error: ${dirPath + File.separator + fileName}")
            }
        }
    }

}