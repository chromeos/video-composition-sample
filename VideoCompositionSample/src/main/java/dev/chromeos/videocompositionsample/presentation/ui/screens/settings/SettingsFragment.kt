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

package dev.chromeos.videocompositionsample.presentation.ui.screens.settings

import android.app.Activity
import android.content.Intent
import android.graphics.Point
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.view.*
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.chromeos.videocompositionsample.presentation.R
import dev.chromeos.videocompositionsample.presentation.tools.extensions.putIntBuild
import dev.chromeos.videocompositionsample.presentation.ui.base.BaseFragment
import dev.chromeos.videocompositionsample.presentation.ui.base.IBackPressed
import dev.chromeos.videocompositionsample.presentation.ui.mapper.SettingsTestCaseModelMapper
import dev.chromeos.videocompositionsample.presentation.ui.mapper.TrackModelMapper
import dev.chromeos.videocompositionsample.presentation.ui.model.SettingsTestCaseModel
import dev.chromeos.videocompositionsample.presentation.ui.model.TrackModel
import dev.chromeos.videocompositionsample.presentation.ui.screens.settings.adapter.EffectPopupAdapter
import dev.chromeos.videocompositionsample.presentation.ui.screens.settings.adapter.SelectedClipAdapter
import dev.chromeos.videocompositionsample.presentation.ui.screens.settings.adapter.TemplateClipAdapter
import dev.chromeos.videocompositionsample.presentation.ui.screens.settings.adapter.TestCasesPopupAdapter
import dev.chromeos.videocompositionsample.presentation.ui.screens.settings.core.ISettingsView
import dev.chromeos.videocompositionsample.presentation.ui.screens.settings.core.SettingsPresenter
import dev.chromeos.videocompositionsample.domain.entity.*
import kotlinx.android.synthetic.main.fragment_settings.*
import java.util.*
import javax.inject.Inject

class SettingsFragment : BaseFragment(), ISettingsView, IBackPressed {

    companion object {
        private const val MAX_SELECTED_CLIP_COUNT = 10

        private const val ARGUMENT_REQUEST_CODE = "SettingsFragment.ARGUMENT_REQUEST_CODE"
        private const val EXTRA_RESULT_SETTINGS = "SettingsFragment.EXTRA_RESULT_SETTINGS"

        fun newInstance(requestCode: Int): Fragment {
            val fragment = SettingsFragment()
            val bundle = Bundle()
                    .putIntBuild(ARGUMENT_REQUEST_CODE, requestCode)

            fragment.arguments = bundle
            return fragment
        }

        fun getSettings(data: Intent): Settings? {
            return data.getSerializableExtra(EXTRA_RESULT_SETTINGS) as Settings?
        }
    }

    private val templateClipsAdapter: TemplateClipAdapter by lazy { TemplateClipAdapter() }
    private val selectedClipsAdapter: SelectedClipAdapter by lazy { SelectedClipAdapter() }
    private val effectPopupAdapter: EffectPopupAdapter by lazy { EffectPopupAdapter() }
    private val testCasesPopupAdapter: TestCasesPopupAdapter by lazy { TestCasesPopupAdapter() }

    private var popupWindow: PopupWindow? = null
    private var helpPopupWidth: Int = 0
    private val warningToast: Toast by lazy { Toast.makeText(requireContext(), null, Toast.LENGTH_SHORT) }

    private val trackModelMapper = TrackModelMapper()
    private val settingsTestCaseModelMapper = SettingsTestCaseModelMapper()
    private var selectedTestCaseIds: MutableList<Int> = mutableListOf()
    private var selectedExportCaseIds: MutableList<Int> = mutableListOf()

    @Inject lateinit var presenter: SettingsPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        component.inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.onAttach(this)
        presenter.getSettings()

        ivHelp.setOnClickListener {
            showHelpPopup()?.showAsDropDown(ivHelp, ivHelp.width - helpPopupWidth , 0)
        }

        ivClose.setOnClickListener {
            val requestCode = arguments?.getInt(ARGUMENT_REQUEST_CODE)
            requestCode?.let {
                targetFragment?.onActivityResult(it, Activity.RESULT_CANCELED, Intent())
            }
            fragmentManager?.popBackStack()
        }

        fabApply.setOnClickListener {
            if (selectedClipsAdapter.itemCount > 0) {
                applySettings()
            } else {
                warningToast.setText(R.string.error__settings_should_select_clip)
                warningToast.show()
            }
        }

