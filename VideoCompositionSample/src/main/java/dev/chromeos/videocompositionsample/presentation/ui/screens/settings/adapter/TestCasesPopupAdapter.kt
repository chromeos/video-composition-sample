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
import dev.chromeos.videocompositionsample.presentation.R
import dev.chromeos.videocompositionsample.presentation.ui.base.BaseRecyclerViewAdapter
import dev.chromeos.videocompositionsample.presentation.ui.model.SettingsTestCaseModel
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.test_case_popup_item.*

class TestCasesPopupAdapter(var dataList: MutableList<SettingsTestCaseModel> = ArrayList(),
                            var onTestCaseClickListener: ((Int, Boolean) -> Unit)? = null,
                            var onExportClickListener: ((Int, Boolean) -> Unit)? = null)
    : BaseRecyclerViewAdapter<SettingsTestCaseModel>(dataList, R.layout.test_case_popup_item, null) {

    override fun getViewHolder(view: View): RecyclerView.ViewHolder {
        return Holder(view)
    }

    inner class Holder(override val containerView: View)
        : RecyclerView.ViewHolder(containerView), LayoutContainer {

        fun bind(data: SettingsTestCaseModel, position: Int) {
            tvPosition.text = (position + 1).toString()
            tvClipSetValues.text = data.settings.trackList.joinToString(separator = "-") { it.clip.clipId.toString() }
            tvEffectSetValues.text = data.settings.trackList.joinToString(separator = "-") { it.effect.effectId.toString() }
            chbTestCase.isChecked = data.isSelected
            chbExport.isChecked = data.isExportEnabled
            chbTestCase.setOnClickListener {
                dataList[position].isSelected = !dataList[position].isSelected
                onTestCaseClickListener?.invoke(position, dataList[position].isSelected)
            }
            chbExport.setOnClickListener {
                dataList[position].isExportEnabled = !dataList[position].isExportEnabled
                onExportClickListener?.invoke(position, dataList[position].isExportEnabled)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: SettingsTestCaseModel, position: Int) {
        (holder as Holder).bind(item, position)
    }
}