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

package dev.chromeos.videocompositionsample.data.repository.base

import android.content.Context
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import dev.chromeos.videocompositionsample.domain.repository.base.IBasePrefRepo

open class BasePrefRepo<T>(private val context: Context?, private val key: String, private val clazz: Class<T>)
    : IBasePrefRepo<T> {

    override fun put(value: T?) {
        val json = Gson().toJson(value)
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        prefEditor.putString(key, json)
        prefEditor.apply()
    }

    override fun get(): T? {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val json = pref.getString(key, null)
        return try {
            if (json.isNullOrBlank()) null else Gson().fromJson(json, clazz)
        } catch (e: Exception) {
            null
        }
    }

    override fun clear() {
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        prefEditor.putString(key, null)
        prefEditor.apply()
    }
}
