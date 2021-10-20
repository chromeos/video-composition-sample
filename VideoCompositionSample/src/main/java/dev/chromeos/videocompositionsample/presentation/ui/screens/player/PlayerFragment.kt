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

package dev.chromeos.videocompositionsample.presentation.ui.screens.player

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import dev.chromeos.videocompositionsample.presentation.R
import dev.chromeos.videocompositionsample.presentation.opengl.CompositionGLSurfaceView
import dev.chromeos.videocompositionsample.presentation.tools.exception.ErrorModel
import dev.chromeos.videocompositionsample.presentation.tools.info.MessageModel
import dev.chromeos.videocompositionsample.presentation.ui.base.BaseFragment
import dev.chromeos.videocompositionsample.presentation.ui.custom.CustomSliderView
import dev.chromeos.videocompositionsample.presentation.ui.custom.PlayerControlView
import dev.chromeos.videocompositionsample.presentation.ui.custom.ResolutionView
import dev.chromeos.videocompositionsample.presentation.ui.mapper.SummaryModelMapper
import dev.chromeos.videocompositionsample.presentation.ui.mapper.TrackModelMapper
import dev.chromeos.videocompositionsample.presentation.ui.model.SummaryModel
import dev.chromeos.videocompositionsample.presentation.ui.screens.dialog.InfoDialogFragment
import dev.chromeos.videocompositionsample.presentation.ui.screens.player.adapter.ClipSummaryAdapter
import dev.chromeos.videocompositionsample.presentation.ui.screens.player.core.IPlayerView
import dev.chromeos.videocompositionsample.presentation.ui.screens.player.core.PlayerPresenter
import dev.chromeos.videocompositionsample.presentation.ui.screens.settings.SettingsFragment
import dev.chromeos.videocompositionsample.domain.entity.Settings
import kotlinx.android.synthetic.main.fragment_player.*
import javax.inject.Inject
import kotlin.system.exitProcess

class PlayerFragment : BaseFragment(), IPlayerView, InfoDialogFragment.OnInfoDialogListener {

    companion object {
        const val SETTINGS_REQUEST_CODE = 30001
        const val ERROR_CALLBACK_KEY = "ERROR_CALLBACK_KEY"
        const val SHARE_CALLBACK_KEY = "SHARE_CALLBACK_KEY"
        const val NOT_SHARE_CALLBACK_KEY = "NOT_SHARE_CALLBACK_KEY"
    }

    private val isChromeOS: Boolean by lazy { context?.packageManager?.hasSystemFeature("org.chromium.arc.device_management")?: false }

    private val clipSummaryAdapter: ClipSummaryAdapter by lazy { ClipSummaryAdapter() }
    private val trackModelMapper = TrackModelMapper()
    private val summaryModelMapper = SummaryModelMapper()
    private var isShareMode = false

    @Inject lateinit var presenter: PlayerPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        component.inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_player, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isShareMode = false
        presenter.onAttach(this)
        presenter.observeAlphaDragging(sliderAlpha.dragChanges())
        presenter.observeSeekDragging(sliderTime.dragChanges())
        presenter.observeStopSeekDragging(sliderTime.stopDrags())
        presenter.observeStopLongSkipping(playerControlView.stopSkips())

        initPlayerControl()
        initResolutionControl()

        layPlayer.setOnClickListener {
            if (isChromeOS) { // to prevent low performance on PixelBook
                if (layExpandableControl.isExpanded) {
                    presenter.skipHideControlPanelWithDelay()
                } else {
                    presenter.hideControlWithDelay(3000)
                }
            }
            layExpandableControl.toggle()
        }

        ivFileUpload.setOnClickListener {
            presenter.executePlayerCommand(PlayerCommand.Export(resolutionView.resolution))
        }

        ivSetting.setOnClickListener {
            presenter.executePlayerCommand(PlayerCommand.Pause)
            presenter.releaseMediaComposition()
            navigator.toSettings(this, fragmentManager, SETTINGS_REQUEST_CODE)
        }

