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
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import dev.chromeos.videocompositionsample.presentation.R

class ResolutionView
@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : LinearLayout(context, attrs, defStyleAttr) {

    enum class Resolution(val width: Int, val height: Int) {
        Eighth(480, 270),
        Quarter(960, 540),
        Half(1920, 1080),
        Full(3840, 2160)
    }

    private lateinit var tvEighth: TextView
    private lateinit var tvQuarter: TextView
    private lateinit var tvHalf: TextView
    private lateinit var tvFull: TextView

    private val checkedBackgroundResId: Int = R.drawable.rect_round_white_80

    private var listener: OnResolutionCheckListener? = null

    interface OnResolutionCheckListener {
        fun onEighthChecked()
        fun onQuarterChecked()
        fun onHalfChecked()
        fun onFullChecked()
    }

    var resolution: Resolution = Resolution.Full
        set(value) {
            if (value != field) {
                field = value
                when (value) {
                    Resolution.Eighth -> tvEighth.performClick()
                    Resolution.Quarter -> tvQuarter.performClick()
                    Resolution.Half -> tvHalf.performClick()
                    Resolution.Full -> tvFull.performClick()
                }
            }
        }

    init {
        init()
    }

    fun setOnResolutionCheckListener(listener: OnResolutionCheckListener) {
        this.listener = listener
    }

    private fun init() {

        val root = LayoutInflater.from(context).inflate(R.layout.resolution_view, this, true)
        tvEighth = root.findViewById(R.id.tvEighth)
        tvQuarter = root.findViewById(R.id.tvQuarter)
        tvHalf = root.findViewById(R.id.tvHalf)
        tvFull = root.findViewById(R.id.tvFull)

        tvEighth.setOnClickListener {
            setBackgroundRes(checkedBackgroundResId, 0, 0, 0)
            resolution = Resolution.Eighth
            listener?.onEighthChecked()
        }

        tvQuarter.setOnClickListener {
            setBackgroundRes(0, checkedBackgroundResId, 0, 0)
            resolution = Resolution.Quarter
            listener?.onQuarterChecked()
        }

        tvHalf.setOnClickListener {
            setBackgroundRes(0, 0, checkedBackgroundResId, 0)
            resolution = Resolution.Half
            listener?.onHalfChecked()
        }

        tvFull.setOnClickListener {
            setBackgroundRes(0, 0, 0, checkedBackgroundResId)
            resolution = Resolution.Full
            listener?.onFullChecked()
        }

        setBackgroundRes(0, 0, 0, checkedBackgroundResId)
    }

    private fun setBackgroundRes(eighthResId: Int, quarterResId: Int, halfResId: Int, fullResId: Int) {
        tvEighth.setBackgroundResource(eighthResId)
        tvQuarter.setBackgroundResource(quarterResId)
        tvHalf.setBackgroundResource(halfResId)
        tvFull.setBackgroundResource(fullResId)
    }

}