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

package dev.chromeos.videocompositionsample.presentation.ui.custom

import android.content.Context
import android.text.Html
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import dev.chromeos.videocompositionsample.presentation.R

class CustomTextView
@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : AppCompatTextView(context, attrs, defStyleAttr) {

    init {
        setCustomText(context, attrs)
    }

    var html: String? = null
        set(value) {
            field = value
            text = if (value == null) {
                null
            } else {
                getTextFromHtml(value)
            }
        }

    private fun getTextFromHtml(html: String?): CharSequence? {
        return if (html != null && text != null) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            null
        }
    }

    private fun setCustomText(context: Context?, attrs: AttributeSet?) {

        if (context != null && attrs != null) {

            val ta = context.obtainStyledAttributes(attrs, R.styleable.CustomTextView)

            val isHtml = ta.getBoolean(R.styleable.CustomTextView_isHtml, false)
            if (isHtml) {
                val html = ta.getString(R.styleable.CustomTextView_android_text)
                text = getTextFromHtml(html)
            }

            ta.recycle()
        }
    }
}