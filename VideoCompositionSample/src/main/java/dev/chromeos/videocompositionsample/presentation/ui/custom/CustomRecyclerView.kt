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
import android.view.MotionEvent
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView

class CustomRecyclerView
@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : RecyclerView(context, attrs, defStyleAttr) {

    private var externalNestedScrollView: NestedScrollView? = null

    var isEnabledExternalNestedScroll = false

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabledExternalNestedScroll) {
            if (externalNestedScrollView == null) {
                externalNestedScrollView = getParentAsNestedScrollView()
            }
            if (event.action == MotionEvent.ACTION_DOWN) {
                externalNestedScrollView?.requestDisallowInterceptTouchEvent(true)
            } else if (event.action == MotionEvent.ACTION_UP) {
                externalNestedScrollView?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return super.onInterceptTouchEvent(event)
    }

    private fun getParentAsNestedScrollView(): NestedScrollView? {
        var curParent = parent
        while (curParent != null) {
            if (curParent is NestedScrollView) {
                return curParent
            } else {
                curParent = curParent.parent
            }
        }
        return null
    }
}