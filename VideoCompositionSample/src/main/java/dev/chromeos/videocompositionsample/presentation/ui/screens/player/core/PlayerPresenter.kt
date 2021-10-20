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

package dev.chromeos.videocompositionsample.presentation.ui.screens.player.core

import android.content.Context
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Build
import android.util.Log
import androidx.annotation.StringRes
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SeekParameters
import dev.chromeos.videocompositionsample.presentation.R
import dev.chromeos.videocompositionsample.presentation.media.MediaTrack
import dev.chromeos.videocompositionsample.presentation.media.encoder.utils.VideoMimeType
import dev.chromeos.videocompositionsample.presentation.media.mapper.MediaTrackMapper
import dev.chromeos.videocompositionsample.presentation.opengl.CompositionRenderer.Companion.ENCODE_SPEED_FACTOR
import dev.chromeos.videocompositionsample.presentation.tools.exception.ErrorModel
import dev.chromeos.videocompositionsample.presentation.tools.extensions.exhaustive
import dev.chromeos.videocompositionsample.presentation.tools.extensions.getCacheDirFilePath
import dev.chromeos.videocompositionsample.presentation.tools.extensions.getExternalStorageFilePath
import dev.chromeos.videocompositionsample.presentation.tools.extensions.saveToVideoGallery
import dev.chromeos.videocompositionsample.presentation.tools.info.MessageModel
import dev.chromeos.videocompositionsample.presentation.tools.resource.IResourceManager
import dev.chromeos.videocompositionsample.presentation.ui.base.BasePresenter
import dev.chromeos.videocompositionsample.presentation.ui.custom.ResolutionView
import dev.chromeos.videocompositionsample.presentation.ui.model.TestCaseModel
import dev.chromeos.videocompositionsample.presentation.ui.screens.player.PlayerCommand
import dev.chromeos.videocompositionsample.presentation.ui.screens.player.PlayerFragment
import dev.chromeos.videocompositionsample.presentation.ui.screens.player.PlayerFragment.Companion.NOT_SHARE_CALLBACK_KEY
import dev.chromeos.videocompositionsample.presentation.ui.screens.player.PlayerFragment.Companion.SHARE_CALLBACK_KEY
import dev.chromeos.videocompositionsample.domain.entity.EncoderCodecType
import dev.chromeos.videocompositionsample.domain.entity.Settings
import dev.chromeos.videocompositionsample.domain.interactor.base.observers.BaseSingleObserver
import dev.chromeos.videocompositionsample.domain.interactor.settings.GetSettingsUseCase
import dev.chromeos.videocompositionsample.domain.interactor.test.GetTestCasesUseCase
import dev.chromeos.videocompositionsample.domain.schedulers.ISchedulerProvider
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

