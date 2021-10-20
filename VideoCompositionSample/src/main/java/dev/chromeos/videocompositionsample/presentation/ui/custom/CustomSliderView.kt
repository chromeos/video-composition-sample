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

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.RelativeLayout
import android.widget.TextView
import com.google.android.material.slider.Slider
import dev.chromeos.videocompositionsample.presentation.R
import dev.chromeos.videocompositionsample.presentation.tools.extensions.toTimeLineString
import dev.chromeos.videocompositionsample.presentation.tools.extensions.toTwoDecimalString
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class CustomSliderView
@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : RelativeLayout(context, attrs, defStyleAttr) {

    private val dragSubject = PublishSubject.create<Float>()
    private val stopDragSubject = PublishSubject.create<Unit>()

    private lateinit var slider: Slider
    private lateinit var tvTitle: TextView
    private lateinit var tvValue: TextView

    private var isTimeLineStyle: Boolean = false
    var timeLineFps = 30

    var isDragging = false

    var isDraggable: Boolean = true
        set(value) {
            field = value
            slider.isEnabled = field
        }

    var valueFrom: Float = 0f
        set(value) {
            field = value
            slider.valueFrom = field
        }

    var valueTo: Float = 1f
        set(value) {
            field = value
            slider.valueTo = field
        }

    init {
        init(attrs)
    }

    var value: Float = 0f
        set(value) {
            field = value
            if (!isDragging) {
                slider.value = field
            }
        }

    @SuppressLint("ClickableViewAccessibility")
    private fun init(attrs: AttributeSet? = null) {

        val root = LayoutInflater.from(context).inflate(R.layout.custom_slider_view, this, true)
        tvTitle = root.findViewById(R.id.tvTitle)
        tvValue = root.findViewById(R.id.tvValue)
        slider = root.findViewById(R.id.slider)

        val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.CustomSliderView, 0, 0)
        try {
            isTimeLineStyle = typedArray.getBoolean(R.styleable.CustomSliderView_isTimeLineStyle, false)
            timeLineFps = typedArray.getInt(R.styleable.CustomSliderView_timeLineFps, 30)
            tvTitle.text = typedArray.getString(R.styleable.CustomSliderView_title) ?: ""
            slider.valueFrom = typedArray.getFloat(R.styleable.CustomSliderView_valueFrom, 0f)
            slider.valueTo = typedArray.getFloat(R.styleable.CustomSliderView_valueTo, 1f)

        } finally {
            typedArray.recycle()
        }

        slider.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_UP) {
                Handler().postDelayed({
                    isDragging = false
                    stopDragSubject.onNext(Unit)
                }, 100)
            }
            false
        }

        slider.addOnChangeListener { slider, value, fromUser ->
            isDragging = fromUser
            if (fromUser) {
                dragSubject.onNext(value)
            }
            tvValue.text = if (isTimeLineStyle) {
                slider.value.toTimeLineString(timeLineFps)
            } else {
                slider.value.toTwoDecimalString()
            }
        }

        tvValue.text = if (isTimeLineStyle) {
            slider.setLabelFormatter { it.toTimeLineString(timeLineFps) }
            slider.value.toTimeLineString(timeLineFps)
        } else {
            slider.value.toTwoDecimalString()
        }
    }

    fun dragChanges(): Observable<Float> {
        return dragSubject
    }

    fun stopDrags(): Observable<Unit> {
        return stopDragSubject
    }
}
