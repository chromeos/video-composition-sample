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

package dev.chromeos.videocompositionsample.presentation.media.exoplayer

import android.content.Context
import android.os.Looper
import androidx.annotation.VisibleForTesting
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.analytics.AnalyticsCollector
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.upstream.BandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.util.Assertions
import com.google.android.exoplayer2.util.Clock
import com.google.android.exoplayer2.util.Util
import java.nio.ByteBuffer

class CustomSimpleExoPlayer : SimpleExoPlayer {

    var onAudioQueueInputListener: ((byteBuffer: ByteBuffer, sampleRateHz: Int, channelCount: Int) -> Unit)? = null

    private constructor(
            context: Context,
            renderersFactory: RenderersFactory,
            trackSelector: TrackSelector,
            loadControl: LoadControl,
            drmSessionManager:
            DrmSessionManager<FrameworkMediaCrypto>? = null,
            bandwidthMeter: BandwidthMeter,
            analyticsCollector: AnalyticsCollector,
            clock: Clock,
            looper: Looper
    ) : super(
            context,
            renderersFactory,
            trackSelector,
            loadControl,
            drmSessionManager,
            bandwidthMeter,
            analyticsCollector,
            clock,
            looper
    )

    private constructor(
            context: Context,
            renderersFactory: RenderersFactory,
            trackSelector: TrackSelector,
            loadControl: LoadControl,
            bandwidthMeter: BandwidthMeter,
            analyticsCollector: AnalyticsCollector,
            clock: Clock,
            looper: Looper
    ) : super(
            context,
            renderersFactory,
            trackSelector,
            loadControl,
            null,
            bandwidthMeter,
            analyticsCollector,
            clock,
            looper
    ) {
        if (renderersFactory is CustomRenderersFactory) {
            renderersFactory.onAudioQueueInputListener = { byteBuffer, sampleRateHz, channelCount ->
                onAudioInputBuffer(byteBuffer, sampleRateHz, channelCount)
            }
        }
    }

    private fun onAudioInputBuffer(byteBuffer: ByteBuffer, sampleRateHz: Int, channelCount: Int) {
        onAudioQueueInputListener?.invoke(byteBuffer, sampleRateHz, channelCount)
    }