class PlayerPresenter
@Inject constructor(private val context: Context,
                    private val resourceManager: IResourceManager,
                    private val getSettingsUseCase: GetSettingsUseCase,
                    private val getTestCasesUseCase: GetTestCasesUseCase,
                    private val mediaTrackMapper: MediaTrackMapper,
                    private val schedulerProvider: ISchedulerProvider
) : BasePresenter<IPlayerView>(CompositeDisposable()) {

    companion object {
        private const val TEST_TAG = "Composition TestCases ---->"
        const val DURATION_MILLIS = 30000L
        const val FPS = 30
        const val EXPORT_FILE_NAME = "composition"
        const val INIT_RETRY_ATTEMPTS = 10
    }

    enum class PlayerControlState {
        Play,
        Pause,
        Seek,
        Export
    }

    private var mediaTrackList: List<MediaTrack> = listOf()
    private var isMediaCompositionInit = false

    private var playerControlState = PlayerControlState.Pause
    private var timeLineDisposable: Disposable? = null
    private var playDisposable: Disposable? = null
    private var pauseWithDelayDisposable: Disposable? = null
    private var controlPanelDisposable: Disposable? = null
    private var singleTargetSeekDisposable: Disposable? = null

    private var isSingleSeekProcessing = false
    private var isDragSeekProcessing = false
    private var isSkipProcessing = false

    private lateinit var currentSettings: Settings
    private val notReadyMediaTrackIds: MutableSet<Int> = mutableSetOf()
    private var retryAttempt = 0
    private var isRetryProcessing = false
    private var hasActiveErrorMessage = false

    private var exportAbsoluteFilePath = EXPORT_FILE_NAME
    private var isTestExportProcessing = false
    private var testCaseIndex = 0
    private var testCases: List<TestCaseModel> = listOf()

    fun init(settings: Settings) {
        retryAttempt = 0
        currentSettings = settings
        if (settings.isTestCaseMode) {
            startTestCases(
                    selectedTestCaseIds = settings.selectedTestCaseIds,
                    selectedExportCaseIds = settings.selectedExportCaseIds
            )
        } else {
            initMediaComposition()
        }
    }

    fun initCurrent() {
        retryAttempt = 0
        val disposable = Observable.timer(300, TimeUnit.MILLISECONDS)
                .subscribeOn(schedulerProvider.background())
                .observeOn(schedulerProvider.main())
                .subscribe {
                    initMediaComposition()
                }
        compositeDisposable.add(disposable)
    }

    private fun initMediaComposition() {
        System.gc()
        notReadyMediaTrackIds.clear()
        mediaTrackList = mediaTrackMapper.map(currentSettings.trackList, isEnableDecoderFallback = true, isExport = false)
        weakView.get()?.getGLSurfaceView()?.setMediaTracks(mediaTrackList)
        isMediaCompositionInit = true
        executePlayerCommand(PlayerCommand.ToBegin)
        observeTimeLine()
        monitorPlayerList()
    }

    private fun monitorPlayerList() {
        mediaTrackList.mapIndexed { index: Int, mediaTrack: MediaTrack ->
            mediaTrack.player?.addListener(object : Player.EventListener {
                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_IDLE -> {
                            notReadyMediaTrackIds.add(index)
                            if (playerControlState != PlayerControlState.Export && playerControlState != PlayerControlState.Play) {
                                retryInit(index)
                            } else if (playerControlState == PlayerControlState.Export) {
                                executePlayerCommand(PlayerCommand.Pause)
                            }
                            weakView.get()?.onPlayersError(notReadyMediaTrackIds)
                            weakView.get()?.getGLSurfaceView()?.playerStateChanged(index, false)
                        }
                        Player.STATE_ENDED -> {
                            weakView.get()?.getGLSurfaceView()?.playerStateChanged(index, false)
                        }
                        else -> {
                            weakView.get()?.getGLSurfaceView()?.playerStateChanged(index, playWhenReady)
                        }
                    }
                }

                override fun onSeekProcessed() {
                    if (playerControlState != PlayerControlState.Export) {
                        weakView.get()?.getGLSurfaceView()?.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    }
                }

            }) ?: run {
                notReadyMediaTrackIds.add(index)
                weakView.get()?.onPlayersError(notReadyMediaTrackIds)
            }
        }
    }

    private fun retryInit(index: Int) {
        if (retryAttempt < INIT_RETRY_ATTEMPTS && !isRetryProcessing) {
            showProgress()
            isRetryProcessing = true
            retryAttempt++
            val disposable = Observable.timer(150, TimeUnit.MILLISECONDS)
                    .subscribeOn(schedulerProvider.background())
                    .observeOn(schedulerProvider.main())
                    .subscribe {
                        notReadyMediaTrackIds.remove(index)
                        weakView.get()?.onPlayersError(notReadyMediaTrackIds)
                        if (index < mediaTrackList.size) {
                            mediaTrackList[index].player?.retry()
                        }
                        isRetryProcessing = false
                        if (notReadyMediaTrackIds.isNotEmpty()) {
                            retryInit(notReadyMediaTrackIds.first())
                        } else {
                            hideProgress()
                        }
                    }
            compositeDisposable.add(disposable)
        } else if (!isRetryProcessing) {
            hideProgress()
        }
    }

    fun getSettings() {
        val disposable = getSettingsUseCase.execute(object : BaseSingleObserver<GetSettingsUseCase.ResponseValues>() {
            override fun onSuccess(data: GetSettingsUseCase.ResponseValues) {
                currentSettings = data.settings
                weakView.get()?.onGetSettings(data.settings)
            }

            override fun onError(e: Throwable) {
                showErrorMessage(getErrorModel(messageResId = R.string.error__settings_cannot_be_loaded))
            }
        })
        compositeDisposable.add(disposable)
    }

    private fun observeTimeLine() {
        val durationSecond = DURATION_MILLIS.toFloat() / 1000f
        timeLineDisposable?.let { compositeDisposable.delete(it) }
        timeLineDisposable = Flowable.interval(50, TimeUnit.MILLISECONDS)
                .filter {
                    isMediaCompositionInit
                            && (playerControlState == PlayerControlState.Play || playerControlState == PlayerControlState.Export)
                }
                .switchMap { Flowable.just(weakView.get()?.getGLSurfaceView()?.getTimePosition() ?: 0) }
                .subscribeOn(schedulerProvider.background())
                .observeOn(schedulerProvider.main())
                .onErrorResumeNext(Flowable.just(0L))
                .subscribe { milliSeconds ->
                    val value: Float = milliSeconds.toFloat() / 1000f
                    if (value < durationSecond) {
                        if (playerControlState == PlayerControlState.Export) {
                            val percent = value / durationSecond * 100
                            weakView.get()?.setProgressPercent(percent.toInt())
                        }
                        weakView.get()?.onTimeLineChanged(value)
                    } else {
                        weakView.get()?.onTimeLineChanged(durationSecond)
                        if (playerControlState == PlayerControlState.Play || playerControlState == PlayerControlState.Export) {
                            pauseWithDelay()
                        }
                    }
                }

        timeLineDisposable?.let { compositeDisposable.add(it) }
    }

    private fun pauseWithDelay() {
        if (pauseWithDelayDisposable?.isDisposed == false) return

        val isExport = playerControlState == PlayerControlState.Export
        showAverageFps(isExport)

        pauseWithDelayDisposable = Observable.timer(300, TimeUnit.MILLISECONDS)
                .subscribeOn(schedulerProvider.background())
                .observeOn(schedulerProvider.main())
                .subscribe {
                    if (currentSettings.isTestCaseMode) {
                        if (playerControlState == PlayerControlState.Export) {
                            executePlayerCommand(PlayerCommand.Pause)
                        } else if (playerControlState == PlayerControlState.Play) {
                            if (currentSettings.isTestExportEnabled) {
                                continueTestWithExport()
                            } else {
                                continueNextTestCase()
                            }
                        }

                    } else {
                        if (playerControlState != PlayerControlState.Pause) {
                            executePlayerCommand(PlayerCommand.Pause)
                        }
                    }
                }
        pauseWithDelayDisposable?.let { compositeDisposable.add(it) }
    }

    fun observeAlphaDragging(alphaDragChanges: Observable<Float>) {
        val disposable = alphaDragChanges
                .debounce(10, TimeUnit.MILLISECONDS)
                .distinctUntilChanged()
                .subscribeOn(schedulerProvider.background())
                .observeOn(schedulerProvider.main())
                .onErrorResumeNext(Observable.just(0f))
                .subscribe {
                    executePlayerCommand(PlayerCommand.SetOpacity(2, it))
                }

        compositeDisposable.add(disposable)
    }

    fun observeSeekDragging(seekDragChanges: Observable<Float>) {
        val disposable = seekDragChanges
                .debounce(10, TimeUnit.MILLISECONDS)
                .distinctUntilChanged()
                .subscribeOn(schedulerProvider.background())
                .observeOn(schedulerProvider.main())
                .onErrorResumeNext(Observable.just(0f))
                .subscribe { value ->
                    val milliSeconds = (value * 1000).toLong()
                    executePlayerCommand(PlayerCommand.Seek(milliSeconds))
                }

        compositeDisposable.add(disposable)
    }

    fun observeStopSeekDragging(stopDrags: Observable<Unit>) {
        var count = 0
        val disposable = stopDrags
                .switchMap { Observable.just(Unit).delay(50, TimeUnit.MILLISECONDS) }
                .subscribeOn(schedulerProvider.background())
                .observeOn(schedulerProvider.main())
                .map {
                    count++
                    isDragSeekProcessing = true
                    val exactPosition = weakView.get()?.getGLSurfaceView()?.getTimePosition() ?: 0
                    mediaTrackList.forEach { mediaTrack ->
                        mediaTrack.player?.setSeekParameters(SeekParameters.EXACT)
                    }
                    weakView.get()?.getGLSurfaceView()?.seek(exactPosition)
                }
                .delay(1700, TimeUnit.MILLISECONDS)
                .subscribeOn(schedulerProvider.background())
                .observeOn(schedulerProvider.main())
                .subscribe {
                    if (count == 1 && weakView.get()?.getSliderTime()?.isDragging == false) {
                        weakView.get()?.getGLSurfaceView()?.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                        mediaTrackList.forEach { mediaTrack ->
                            mediaTrack.player?.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                        }
                        notifyPlayerControlView(PlayerControlState.Pause)
                        count = 0
                        isDragSeekProcessing = false
                    } else {
                        count--
                    }
                }

        compositeDisposable.add(disposable)
    }

    fun observeStopLongSkipping(stopSkipping: Observable<Unit>) {
        var count = 0
        val disposable = stopSkipping
                .switchMap { Observable.just(Unit).delay(50, TimeUnit.MILLISECONDS) }
                .subscribeOn(schedulerProvider.background())
                .observeOn(schedulerProvider.main())
                .map {
                    count++
                    isSkipProcessing = true
                    val exactPosition = weakView.get()?.getGLSurfaceView()?.getTimePosition() ?: 0
                    mediaTrackList.forEach { mediaTrack ->
                        mediaTrack.player?.setSeekParameters(SeekParameters.EXACT)
                    }
                    weakView.get()?.getGLSurfaceView()?.seek(exactPosition)
                }
                .delay(1700, TimeUnit.MILLISECONDS)
                .subscribeOn(schedulerProvider.background())
                .observeOn(schedulerProvider.main())
                .subscribe {
                    if (count == 1 && weakView.get()?.getPlayerControlView()?.isSkipping == false) {
                        weakView.get()?.getGLSurfaceView()?.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                        mediaTrackList.forEach { mediaTrack ->
                            mediaTrack.player?.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                        }
                        notifyPlayerControlView(PlayerControlState.Pause)
                        count = 0
                        isSkipProcessing = false
                    } else {
                        count--
                    }
                }

        compositeDisposable.add(disposable)
    }

    fun showAverageFps(isExport: Boolean) {
        val disposable = Observable.timer(150, TimeUnit.MILLISECONDS)
                .subscribeOn(schedulerProvider.background())
                .observeOn(schedulerProvider.main())
                .subscribe {
                    val fpsAverageList = if (isExport) {
                        weakView.get()?.getGLSurfaceView()?.getFpsAverageList()?.map { it / ENCODE_SPEED_FACTOR }?: emptyList()
                    } else {
                        if (!currentSettings.isTestCaseMode) {
                            executePlayerCommand(PlayerCommand.ExactCurrentPositionSeek)
                        }
                        weakView.get()?.getGLSurfaceView()?.getFpsAverageList()?: emptyList()
                    }
                    weakView.get()?.showFpsAverageList(fpsAverageList)
                }
        compositeDisposable.add(disposable)
    }

    fun hideControlWithDelay(delay: Long) {
        if (controlPanelDisposable?.isDisposed != false) {
            controlPanelDisposable = Observable.timer(delay, TimeUnit.MILLISECONDS)
                    .subscribeOn(schedulerProvider.background())
                    .observeOn(schedulerProvider.main())
                    .subscribe {
                        weakView.get()?.hideControlPanel()
                    }
            controlPanelDisposable?.let { compositeDisposable.add(it) }
        }
    }

    fun skipHideControlPanelWithDelay() {
        if (controlPanelDisposable?.isDisposed != false) {
            controlPanelDisposable?.dispose()
        }
    }

    fun executePlayerCommand(command: PlayerCommand) {
        if (!isMediaCompositionInit) return
        when (command) {
            PlayerCommand.Play -> {
                if (isSingleSeekProcessing || isDragSeekProcessing || isSkipProcessing) return

                executePlay {
                    weakView.get()?.getGLSurfaceView()?.play()
                }
            }
            PlayerCommand.Pause -> {
                playDisposable?.dispose()
                Log.d(TEST_TAG, "Pause")
                Log.d(TEST_TAG, "Current timestamp ${System.currentTimeMillis()}")
                if (playerControlState == PlayerControlState.Export) {
                    onExportFinished()
                }
                notifyPlayerControlView(PlayerControlState.Pause)
                mediaTrackList.forEach { mediaTrack ->
                    mediaTrack.player?.playWhenReady = false
                }
                weakView.get()?.getGLSurfaceView()?.pause()
            }
            PlayerCommand.SkipForward -> executeSingleTargetSeek {
                val milliSeconds = weakView.get()?.getGLSurfaceView()?.getTimePosition() ?: 0
                val forwardPosition = min(DURATION_MILLIS, milliSeconds + 1000 / FPS)
                weakView.get()?.getGLSurfaceView()?.seek(forwardPosition)
            }
            PlayerCommand.SkipBackward -> executeSingleTargetSeek {
                val milliSeconds = weakView.get()?.getGLSurfaceView()?.getTimePosition() ?: 0
                val backwardPosition = max(0, milliSeconds - 1000 / FPS)
                weakView.get()?.getGLSurfaceView()?.seek(backwardPosition)
            }
            PlayerCommand.LongSkipForward -> executeLongSkip {
                val milliSeconds = weakView.get()?.getGLSurfaceView()?.getTimePosition() ?: 0
                val forwardPosition = min(DURATION_MILLIS, milliSeconds + 1000 / FPS)
                weakView.get()?.getGLSurfaceView()?.seek(forwardPosition)
            }
            PlayerCommand.LongSkipBackward -> executeLongSkip {
                val milliSeconds = weakView.get()?.getGLSurfaceView()?.getTimePosition() ?: 0
                val backwardPosition = max(0, milliSeconds - 1000 / FPS)
                weakView.get()?.getGLSurfaceView()?.seek(backwardPosition)
            }
            PlayerCommand.ToBegin -> executeSingleTargetSeek { weakView.get()?.getGLSurfaceView()?.seek(0L) }
            PlayerCommand.ToEnd -> executeSingleTargetSeek { weakView.get()?.getGLSurfaceView()?.seek(DURATION_MILLIS) }
            is PlayerCommand.Seek -> {
                weakView.get()?.getGLSurfaceView()?.seek(command.time)
                notifyPlayerControlView(PlayerControlState.Seek)
            }
            PlayerCommand.ExactCurrentPositionSeek -> {
                val milliSeconds = weakView.get()?.getGLSurfaceView()?.getTimePosition() ?: 0
                if (milliSeconds >= DURATION_MILLIS) return
                executeSingleTargetSeek {
                    weakView.get()?.getGLSurfaceView()?.seek(milliSeconds)
                }
            }
            is PlayerCommand.SetOpacity -> {
                weakView.get()?.getGLSurfaceView()?.setOpacity(command.trackIndex, command.opacity)
            }
            is PlayerCommand.Export -> {
                val videoMimeType = when (currentSettings.encoderCodecType) {
                    EncoderCodecType.h264 -> VideoMimeType.h264
                    EncoderCodecType.h265 -> VideoMimeType.h265
                }
                executeExport {
                    exportAbsoluteFilePath = if (currentSettings.isTestCaseMode) {
                        getTestCaseExportAbsoluteFilePath(testCases[testCaseIndex].testCaseId)
                    } else {
                        val fileName = "$EXPORT_FILE_NAME.mp4"
                        context.getCacheDirFilePath(fileName)
                    }
                    weakView.get()?.getGLSurfaceView()?.export(
                            absoluteFilePath = exportAbsoluteFilePath,
                            width = command.resolution.width,
                            height = command.resolution.height,
                            videoMimeType = videoMimeType,
                            isTestCaseMode = currentSettings.isTestCaseMode
                    )
                }
            }
        }.exhaustive
    }

    private fun executePlay(action: () -> Unit) {
        if (playDisposable?.isDisposed != false) {

            notifyPlayerControlView(PlayerControlState.Play)
            var currentTimePosition = weakView.get()?.getGLSurfaceView()?.getTimePosition() ?: 0

            if (currentTimePosition >= DURATION_MILLIS) {
                weakView.get()?.getGLSurfaceView()?.setBeginTimePosition()
                currentTimePosition = 0L
            }

            if (currentTimePosition == 0L) {
                mediaTrackList.forEach { mediaTrack ->
                    mediaTrack.player?.setSeekParameters(SeekParameters.EXACT)
                    mediaTrack.player?.seekTo(mediaTrack.startVideoFrom)
                }
            } else {
                mediaTrackList.forEach { mediaTrack ->
                    if (currentTimePosition >= mediaTrack.position
                            && currentTimePosition <= mediaTrack.position + mediaTrack.duration) {
                        mediaTrack.player?.playWhenReady = true
                    } else if (currentTimePosition < mediaTrack.position) {
                        // preset all players start positions
                        mediaTrack.player?.setSeekParameters(SeekParameters.EXACT)
                        mediaTrack.player?.seekTo(mediaTrack.startVideoFrom)
                    }
                }
            }

            playDisposable = Observable.timer(150, TimeUnit.MILLISECONDS)
                    .subscribeOn(schedulerProvider.background())
                    .observeOn(schedulerProvider.main())
                    .subscribe {
                        mediaTrackList.forEach { mediaTrack ->
                            mediaTrack.player?.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                        }
                        action.invoke()
                    }

            playDisposable?.let { compositeDisposable.add(it) }
        }
    }

    private fun executeLongSkip(action: () -> Unit) {
        notifyPlayerControlView(PlayerControlState.Seek)
        val disposable = Flowable.interval(0, 30, TimeUnit.MILLISECONDS)
                .subscribeOn(schedulerProvider.background())
                .observeOn(schedulerProvider.main())
                .takeWhile {
                    weakView.get()?.getPlayerControlView()?.isSkipping == true
                }
                .flatMapMaybe {
                    Maybe.just(action.invoke())
                }
                .delay(33, TimeUnit.MILLISECONDS)
                .subscribeOn(schedulerProvider.background())
                .observeOn(schedulerProvider.main())
                .map {
                    val milliSeconds = weakView.get()?.getGLSurfaceView()?.getTimePosition() ?: 0
                    val value: Float = milliSeconds.toFloat() / 1000f
                    weakView.get()?.onTimeLineChanged(value)
                }
                .subscribe()

        compositeDisposable.add(disposable)
    }

    private fun executeSingleTargetSeek(action: () -> Unit) {
        isSingleSeekProcessing = true

        var seekDelayMillis = 1000L
        if (singleTargetSeekDisposable?.isDisposed == false) {
            singleTargetSeekDisposable?.dispose()
            seekDelayMillis = 2000L
        } else {
            notifyPlayerControlView(PlayerControlState.Seek)
            mediaTrackList.forEach { mediaTrack ->
                mediaTrack.player?.setSeekParameters(SeekParameters.EXACT)
            }
        }

        singleTargetSeekDisposable = Maybe.fromCallable { action.invoke() }
                .delay(50, TimeUnit.MILLISECONDS)
                .observeOn(schedulerProvider.main())
                .map {
                    val milliSeconds = weakView.get()?.getGLSurfaceView()?.getTimePosition() ?: 0
                    val value: Float = milliSeconds.toFloat() / 1000f
                    weakView.get()?.onTimeLineChanged(value)
                }
                .delay(seekDelayMillis, TimeUnit.MILLISECONDS)
                .subscribeOn(schedulerProvider.background())
                .observeOn(schedulerProvider.main())
                .subscribe {
                    weakView.get()?.getGLSurfaceView()?.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                    mediaTrackList.forEach { mediaTrack ->
                        mediaTrack.player?.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                    }
                    notifyPlayerControlView(PlayerControlState.Pause)
                    isSingleSeekProcessing = false
                }

        singleTargetSeekDisposable?.let { compositeDisposable.add(it) }
    }

    private fun executeExport(action: () -> Unit) {
        // reconfigure codecs for export
        showProgress()
        releaseMediaComposition()
        retryAttempt = 0
        val disposable = Observable.timer(100, TimeUnit.MILLISECONDS)
                .subscribeOn(schedulerProvider.background())
                .observeOn(schedulerProvider.main())
                .subscribe {
                    notReadyMediaTrackIds.clear()
                    mediaTrackList = mediaTrackMapper.map(currentSettings.trackList, isEnableDecoderFallback = true, isExport = true)
                    weakView.get()?.getGLSurfaceView()?.setMediaTracks(mediaTrackList)
                    isMediaCompositionInit = true
                    executePlayerCommand(PlayerCommand.ToBegin)
                    monitorPlayerList()
                    waitReconfigureAndContinueExport(action)
                }
        compositeDisposable.add(disposable)
    }

    private fun waitReconfigureAndContinueExport(action: () -> Unit) {
        val disposable = Observable.timer(1600, TimeUnit.MILLISECONDS)
                .subscribeOn(schedulerProvider.background())
                .observeOn(schedulerProvider.main())
                .subscribe {
                    hideProgress()
                    if (notReadyMediaTrackIds.isNotEmpty()) {
                        reconfigureCodecsForPlayback()
                        showErrorMessage(getErrorModel(messageResId = R.string.error__can_not_start_export))
                    } else {
                        startExport(action)
                    }
                }
        compositeDisposable.add(disposable)
    }

    private fun startExport(action: () -> Unit) {
        weakView.get()?.showProgress(true)
        mediaTrackList.forEach { mediaTrack ->
            mediaTrack.player?.setSeekParameters(SeekParameters.EXACT)
        }
        weakView.get()?.getGLSurfaceView()?.seek(0L)
        mediaTrackList.forEach { mediaTrack ->
            // preset all players start positions
            mediaTrack.player?.seekTo(mediaTrack.startVideoFrom)
        }
        weakView.get()?.onTimeLineChanged(0f)
        notifyPlayerControlView(PlayerControlState.Export)
        val disposable = Observable.timer(700, TimeUnit.MILLISECONDS)
                .subscribeOn(schedulerProvider.background())
                .observeOn(schedulerProvider.main())
                .subscribe {
                    if (currentSettings.isTestCaseMode) {
                        val resolution = weakView.get()?.getResolutionView()?.resolution?: ResolutionView.Resolution.Half
                        Log.d(TEST_TAG, "Start export MediaComposition #${testCases[testCaseIndex].testCaseId + 1}")
                        Log.d(TEST_TAG, "Export resolution: ${resolution.width}x${resolution.height}")
                        Log.d(TEST_TAG, "Current timestamp ${System.currentTimeMillis()}")
                    }
                    //TODO refactoring for encode first frame
                    System.gc()
                    action.invoke()
                }

        compositeDisposable.add(disposable)
    }

    private fun onExportFinished() {
        showAverageFps(true)
        hideProgress()
        val disposable = Observable.timer(300, TimeUnit.MILLISECONDS)
                .subscribeOn(schedulerProvider.background())
                .observeOn(schedulerProvider.main())
                .subscribe (
                        {
                            val file = File(exportAbsoluteFilePath)
                            val fileUri = Uri.fromFile(file)
                            if (currentSettings.isTestCaseMode) {
                                fileUri?.let {
                                    Log.d(TEST_TAG, "MediaComposition #${testCases[testCaseIndex].testCaseId + 1} exported successfully")
                                }?: Log.e(TEST_TAG, "Error during export TestCase #${testCases[testCaseIndex].testCaseId + 1}, file not saved")
                                waitReconfigureCodecsAndContinueNextTestCase()
                                isTestExportProcessing = false
                            } else {
                                fileUri?.let {
                                    Log.d(TEST_TAG, "MediaComposition exported successfully")
                                    val fileName = "$EXPORT_FILE_NAME-${System.currentTimeMillis()}.mp4"
                                    val dstFilePath = context.saveToVideoGallery(exportAbsoluteFilePath, fileName)
                                    val message = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        resourceManager.getString(R.string.player__success_export)

                                    } else {
                                        context.resources.getString(R.string.player__success_export_with_path, dstFilePath)
                                    }
                                    val messageModel = MessageModel(
                                            message = message,
                                            callbackDetails = MessageModel.CallbackDetails(
                                                    positiveButtonText = resourceManager.getString(R.string.Yes),
                                                    positiveKey = SHARE_CALLBACK_KEY,
                                                    negativeButtonText = resourceManager.getString(R.string.No),
                                                    negativeKey = NOT_SHARE_CALLBACK_KEY
                                            )
                                    )
                                    weakView.get()?.onExportSuccessFinished(messageModel)

                                }?: run {
                                    Log.e(TEST_TAG, "Error during export, file not saved")
                                    showErrorMessage(getErrorModel(messageResId = R.string.error__data_cannot_be_shared))
                                    reconfigureCodecsForPlayback()
                                }
                            }
                        },
                        {
                            reconfigureCodecsForPlayback()
                            if (currentSettings.isTestCaseMode) {
                                Log.e(TEST_TAG, "Error export TestCase #${testCases[testCaseIndex].testCaseId + 1}")
                                waitReconfigureCodecsAndContinueNextTestCase()
                            }
                        }
                )
        compositeDisposable.add(disposable)
    }

    fun reconfigureCodecsForPlayback() {
        releaseMediaComposition()
        val disposable = Observable.timer(300, TimeUnit.MILLISECONDS)
                .subscribeOn(schedulerProvider.background())
                .observeOn(schedulerProvider.main())
                .subscribe {
                    retryAttempt = 0
                    initMediaComposition()
                }
        compositeDisposable.add(disposable)
    }

    private fun notifyPlayerControlView(state: PlayerControlState) {
        playerControlState = state
        when (state) {
            PlayerControlState.Play -> weakView.get()?.onPlayerPlay()
            PlayerControlState.Pause -> weakView.get()?.onPlayerPause()
            PlayerControlState.Seek -> weakView.get()?.onPlayerSeek()
            PlayerControlState.Export -> weakView.get()?.onPlayerExport()
        }.exhaustive
    }

    private fun getErrorModel(@StringRes messageResId: Int): ErrorModel {
        return ErrorModel(
                messageResId = messageResId,
                callbackDetails = ErrorModel.CallbackDetails(
                        positiveKey = PlayerFragment.ERROR_CALLBACK_KEY
                )
        )
    }

    fun showErrorMessage(errorModel: ErrorModel) {
        if (!hasActiveErrorMessage) {
            hasActiveErrorMessage = true
            weakView.get()?.showError(errorModel)
        }
    }

    fun activateErrorMessages() {
        hasActiveErrorMessage = false
    }

    fun share() {
        weakView.get()?.toShare(exportAbsoluteFilePath)
    }

    fun releaseMediaComposition() {
        isMediaCompositionInit = false
        mediaTrackList.forEach {
            it.player?.clearVideoSurface()
            it.player?.release()
            it.player = null
        }
        mediaTrackList = emptyList()
    }

    fun startTestCases(selectedTestCaseIds: List<Int>, selectedExportCaseIds: List<Int>) {
        val disposable = getTestCasesUseCase.execute(object : BaseSingleObserver<GetTestCasesUseCase.ResponseValues>() {
            override fun onSuccess(data: GetTestCasesUseCase.ResponseValues) {
                if (data.testCasesSettings.isEmpty()) {
                    Log.e(TEST_TAG, "Error: test case list is empty")
                    weakView.get()?.showError(ErrorModel(messageResId = R.string.error__test_cases_empty_list))
                } else {
                    testCaseIndex = 0
                    testCases = if (selectedTestCaseIds.isEmpty()) {
                        data.testCasesSettings
                                .mapIndexed {  index, settings ->
                                    TestCaseModel(
                                            settings = settings.copy(isTestExportEnabled = true),
                                            testCaseId = index
                                    )
                                }
                    } else {
                        data.testCasesSettings
                                .mapIndexed { index, setting ->
                                    TestCaseModel(
                                            setting.copy(isTestExportEnabled = selectedExportCaseIds.contains(index)),
                                            testCaseId = index
                                    )
                                }
                                .filterIndexed { index, _ ->
                                    selectedTestCaseIds.contains(index)
                                }

                    }
                    processTestCase(testCaseIndex)
                }
            }

            override fun onError(e: Throwable) {
                Log.e(TEST_TAG, "Error load test cases")
                weakView.get()?.showError(ErrorModel(messageResId = R.string.error__test_cases_read_data))
            }
        })
        compositeDisposable.add(disposable)
    }

    private fun processTestCase(testCaseIndex: Int) {
        if (testCaseIndex < testCases.size) {
            weakView.get()?.showHideSummary(testCases[testCaseIndex].settings)
            val delay = if (testCaseIndex == 0) {
                2000L
            } else {
                15000L
            }
            Log.d(TEST_TAG, "Start init MediaComposition #${testCases[testCaseIndex].testCaseId + 1}")
            Log.d(TEST_TAG, "Current timestamp ${System.currentTimeMillis()}")
            retryAttempt = 0
            currentSettings = testCases[testCaseIndex].settings
            initMediaComposition()
            Log.d(TEST_TAG, "waiting... delay ${delay}ms")

            val disposable = Observable.timer(delay, TimeUnit.MILLISECONDS)
                    .subscribeOn(schedulerProvider.background())
                    .observeOn(schedulerProvider.main())
                    .subscribe {
                        Log.d(TEST_TAG, "Start Play MediaComposition #${testCases[testCaseIndex].testCaseId + 1}")
                        Log.d(TEST_TAG, "Current timestamp ${System.currentTimeMillis()}")
                        executePlayerCommand(PlayerCommand.Play)
                    }
            compositeDisposable.add(disposable)
        } else {
            if (testCases.isEmpty()) {
                Log.d(TEST_TAG, "Test case list is empty")
            }
            weakView.get()?.onTestCasesFinished()
        }
    }

    private fun waitReconfigureCodecsAndContinueNextTestCase() {
        val disposable = Observable.timer(1600, TimeUnit.MILLISECONDS)
                .subscribeOn(schedulerProvider.background())
                .observeOn(schedulerProvider.main())
                .subscribe {
                    hideProgress()
                    if (notReadyMediaTrackIds.isNotEmpty()) {
                        reconfigureCodecsForPlayback()
                        showErrorMessage(getErrorModel(messageResId = R.string.error__test_can_not_continue))
                    } else {

                        continueNextTestCase()
                    }
                }
        compositeDisposable.add(disposable)
    }

    private fun continueTestWithExport() {
        isTestExportProcessing = true
        executePlayerCommand(PlayerCommand.Pause)
        if (!isMediaCompositionInit) {
            Log.e(TEST_TAG, "Can't continue test cases because MediaComposition not init")
            return
        }
        val resolution = weakView.get()?.getResolutionView()?.resolution?: ResolutionView.Resolution.Half
        exportAbsoluteFilePath = getTestCaseExportAbsoluteFilePath(testCases[testCaseIndex].testCaseId)
        executePlayerCommand(PlayerCommand.Export(resolution))
    }

    private fun getTestCaseExportAbsoluteFilePath(testCaseId: Int): String {
        val fileName = "${EXPORT_FILE_NAME}_${testCaseId + 1}.mp4"
        return context.getExternalStorageFilePath(fileName)
    }

    private fun continueNextTestCase() {
        executePlayerCommand(PlayerCommand.Pause)
        testCaseIndex++
        if (testCaseIndex < testCases.size) {
            System.gc()
            releaseMediaComposition()
            isMediaCompositionInit = false
            val delay = 700L
            Log.d(TEST_TAG, "Start delete MediaComposition #${testCases[testCaseIndex - 1].testCaseId + 1}")
            Log.d(TEST_TAG, "waiting... delay ${delay}ms")
            val disposable = Observable.timer(700, TimeUnit.MILLISECONDS)
                    .subscribeOn(schedulerProvider.background())
                    .observeOn(schedulerProvider.main())
                    .subscribe(
                            {
                                hideProgress()
                                Log.d(TEST_TAG, "Success deleted MediaComposition #${testCases[testCaseIndex - 1].testCaseId + 1}")
                                processTestCase(testCaseIndex)
                            },
                            {
                                hideProgress()
                                Log.e(TEST_TAG, "Error delete MediaComposition #${testCases[testCaseIndex - 1].testCaseId + 1}")
                                weakView.get()?.showError(ErrorModel(message = resourceManager.getResources().getString(R.string.error__test_cases_uninitialize, testCases[testCaseIndex - 1].testCaseId + 1)))
                            }
                    )
            weakView.get()?.showProgress(disposable)
            compositeDisposable.add(disposable)
        } else {
            weakView.get()?.onTestCasesFinished()
        }
    }
}