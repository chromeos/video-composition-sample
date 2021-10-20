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

package dev.chromeos.videocompositionsample.presentation.ui.screens.player.adapter

import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import dev.chromeos.videocompositionsample.presentation.R
import dev.chromeos.videocompositionsample.presentation.ui.base.BaseRecyclerViewAdapter
import dev.chromeos.videocompositionsample.presentation.ui.model.SummaryModel
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.clip_summary_item.*

class ClipSummaryAdapter(dataList: MutableList<SummaryModel> = ArrayList())
    : BaseRecyclerViewAdapter<SummaryModel>(dataList, R.layout.clip_summary_item, null) {

    override fun getViewHolder(view: View): RecyclerView.ViewHolder {
        return Holder(view)
    }

    inner class Holder(override val containerView: View)
        : RecyclerView.ViewHolder(containerView), LayoutContainer {

        fun bind(data: SummaryModel) {
            val clipSummary = "${containerView.context.getString(data.nameResId)}, effect: ${data.effectId}"
            tvClipSummary.text = clipSummary

            if (data.isLoadSuccess) {
                tvClipSummaryState.text = containerView.context.getString(R.string.OK)
                tvClipSummaryState.setTextColor(ContextCompat.getColor(containerView.context, R.color.white))
            } else {
                tvClipSummaryState.text = containerView.context.getString(R.string.label__error)
                tvClipSummaryState.setTextColor(ContextCompat.getColor(containerView.context, R.color.red))
            }

            if (data.fpsAverage == 0f) {
                tvFpsAverage.text = ""
            } else {
                tvFpsAverage.text = data.fpsAverage.toString()
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: SummaryModel, position: Int) {
        (holder as Holder).bind(item)
    }
}