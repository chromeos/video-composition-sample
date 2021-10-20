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

package dev.chromeos.videocompositionsample.presentation.tools.info

import android.content.Context
import androidx.annotation.StringRes
import dev.chromeos.videocompositionsample.presentation.R

class MessageModel(private val message: String? = "",
                 @StringRes private val messageResId: Int? = 0,
                 val callbackDetails: CallbackDetails? = null
) {

    fun getMessage(context: Context): String {
        return if (message.isNullOrEmpty()) {
            var message = ""

            if (messageResId != null && messageResId > 0) {
                message = try {
                    context.getString(messageResId)
                } catch (e: Exception) {
                    this.message ?: ""
                }
            }

            if (message.isEmpty())
                context.getString(R.string.messages__no_comments)
            else
                message
        } else {
            message
        }
    }

    data class CallbackDetails(
            val positiveButtonText: String? = null,
            val negativeButtonText: String? = null,
            val positiveKey: String? = null,
            val negativeKey: String? = null
    )
}