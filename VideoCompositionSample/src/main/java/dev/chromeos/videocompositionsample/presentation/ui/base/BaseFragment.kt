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

import androidx.fragment.app.Fragment
import dev.chromeos.videocompositionsample.presentation.VideoCompositionSampleApp
import dev.chromeos.videocompositionsample.presentation.di.AppComponent
import dev.chromeos.videocompositionsample.presentation.tools.exception.ErrorModel
import dev.chromeos.videocompositionsample.presentation.tools.info.MessageModel
import dev.chromeos.videocompositionsample.presentation.tools.logger.ILogger
import dev.chromeos.videocompositionsample.presentation.ui.Navigator
import io.reactivex.disposables.Disposable
import javax.inject.Inject

open class BaseFragment: Fragment(), IBaseView {

    @Inject lateinit var navigator: Navigator
    @Inject lateinit var logger: ILogger

    protected val component: AppComponent by lazy {
        ((activity as? BaseActivity)?.application as? VideoCompositionSampleApp)
            ?.appComponent
            ?: throw IllegalStateException("Exception while instantiating fragment component.")
    }

    override fun showProgress(disposable: Disposable?,  withPercent: Boolean) {
        (activity as? BaseActivity)?.showProgress(disposable)
    }

    override fun showProgress(withPercent: Boolean) {
        (activity as? BaseActivity)?.showProgress(withPercent)
    }

    override fun setProgressPercent(percent: Int) {
        (activity as? BaseActivity)?.setProgressPercent(percent)
    }

    override fun hideProgress() {
        (activity as? BaseActivity)?.hideProgress()
    }

    override fun showError(errorModel: ErrorModel) {
        showError(errorModel.getMessage(requireContext()))
    }

    override fun showError(message: String) {
        if (activity is BaseActivity)
            (activity as BaseActivity).onShowError(message)
    }

    override fun onShowMessage(message: String) {
        if (activity is BaseActivity)
            (activity as BaseActivity).onShowMessage(message)
    }

    override fun onShowMessage(messageModel: MessageModel) {
        if (activity is BaseActivity)
            (activity as BaseActivity).onShowMessage(messageModel)
    }

    fun removeAllFragment() {
        if (activity is BaseActivity)
            (activity as BaseActivity).removeAllFragment()
    }
}