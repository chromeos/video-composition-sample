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
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import dev.chromeos.videocompositionsample.presentation.R
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class PlayerControlView
@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {

    private lateinit var ivToBegin: ImageView
    private lateinit var ivSkipBackward: ImageView
    private lateinit var ivPlayPause: ImageView
    private lateinit var ivSkipForward: ImageView
    private lateinit var ivToEnd: ImageView

    private var listener: OnClickListener? = null
    private var isPlayState = false

    interface OnClickListener {
        fun onToBeginClick()
        fun onPlayClick()
        fun onPauseClick()
        fun onSkipForward()
        fun onSkipBackward()
        fun onLongSkipForward()
        fun onLongSkipBackward()
        fun onToEndClick()
    }

    private val stopSkipSubject = PublishSubject.create<Unit>()
    var isSkipping = false

    var isPlayButtonEnabled = true
        set(value) {
            ivPlayPause.isEnabled = value
            field = value
        }

    var isSkipButtonsEnabled = true
        set(value) {
            if (value) {
                ivToBegin.isEnabled = true
                ivSkipBackward.isEnabled = true
                ivSkipForward.isEnabled = true
                ivToEnd.isEnabled = true
            } else {
                ivToBegin.isEnabled = false
                ivSkipBackward.isEnabled = false
                ivSkipForward.isEnabled = false
                ivToEnd.isEnabled = false
            }
            field = value
        }

    init {
        init()
    }

    fun setOnClickListener(listener: OnClickListener) {
        this.listener = listener
    }

    @SuppressLint("ClickableViewAccessibility")
    fun init() {

        val root = LayoutInflater.from(context).inflate(R.layout.player_control_view, this, true)

        ivToBegin = root.findViewById(R.id.ivToBegin)
        ivSkipBackward = root.findViewById(R.id.ivSkipBackward)
        ivPlayPause = root.findViewById(R.id.ivPlayPause)
        ivSkipForward = root.findViewById(R.id.ivSkipForward)
        ivToEnd = root.findViewById(R.id.ivToEnd)

        ivToBegin.setOnClickListener { listener?.onToBeginClick() }

        ivSkipForward.setOnClickListener { listener?.onSkipForward() }
        ivSkipForward.setOnLongClickListener { v ->
            ivSkipForward.setOnClickListener(null)
            isSkipping = true
            listener?.onLongSkipForward()
            ivSkipForward.setOnTouchListener { view, motionEvent ->
                if (motionEvent.action == MotionEvent.ACTION_UP) {
                    Handler().postDelayed({
                        ivSkipForward.setOnTouchListener { _, _ ->  false}
                        ivSkipForward.setOnClickListener { listener?.onSkipForward() }
                        isSkipping = false
                        stopSkipSubject.onNext(Unit)
                    }, 100)
                }
                false
            }
            false
        }

        ivSkipBackward.setOnClickListener { listener?.onSkipBackward() }
        ivSkipBackward.setOnLongClickListener {  v ->
            ivSkipBackward.setOnClickListener(null)
            isSkipping = true
            listener?.onLongSkipBackward()
            ivSkipBackward.setOnTouchListener { view, motionEvent ->
                if (motionEvent.action == MotionEvent.ACTION_UP) {
                    Handler().postDelayed({
                        ivSkipBackward.setOnTouchListener { _, _ ->  false}
                        ivSkipBackward.setOnClickListener { listener?.onSkipBackward() }
                        isSkipping = false
                        stopSkipSubject.onNext(Unit)
                    }, 100)
                }
                false
            }
            false
        }

        ivPlayPause.setOnClickListener {
            if (isPlayState) {
                listener?.onPauseClick()
            } else {
                listener?.onPlayClick()
            }
        }

        ivToEnd.setOnClickListener { listener?.onToEndClick() }
    }

    fun setPauseState() {
        ivPlayPause.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        isPlayState = false
    }

    fun setPlayState() {
        ivPlayPause.setImageResource(R.drawable.ic_baseline_pause_24)
        isPlayState = true
    }

    fun stopSkips(): Observable<Unit> {
        return stopSkipSubject
    }
}