        val templateList = listOf(
                Track(Clip.Video_3840x2160_30fps, Effect.Effect1),
                Track(Clip.Video_1080x1920_30fps, Effect.Effect2),
                Track(Clip.Video_3840x2160_60fps, Effect.Effect3),
                Track(Clip.Video_1920x1080_120fps, Effect.Effect4)
        )
        templateClipsAdapter.add(trackModelMapper.mapToModel(templateList))

        val itemDecorator = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        ContextCompat.getDrawable(requireContext(), R.drawable.divider)?.let {
            itemDecorator.setDrawable(it)
        }
        rvTemplateClips.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = templateClipsAdapter
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
            addItemDecoration(itemDecorator)
        }
        templateClipsAdapter.onItemClickListener = { clipModel ->
            if (selectedClipsAdapter.itemCount < MAX_SELECTED_CLIP_COUNT) {
                val model = clipModel.copy(uuid = UUID.randomUUID().toString())
                selectedClipsAdapter.add(model)
                if (selectedClipsAdapter.itemCount > templateClipsAdapter.itemCount) {
                    rvSelectedClips.isEnabledExternalNestedScroll = false
                }
                rvSelectedClips.smoothScrollToPosition(selectedClipsAdapter.itemCount)
            } else {
                warningToast.setText(getString(R.string.error__settings_clip_max_limit, MAX_SELECTED_CLIP_COUNT))
                warningToast.show()
            }
        }

        rvSelectedClips.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = selectedClipsAdapter
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
            addItemDecoration(itemDecorator)
        }
        selectedClipsAdapter.onRemoveClickListener = { clipModel ->
            selectedClipsAdapter.remove(clipModel)
            selectedClipsAdapter.notifyDataSetChanged() // to update and display in TextView the ordinal number of each item
            if (selectedClipsAdapter.itemCount <= templateClipsAdapter.itemCount) {
                rvSelectedClips.isEnabledExternalNestedScroll = true
            }
        }
        selectedClipsAdapter.onEffectClickListener = { trackModel ->
            showEffectsPopup(trackModel)?.showAtLocation(view, Gravity.CENTER, 100, 100)
        }

        chbTestCases.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                tvSelectTestCases.visibility = View.VISIBLE
            } else {
                tvSelectTestCases.visibility = View.GONE
            }
        }

        tvSelectTestCases.setOnClickListener {
            presenter.getTestCases()
        }
    }

    private fun showHelpPopup(): PopupWindow? {
        val popupView = LayoutInflater.from(requireContext()).inflate(R.layout.help_popup, null)
        val displayMetrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
        helpPopupWidth = displayMetrics.widthPixels / 2

        popupWindow = PopupWindow(popupView, helpPopupWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
        popupWindow?.isOutsideTouchable = true

        return popupWindow
    }

    private fun showEffectsPopup(trackModel: TrackModel): PopupWindow? {
        val popupView = LayoutInflater.from(requireContext()).inflate(R.layout.effect_popup, null)
        popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        popupWindow?.isOutsideTouchable = true

        val tvTitle = popupView.findViewById<TextView>(R.id.tvTitle)
        tvTitle.text = getString(trackModel.nameResId)

        val rvPopup = popupView.findViewById<RecyclerView>(R.id.rvPopup)
        effectPopupAdapter.replaceAll(Effect.values().toMutableList())
        rvPopup.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = effectPopupAdapter
        }

        effectPopupAdapter.onItemClickListener = { effect ->
            Handler().postDelayed({ popupWindow?.dismiss() }, 150)
            val newTrackModel = trackModel.copy(effectId = effect.effectId)
            selectedClipsAdapter.replace(trackModel, newTrackModel)
        }

        return popupWindow
    }

    override fun onGetTestCases(testCasesSettings: List<Settings>) {
        if (selectedTestCaseIds.isEmpty()) {
            testCasesSettings.forEachIndexed { index, _ ->
                selectedTestCaseIds.add(index)
            }
        }
        if (selectedExportCaseIds.isEmpty()) {
            testCasesSettings.forEachIndexed { index, _ ->
                selectedExportCaseIds.add(index)
            }
        }
        val testCases = settingsTestCaseModelMapper.mapToModel(
                entityList = testCasesSettings,
                selectedTestCaseIds = selectedTestCaseIds,
                selectedExportCaseIds = selectedExportCaseIds
        )
        showSelectTestCasesPopup(testCases)?.showAtLocation(view, Gravity.CENTER_HORIZONTAL, 0, 0)
    }

    private fun showSelectTestCasesPopup(settingsTestCases: List<SettingsTestCaseModel>): PopupWindow? {
        val popupView = LayoutInflater.from(requireContext()).inflate(R.layout.select_test_cases_popup, null)
        popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val display: Display = requireActivity().windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val width: Int = size.x
        val height: Int = size.y
        popupWindow?.width = (width * 0.8).toInt()
        popupWindow?.height = height - 250

        popupWindow?.isOutsideTouchable = true

        val rvTestCases = popupView.findViewById<RecyclerView>(R.id.rvTestCases)
        testCasesPopupAdapter.replaceAll(settingsTestCases.toMutableList())
        rvTestCases.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = testCasesPopupAdapter
        }

        testCasesPopupAdapter.onTestCaseClickListener = { position, isChecked ->
            if (selectedTestCaseIds.contains(position) && !isChecked) {
                selectedTestCaseIds.remove(position)
            } else if (!selectedTestCaseIds.contains(position) && isChecked) {
                selectedTestCaseIds.add(position)
            }
        }

        testCasesPopupAdapter.onExportClickListener = { position, isChecked ->
            if (selectedExportCaseIds.contains(position) && !isChecked) {
                selectedExportCaseIds.remove(position)
            } else if (!selectedExportCaseIds.contains(position) && isChecked) {
                selectedExportCaseIds.add(position)
            }
        }

        val ivToggleFilterAll = popupView.findViewById<ImageView>(R.id.ivToggleFilterAll)
        ivToggleFilterAll.setOnClickListener {
            val filterAllChecked = selectedTestCaseIds.size == settingsTestCases.size
            selectedTestCaseIds.clear()
            if (!filterAllChecked) {
                testCasesPopupAdapter.items.forEachIndexed { index, _ ->
                    selectedTestCaseIds.add(index)
                }
            }
            val newTestCases = testCasesPopupAdapter.items.map {
                it.isSelected = !filterAllChecked
                it
            }
            testCasesPopupAdapter.replaceAll(newTestCases)
        }

        val ivToggleExportAll = popupView.findViewById<ImageView>(R.id.ivToggleExportAll)
        ivToggleExportAll.setOnClickListener {
            val exportAllChecked = selectedExportCaseIds.size == settingsTestCases.size
            selectedExportCaseIds.clear()
            if (!exportAllChecked) {
                testCasesPopupAdapter.items.forEachIndexed { index, _ ->
                    selectedExportCaseIds.add(index)
                }
            }
            val newExportCases = testCasesPopupAdapter.items.map {
                it.isExportEnabled = !exportAllChecked
                it
            }
            testCasesPopupAdapter.replaceAll(newExportCases)
        }

        return popupWindow
    }

    override fun onGetSettings(settings: Settings) {
        chbShowSummary.isChecked = settings.isSummaryVisible
        chbDecoderFallback.isChecked = settings.isEnableDecoderFallback
        chbTestCases.isChecked = settings.isTestCaseMode

        selectedClipsAdapter.replaceAll(trackModelMapper.mapToModel(settings.trackList))
        rvSelectedClips.isEnabledExternalNestedScroll = selectedClipsAdapter.itemCount <= templateClipsAdapter.itemCount

        when (settings.encoderCodecType) {
            EncoderCodecType.h264 -> rbH264.isChecked = true
            EncoderCodecType.h265 -> rbH265.isChecked = true
        }
    }

    private fun applySettings() {
        val trackList = trackModelMapper.mapFromModel(selectedClipsAdapter.items)

        val encoderCodecType = when {
            rbH264.isChecked -> EncoderCodecType.h264
            else -> EncoderCodecType.h265
        }

        val newSettings = Settings(
                trackList = trackList,
                encoderCodecType = encoderCodecType,
                isEnableDecoderFallback = chbDecoderFallback.isChecked,
                isSummaryVisible = chbShowSummary.isChecked,
                isTestCaseMode = chbTestCases.isChecked,
                selectedTestCaseIds = selectedTestCaseIds,
                selectedExportCaseIds = selectedExportCaseIds
        )
        presenter.putSettings(newSettings)
    }

    override fun onPutSettings(settings: Settings) {
        val requestCode = arguments?.getInt(ARGUMENT_REQUEST_CODE)
        requestCode?.let {
            val intent = Intent()
            intent.putExtra(EXTRA_RESULT_SETTINGS, settings)
            targetFragment?.onActivityResult(it, Activity.RESULT_OK, intent)
        }
        fragmentManager?.popBackStack()
    }

    override fun onDestroyView() {
        presenter.onDetach()
        super.onDestroyView()
        popupWindow?.dismiss()
    }

    override fun onBackPressed(): Boolean {
        fragmentManager?.popBackStack()
        return true
    }
}