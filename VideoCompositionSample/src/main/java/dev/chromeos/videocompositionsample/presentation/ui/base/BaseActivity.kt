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

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import dev.chromeos.videocompositionsample.presentation.VideoCompositionSampleApp
import dev.chromeos.videocompositionsample.presentation.R
import dev.chromeos.videocompositionsample.presentation.di.AppComponent
import dev.chromeos.videocompositionsample.presentation.tools.exception.ErrorModel
import dev.chromeos.videocompositionsample.presentation.tools.info.MessageModel
import dev.chromeos.videocompositionsample.presentation.tools.logger.ILogger
import dev.chromeos.videocompositionsample.presentation.ui.Navigator
import dev.chromeos.videocompositionsample.presentation.ui.screens.dialog.InfoDialogFragment
import io.reactivex.disposables.Disposable
import javax.inject.Inject

abstract class BaseActivity : AppCompatActivity(), InfoDialogFragment.OnInfoDialogListener, IBaseView {

    @Inject lateinit var navigator: Navigator
    @Inject lateinit var logger: ILogger

    private var progressView: View? = null
    private var progressViewPercent: TextView? = null
    private var disposable: Disposable? = null

    protected val component: AppComponent by lazy {
        (application as VideoCompositionSampleApp)
            .appComponent
    }

    override fun onStart() {
        super.onStart()
        if (progressView == null) {
            val root = findViewById<ViewGroup>(android.R.id.content)
            if (root != null) {
                progressView = layoutInflater.inflate(R.layout.progress_view, root, false)
                progressView?.visibility = View.GONE
                root.addView(progressView)
                progressViewPercent = progressView?.findViewById(R.id.tvProgressPercent)
            }
        }
    }

    override fun showProgress(disposable: Disposable?, withPercent: Boolean) {
        this.disposable = disposable
        progressView?.visibility = View.VISIBLE
        if (withPercent) {
            progressViewPercent?.visibility = View.VISIBLE
        } else {
            progressViewPercent?.visibility = View.GONE
        }
    }

    override fun setProgressPercent(percent: Int) {
        val percentStr = "$percent%"
        progressViewPercent?.text = percentStr
    }

    override fun hideProgress() {
        progressView?.visibility = View.GONE
    }

    override fun onBackPressed() {
        if (progressView?.visibility == View.VISIBLE) {
            if (disposable?.isDisposed == false) {
                disposable?.dispose()
            }
            progressView?.visibility = View.GONE
        } else {
            val currentFragment = getCurrentFragment()
            if (currentFragment is IBackPressed) {
                if (!currentFragment.onBackPressed()) {
                    if (supportFragmentManager.backStackEntryCount > 1) {
                        super.onBackPressed()
                    } else {
                        finish()
                    }
                }
            } else {
                if (supportFragmentManager.backStackEntryCount > 1) {
                    super.onBackPressed()
                } else {
                    finish()
                }
            }
        }
    }

    fun getCurrentFragment(): Fragment? {
        for (fragmentCounter in supportFragmentManager.backStackEntryCount - 1 downTo 0) {
            val tag = supportFragmentManager.getBackStackEntryAt(fragmentCounter).name
            val fragment = supportFragmentManager.findFragmentByTag(tag)
            if (fragment != null && fragment.isVisible) {
                return fragment
            }
        }
        return null
    }

    fun removeAllFragment() {
        val fragments = supportFragmentManager.fragments
        fragments.forEach { supportFragmentManager.beginTransaction().remove(it).commit() }
        supportFragmentManager.popBackStack(null, POP_BACK_STACK_INCLUSIVE)
    }

    override fun showError(errorModel: ErrorModel) {
        navigator.toInfoDialog(
                fm = supportFragmentManager,
                title = getString(R.string.label__error),
                subtitle = errorModel.getMessage(this),
                positiveButtonText = errorModel.callbackDetails?.positiveButtonText,
                negativeButtonText = errorModel.callbackDetails?.negativeButtonText,
                positiveKey = errorModel.callbackDetails?.positiveKey,
                negativeKey = errorModel.callbackDetails?.negativeKey
        )
    }

    override fun showError(message: String) {
        onShowError(message)
    }

    fun onShowError(message: String) {
        navigator.toInfoDialog(supportFragmentManager, getString(R.string.label__error), message)
    }

    override fun onShowMessage(messageModel: MessageModel) {
        navigator.toInfoDialog(
                fm = supportFragmentManager,
                title = null,
                subtitle = messageModel.getMessage(this),
                positiveButtonText = messageModel.callbackDetails?.positiveButtonText,
                negativeButtonText = messageModel.callbackDetails?.negativeButtonText,
                positiveKey = messageModel.callbackDetails?.positiveKey,
                negativeKey = messageModel.callbackDetails?.negativeKey
        )
    }

    override fun onShowMessage(message: String) {
        navigator.toInfoDialog(supportFragmentManager, null, message)
    }

    protected fun hideNavigationBar() {
        val uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_VISIBLE
        val decorView = window.decorView
        decorView.systemUiVisibility = uiOptions
    }

    override fun onPositiveCallback(key: String) {
        (getCurrentFragment() as? InfoDialogFragment.OnInfoDialogListener)?.onPositiveCallback(key)
    }

    override fun onNegativeCallback(key: String) {
        (getCurrentFragment() as? InfoDialogFragment.OnInfoDialogListener)?.onNegativeCallback(key)
    }
}