        rvSummary.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = clipSummaryAdapter
        }
    }

    override fun onStart() {
        super.onStart()
        compositionGLSurfaceView?.init(
                onSurfaceReady = { presenter.getSettings() },
                onStopExport = { presenter.executePlayerCommand(PlayerCommand.Pause) },
                onEncodeError = {
                    presenter.showErrorMessage(
                            ErrorModel(
                                    messageResId = R.string.error__data_cannot_be_saved,
                                    callbackDetails = ErrorModel.CallbackDetails(
                                            positiveKey = ERROR_CALLBACK_KEY
                                    )
                            )
                    )
                    presenter.executePlayerCommand(PlayerCommand.Pause)
                }
        )
    }

    override fun getGLSurfaceView(): CompositionGLSurfaceView? = compositionGLSurfaceView

    override fun getSliderTime(): CustomSliderView? = sliderTime

    override fun getPlayerControlView(): PlayerControlView? = playerControlView

    override fun getResolutionView(): ResolutionView? = resolutionView

    private fun initPlayerControl() {
        sliderTime.timeLineFps = PlayerPresenter.FPS
        sliderTime.valueTo = (PlayerPresenter.DURATION_MILLIS / 1000).toFloat()
        sliderAlpha.value = 1f
        playerControlView.setOnClickListener(object : PlayerControlView.OnClickListener {
            override fun onToBeginClick() {
                presenter.executePlayerCommand(PlayerCommand.ToBegin)
            }

            override fun onPlayClick() {
                presenter.executePlayerCommand(PlayerCommand.Play)
                if (isChromeOS) { // to prevent low performance on PixelBook
                    presenter.hideControlWithDelay(700)
                }
            }

            override fun onPauseClick() {
                presenter.executePlayerCommand(PlayerCommand.Pause)
                presenter.showAverageFps(false)
            }

            override fun onSkipForward() {
                presenter.executePlayerCommand(PlayerCommand.SkipForward)
            }

            override fun onSkipBackward() {
                presenter.executePlayerCommand(PlayerCommand.SkipBackward)
            }

            override fun onLongSkipForward() {
                presenter.executePlayerCommand(PlayerCommand.LongSkipForward)
            }

            override fun onLongSkipBackward() {
                presenter.executePlayerCommand(PlayerCommand.LongSkipBackward)
            }

            override fun onToEndClick() {
                presenter.executePlayerCommand(PlayerCommand.ToEnd)
            }
        })
    }

    override fun toggleControlPanel() {
        layExpandableControl.toggle()
    }

    override fun hideControlPanel() {
        layExpandableControl.collapse()
    }

    private fun setTestCasesMode(enabled: Boolean) {
        if (enabled) {
            playerControlView.isPlayButtonEnabled = false
            playerControlView.isSkipButtonsEnabled = false
            sliderTime.isDraggable = false
            ivFileUpload.isEnabled = false
            layExpandableControl.collapse()
        } else {
            playerControlView.isPlayButtonEnabled = true
            playerControlView.isSkipButtonsEnabled = true
            sliderTime.isDraggable = true
            ivFileUpload.isEnabled = true
            layExpandableControl.expand()
        }
    }

    override fun onTestCasesFinished() {
        onShowMessage(getString(R.string.test_cases__finished))
    }

    private fun initResolutionControl() {
        resolutionView.resolution = ResolutionView.Resolution.Full
        resolutionView.setOnResolutionCheckListener(object : ResolutionView.OnResolutionCheckListener {
            override fun onEighthChecked() {}

            override fun onQuarterChecked() {}

            override fun onHalfChecked() {}

            override fun onFullChecked() {}
        })
    }

    override fun showHideSummary(settings: Settings) {
        if (settings.isSummaryVisible) {
            laySummary.visibility = View.VISIBLE
            tvEncoderCodecValue.text = settings.encoderCodecType.name
            if (settings.isEnableDecoderFallback) {
                tvDecoderFallbackValue.text = getString(R.string.settings__enabled)
                tvDecoderFallbackValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
            } else {
                tvDecoderFallbackValue.text = getString(R.string.settings__disabled)
                tvDecoderFallbackValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            }
        } else {
            laySummary.visibility = View.GONE
        }
        val trackModelList = trackModelMapper.mapToModel(settings.trackList)
        val summaryModelList = summaryModelMapper.map(trackModelList)
        clipSummaryAdapter.replaceAll(summaryModelList)
    }

    override fun onGetSettings(settings: Settings) {
        showHideSummary(settings)
        if (settings.isTestCaseMode) {
            setTestCasesMode(true)
            presenter.startTestCases(
                    selectedTestCaseIds = settings.selectedTestCaseIds,
                    selectedExportCaseIds = settings.selectedExportCaseIds
            )
        } else {
            setTestCasesMode(false)
            presenter.init(settings)
        }
    }

    override fun onPlayersError(notReadyMediaTrackIds: MutableSet<Int>) {
        clipSummaryAdapter.data.mapIndexed { index: Int, summaryModel: SummaryModel ->
            summaryModel.isLoadSuccess = !notReadyMediaTrackIds.contains(index)
        }
        clipSummaryAdapter.notifyDataSetChanged()
    }

    override fun onPlayerPlay() {
        hideFpsAverage()
        playerControlView.setPlayState()
        playerControlView.isPlayButtonEnabled = true
        playerControlView.isSkipButtonsEnabled = false
        sliderTime.isDraggable = false
        sliderAlpha.isDraggable = false
    }

    override fun onPlayerPause() {
        ivRecord.visibility = View.GONE
        playerControlView.setPauseState()
        playerControlView.isPlayButtonEnabled = true
        playerControlView.isSkipButtonsEnabled = true
        sliderTime.isDraggable = true
        sliderAlpha.isDraggable = true
        ivFileUpload.isEnabled = true
    }

    override fun onPlayerSeek() {
        playerControlView.isPlayButtonEnabled = false
    }

    override fun onPlayerExport() {
        ivRecord.visibility = View.VISIBLE
        ivFileUpload.isEnabled = false
        playerControlView.setPlayState()
        playerControlView.isPlayButtonEnabled = true
        playerControlView.isSkipButtonsEnabled = false
        sliderTime.isDraggable = false
    }

    override fun onTimeLineChanged(value: Float) {
        sliderTime.value = value
    }

    override fun showFpsAverageList(fpsAverageList: List<Float>) {
        if (clipSummaryAdapter.itemCount == fpsAverageList.size) {
            clipSummaryAdapter.items.forEachIndexed { index, summaryModel ->
                summaryModel.fpsAverage = fpsAverageList[index]
            }
            clipSummaryAdapter.notifyDataSetChanged()
        }
    }

    private fun hideFpsAverage() {
        clipSummaryAdapter.items.forEach { it.fpsAverage = 0f }
        clipSummaryAdapter.notifyDataSetChanged()
    }

    override fun onExportSuccessFinished(messageModel: MessageModel) {
        onShowMessage(messageModel)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SETTINGS_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                data?.let {
                    val settings = SettingsFragment.getSettings(data)
                    settings?.let {
                        showHideSummary(settings)
                        setTestCasesMode(settings.isTestCaseMode)
                        presenter.init(settings)
                    }
                }
            } else {
                presenter.initCurrent()
            }
        }
    }

    override fun onStop() {
        presenter.releaseMediaComposition()
        presenter.onDetach()
        super.onStop()
        if (!isShareMode) {
            requireActivity().finish()
            exitProcess(0)
        }
    }

    override fun toShare(absoluteFilePath: String) {
        isShareMode = true
        navigator.toShare(this, fragmentManager, absoluteFilePath)
    }

    override fun onPositiveCallback(key: String) {
        when(key) {
            ERROR_CALLBACK_KEY -> presenter.activateErrorMessages()
            SHARE_CALLBACK_KEY -> presenter.share()
        }
    }

    override fun onNegativeCallback(key: String) {
        when(key) {
            NOT_SHARE_CALLBACK_KEY -> presenter.reconfigureCodecsForPlayback()
        }
    }

}