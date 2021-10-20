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

package dev.chromeos.videocompositionsample.presentation.ui.screens.settings.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import dev.chromeos.videocompositionsample.domain.entity.Effect
import dev.chromeos.videocompositionsample.presentation.R
import dev.chromeos.videocompositionsample.presentation.ui.base.BaseRecyclerViewAdapter
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.effect_popup_item.*

class EffectPopupAdapter(dataList: MutableList<Effect> = ArrayList(),
                         callback: ((Effect) -> Unit)? = null)
    : BaseRecyclerViewAdapter<Effect>(dataList, R.layout.effect_popup_item, callback) {

    override fun getViewHolder(view: View): RecyclerView.ViewHolder {
        return Holder(view)
    }

    inner class Holder(override val containerView: View)
        : RecyclerView.ViewHolder(containerView), LayoutContainer {

        fun bind(data: Effect) {
            tvEffect.text = containerView.context.getString(R.string.settings__Effect, data.effectId)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: Effect, position: Int) {
        (holder as Holder).bind(item)
    }
}