    class Builder
    /**
     * Creates a builder.
     *
     *
     * Use [.Builder] instead, if you intend to provide a custom
     * [RenderersFactory]. This is to ensure that ProGuard or R8 can remove ExoPlayer's [ ] from the APK.
     *
     *
     * The builder uses the following default values:
     *
     *
     *  * [RenderersFactory]: [DefaultRenderersFactory]
     *  * [TrackSelector]: [DefaultTrackSelector]
     *  * [LoadControl]: [DefaultLoadControl]
     *  * [BandwidthMeter]: [DefaultBandwidthMeter.getSingletonInstance]
     *  * [Looper]: The [Looper] associated with the current thread, or the [       ] of the application's main thread if the current thread doesn't have a [       ]
     *  * [AnalyticsCollector]: [AnalyticsCollector] with [Clock.DEFAULT]
     *  * `useLazyPreparation`: `true`
     *  * [Clock]: [Clock.DEFAULT]
     *
     *
     * @param context A [Context].
     */
    constructor(
            private val context: Context,
            private val renderersFactory: RenderersFactory = DefaultRenderersFactory(context),
            private var trackSelector: TrackSelector =
                    DefaultTrackSelector(context),
            private var loadControl: LoadControl =
                    DefaultLoadControl(),
            private var bandwidthMeter: BandwidthMeter =
                    DefaultBandwidthMeter.getSingletonInstance(context),
            private var looper: Looper =
                    Util.getLooper(),
            private var analyticsCollector: AnalyticsCollector =
                    AnalyticsCollector(Clock.DEFAULT),
            private var useLazyPreparation: Boolean =  /* useLazyPreparation= */
                    true,
            private var clock: Clock =
                    Clock.DEFAULT) {
        private var buildCalled = false

        /**
         * Sets the [TrackSelector] that will be used by the player.
         *
         * @param trackSelector A [TrackSelector].
         * @return This builder.
         * @throws IllegalStateException If [.build] has already been called.
         */
        fun setTrackSelector(trackSelector: TrackSelector): Builder {
            Assertions.checkState(!buildCalled)
            this.trackSelector = trackSelector
            return this
        }

        /**
         * Sets the [LoadControl] that will be used by the player.
         *
         * @param loadControl A [LoadControl].
         * @return This builder.
         * @throws IllegalStateException If [.build] has already been called.
         */
        fun setLoadControl(loadControl: LoadControl): Builder {
            Assertions.checkState(!buildCalled)
            this.loadControl = loadControl
            return this
        }

        /**
         * Sets the [BandwidthMeter] that will be used by the player.
         *
         * @param bandwidthMeter A [BandwidthMeter].
         * @return This builder.
         * @throws IllegalStateException If [.build] has already been called.
         */
        fun setBandwidthMeter(bandwidthMeter: BandwidthMeter): Builder {
            Assertions.checkState(!buildCalled)
            this.bandwidthMeter = bandwidthMeter
            return this
        }

        /**
         * Sets the [Looper] that must be used for all calls to the player and that is used to
         * call listeners on.
         *
         * @param looper A [Looper].
         * @return This builder.
         * @throws IllegalStateException If [.build] has already been called.
         */
        fun setLooper(looper: Looper): Builder {
            Assertions.checkState(!buildCalled)
            this.looper = looper
            return this
        }

        /**
         * Sets the [AnalyticsCollector] that will collect and forward all player events.
         *
         * @param analyticsCollector An [AnalyticsCollector].
         * @return This builder.
         * @throws IllegalStateException If [.build] has already been called.
         */
        fun setAnalyticsCollector(analyticsCollector: AnalyticsCollector): Builder {
            Assertions.checkState(!buildCalled)
            this.analyticsCollector = analyticsCollector
            return this
        }

        /**
         * Sets whether media sources should be initialized lazily.
         *
         *
         * If false, all initial preparation steps (e.g., manifest loads) happen immediately. If
         * true, these initial preparations are triggered only when the player starts buffering the
         * media.
         *
         * @param useLazyPreparation Whether to use lazy preparation.
         * @return This builder.
         * @throws IllegalStateException If [.build] has already been called.
         */
        fun setUseLazyPreparation(useLazyPreparation: Boolean): Builder {
            Assertions.checkState(!buildCalled)
            this.useLazyPreparation = useLazyPreparation
            return this
        }

        /**
         * Sets the [Clock] that will be used by the player. Should only be set for testing
         * purposes.
         *
         * @param clock A [Clock].
         * @return This builder.
         * @throws IllegalStateException If [.build] has already been called.
         */
        @VisibleForTesting
        fun setClock(clock: Clock): Builder {
            Assertions.checkState(!buildCalled)
            this.clock = clock
            return this
        }

        /**
         * Builds a [SimpleExoPlayer] instance.
         *
         * @throws IllegalStateException If [.build] has already been called.
         */
        fun build(): CustomSimpleExoPlayer {
            Assertions.checkState(!buildCalled)
            buildCalled = true
            return CustomSimpleExoPlayer(
                    context,
                    renderersFactory,
                    trackSelector,
                    loadControl,
                    bandwidthMeter,
                    analyticsCollector,
                    clock,
                    looper)
        }
        /**
         * Creates a builder with the specified custom components.
         *
         *
         * Note that this constructor is only useful if you try to ensure that ExoPlayer's default
         * components can be removed by ProGuard or R8. For most components except renderers, there is
         * only a marginal benefit of doing that.
         *
         * @param context A [Context].
         * @param renderersFactory A factory for creating [Renderers][Renderer] to be used by the
         * player.
         * @param trackSelector A [TrackSelector].
         * @param loadControl A [LoadControl].
         * @param bandwidthMeter A [BandwidthMeter].
         * @param looper A [Looper] that must be used for all calls to the player.
         * @param analyticsCollector An [AnalyticsCollector].
         * @param useLazyPreparation Whether media sources should be initialized lazily.
         * @param clock A [Clock]. Should always be [Clock.DEFAULT].
         */
        /**
         * Creates a builder with a custom [RenderersFactory].
         *
         *
         * See [.Builder] for a list of default values.
         *
         * @param context A [Context].
         * @param renderersFactory A factory for creating [Renderers][Renderer] to be used by the
         * player.
         */
    }

}