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

import dev.chromeos.videocompositionsample.presentation.tools.exception.ErrorModel
import dev.chromeos.videocompositionsample.presentation.tools.info.MessageModel
import io.reactivex.disposables.Disposable

interface IBaseView {

    fun showProgress(disposable: Disposable? = null, withPercent: Boolean = false)

    fun showProgress(withPercent: Boolean = false) {
        showProgress(null, withPercent)
    }

    fun showProgress() {
        showProgress(null, false)
    }

    fun setProgressPercent(percent: Int)

    fun hideProgress()

    fun showError(message: String)

    fun showError(errorModel: ErrorModel)

    fun onShowMessage(messageModel: MessageModel)

    fun onShowMessage(message: String)
}