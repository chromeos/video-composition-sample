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

package dev.chromeos.videocompositionsample.presentation.ui.base

import dev.chromeos.videocompositionsample.presentation.tools.exception.ErrorMessageFactory
import io.reactivex.disposables.CompositeDisposable
import java.lang.ref.WeakReference

/**
 * Base class that implements the Presenter interface and provides a base implementation for
 * onAttach() and onDetach(). It also handles keeping a reference to the mvpView.
 */
abstract class BasePresenter<T : IBaseView>(protected val compositeDisposable: CompositeDisposable) {

    protected lateinit var weakView: WeakReference<T>

    fun onAttach(mvpView: T) {
        weakView = WeakReference(mvpView)
    }

    protected fun showProgress() {
        weakView.get()?.showProgress(compositeDisposable)
    }

    protected fun hideProgress() {
        weakView.get()?.hideProgress()
    }

    protected open fun onThrowable(e: Throwable) {
        hideProgress()
        weakView.get()?.showError(ErrorMessageFactory.create(e))
    }

    fun onDetach() {
        compositeDisposable.clear()
        weakView.clear()
    }
}