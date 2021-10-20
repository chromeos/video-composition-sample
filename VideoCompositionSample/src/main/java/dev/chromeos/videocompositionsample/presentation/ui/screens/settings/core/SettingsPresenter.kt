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

package dev.chromeos.videocompositionsample.presentation.ui.screens.settings.core

import dev.chromeos.videocompositionsample.domain.entity.Settings
import dev.chromeos.videocompositionsample.domain.interactor.base.observers.BaseCompletableObserver
import dev.chromeos.videocompositionsample.domain.interactor.base.observers.BaseSingleObserver
import dev.chromeos.videocompositionsample.domain.interactor.settings.GetSettingsUseCase
import dev.chromeos.videocompositionsample.domain.interactor.settings.PutSettingsUseCase
import dev.chromeos.videocompositionsample.domain.interactor.test.GetTestCasesUseCase
import dev.chromeos.videocompositionsample.presentation.R
import dev.chromeos.videocompositionsample.presentation.tools.exception.ErrorModel
import dev.chromeos.videocompositionsample.presentation.ui.base.BasePresenter
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

class SettingsPresenter
@Inject constructor(private val getSettingsUseCase: GetSettingsUseCase,
                    private val putSettingsUseCase: PutSettingsUseCase,
                    private val getTestCasesUseCase: GetTestCasesUseCase
) : BasePresenter<ISettingsView>(CompositeDisposable()) {

    fun getSettings() {
        val disposable = getSettingsUseCase.execute(object : BaseSingleObserver<GetSettingsUseCase.ResponseValues>() {
            override fun onSuccess(data: GetSettingsUseCase.ResponseValues) {
                weakView.get()?.onGetSettings(data.settings)
            }

            override fun onError(e: Throwable) {
                weakView.get()?.showError(ErrorModel(messageResId = R.string.error__settings_cannot_be_loaded))
            }
        })
        compositeDisposable.add(disposable)
    }

    fun putSettings(settings: Settings) {
        val disposable = putSettingsUseCase.execute(object : BaseCompletableObserver() {
            override fun onComplete() {
                weakView.get()?.onPutSettings(settings)
            }

            override fun onError(e: Throwable) {
                weakView.get()?.showError(ErrorModel(messageResId = R.string.error__settings_cannot_be_saved))

            }
        }, PutSettingsUseCase.RequestValues(settings))
        compositeDisposable.add(disposable)
    }

    fun getTestCases() {
        val disposable = getTestCasesUseCase.execute(object : BaseSingleObserver<GetTestCasesUseCase.ResponseValues>() {
            override fun onSuccess(data: GetTestCasesUseCase.ResponseValues) {
                weakView.get()?.onGetTestCases(data.testCasesSettings)
            }

            override fun onError(e: Throwable) {
                weakView.get()?.showError(ErrorModel(messageResId = R.string.error__test_cases_read_data))
            }
        })
        compositeDisposable.add(disposable)
    }
}
