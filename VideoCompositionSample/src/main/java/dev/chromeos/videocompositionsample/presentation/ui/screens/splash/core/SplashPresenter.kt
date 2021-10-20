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

package dev.chromeos.videocompositionsample.presentation.ui.screens.splash.core

import dev.chromeos.videocompositionsample.domain.schedulers.ISchedulerProvider
import dev.chromeos.videocompositionsample.presentation.ui.base.BasePresenter
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SplashPresenter
@Inject constructor(private val schedulerProvider: ISchedulerProvider)
    : BasePresenter<ISplashView>(CompositeDisposable()) {

    fun startSplashCountDown() {
        val disposable = Observable.timer(1500, TimeUnit.MILLISECONDS, schedulerProvider.background())
            .subscribeOn(schedulerProvider.background())
            .observeOn(schedulerProvider.main())
            .subscribe {
                weakView.get()?.onSplashComplete()
            }
        compositeDisposable.add(disposable)
    }

    fun completeSplash() {
        compositeDisposable.clear()
        weakView.get()?.onSplashComplete()
    }
}