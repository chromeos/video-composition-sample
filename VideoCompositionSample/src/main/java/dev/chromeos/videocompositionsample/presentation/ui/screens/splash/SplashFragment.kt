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

package dev.chromeos.videocompositionsample.presentation.ui.screens.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import dev.chromeos.videocompositionsample.presentation.R
import dev.chromeos.videocompositionsample.presentation.ui.base.BaseFragment
import dev.chromeos.videocompositionsample.presentation.ui.screens.splash.core.ISplashView
import dev.chromeos.videocompositionsample.presentation.ui.screens.splash.core.SplashPresenter
import kotlinx.android.synthetic.main.fragment_splash.*
import javax.inject.Inject

class SplashFragment: BaseFragment(), ISplashView {

    @Inject lateinit var presenter: SplashPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        component.inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.onAttach(this)
        presenter.startSplashCountDown()
        rootSplash.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                presenter.completeSplash()
            }
            true
        }
    }

    override fun onSplashComplete() {
        navigator.toPlayer(fragmentManager)
    }

    override fun onDestroyView() {
        presenter.onDetach()
        super.onDestroyView()
    }
}