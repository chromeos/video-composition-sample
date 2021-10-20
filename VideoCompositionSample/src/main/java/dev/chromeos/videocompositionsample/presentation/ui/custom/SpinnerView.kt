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
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.widget.AppCompatImageView
import dev.chromeos.videocompositionsample.presentation.R

class SpinnerView
@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : AppCompatImageView(context, attrs, defStyleAttr) {

    private val defaultAnimationResId = R.anim.spinner_rotate_anim
    private var anim: Animation? = null
    private var isRunning: Boolean = false

    init {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {

        if (isInEditMode) return

        if (attrs != null) {
            val a = context.theme.obtainStyledAttributes(attrs, R.styleable.SpinnerView, 0, 0)
            try {
                val id = a.getResourceId(R.styleable.SpinnerView_swAnimation, 0)
                if (id != 0) anim = AnimationUtils.loadAnimation(context, id)
            } finally {
                a.recycle()
            }
        }

        if (anim == null) {
            anim = AnimationUtils.loadAnimation(context, defaultAnimationResId)
        }

        start()
    }

    fun stop() {
        if (!isRunning) return
        clearAnimation()
        visibility = View.GONE
        isRunning = false
    }

    fun start() {
        if (isRunning) return
        visibility = View.VISIBLE
        startAnimation(anim)
        isRunning = true
    }
}
