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

package dev.chromeos.videocompositionsample.presentation.ui.screens.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import dev.chromeos.videocompositionsample.presentation.R
import dev.chromeos.videocompositionsample.presentation.tools.extensions.putStringBuild
import dev.chromeos.videocompositionsample.presentation.ui.base.BaseDialogFragment
import kotlinx.android.synthetic.main.fragment_info_dialog.*

class InfoDialogFragment : BaseDialogFragment() {

    companion object {
        const val ARGUMENT_TITLE = "InfoDialogFragment.ARGUMENT_TITLE"
        const val ARGUMENT_SUBTITLE = "InfoDialogFragment.ARGUMENT_SUBTITLE"
        const val ARGUMENT_POSITIVE_BUTTON_TEXT = "InfoDialogFragment.ARGUMENT_POSITIVE_BUTTON_TEXT"
        const val ARGUMENT_NEGATIVE_BUTTON_TEXT = "InfoDialogFragment.ARGUMENT_NEGATIVE_BUTTON_TEXT"
        const val ARGUMENT_POSITIVE_KEY = "InfoDialogFragment.ARGUMENT_POSITIVE_KEY"
        const val ARGUMENT_NEGATIVE_KEY = "InfoDialogFragment.ARGUMENT_NEGATIVE_KEY"

        fun newInstance(
                title: String? = null,
                subTitle: String? = null,
                positiveButtonText: String? = null,
                negativeButtonText: String? = null,
                positiveKey: String? = null,
                negativeKey: String? = null
        ): Fragment {
            val fragment = InfoDialogFragment()
            val bundle = Bundle()
                    .putStringBuild(ARGUMENT_TITLE, title)
                    .putStringBuild(ARGUMENT_SUBTITLE, subTitle)
                    .putStringBuild(ARGUMENT_POSITIVE_BUTTON_TEXT, positiveButtonText)
                    .putStringBuild(ARGUMENT_NEGATIVE_BUTTON_TEXT, negativeButtonText)
                    .putStringBuild(ARGUMENT_POSITIVE_KEY, positiveKey)
                    .putStringBuild(ARGUMENT_NEGATIVE_KEY, negativeKey)

            fragment.arguments = bundle
            return fragment
        }
    }

    interface OnInfoDialogListener {
        fun onPositiveCallback(key: String)
        fun onNegativeCallback(key: String)
    }

    private var callback: OnInfoDialogListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.AppTheme_Common_Dialog)
    }

    override fun onAttach(context: Context) {
        callback = context as? OnInfoDialogListener
        super.onAttach(context)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        var viewParent = view
        while (viewParent is View) {
            viewParent.fitsSystemWindows = false
            viewParent.setOnApplyWindowInsetsListener { _, insets -> insets }
            viewParent = viewParent.parent as View?
        }
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        dialog.window?.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setWindowAnimations(R.style.AppTheme_DialogAnimation)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_info_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = arguments?.getString(ARGUMENT_TITLE)
        val subTitle = arguments?.getString(ARGUMENT_SUBTITLE)
        val positiveButtonText = arguments?.getString(ARGUMENT_POSITIVE_BUTTON_TEXT)
        val negativeButtonText = arguments?.getString(ARGUMENT_NEGATIVE_BUTTON_TEXT)
        val positiveKey = arguments?.getString(ARGUMENT_POSITIVE_KEY)
        val negativeKey = arguments?.getString(ARGUMENT_NEGATIVE_KEY)

        if (title == null) {
            tvTitle.visibility = View.GONE
        } else {
            tvTitle.visibility = View.VISIBLE
            tvTitle.text = title
        }

        if (subTitle == null) {
            tvSubtitle.visibility = View.GONE
        } else {
            tvSubtitle.visibility = View.VISIBLE
            tvSubtitle.text = subTitle
        }

        positiveButtonText?.let { btnYes.text = it }
        negativeButtonText?.let { btnNo.text = it }

        btnYes.setOnClickListener {
            callback?.onPositiveCallback(positiveKey?: "")
            dismiss()
        }

        negativeKey?.let {
            btnNo.visibility = View.VISIBLE
            btnNo.setOnClickListener {
                callback?.onNegativeCallback(negativeKey)
                dismiss()
            }
        }?: run {
            btnNo.visibility = View.GONE
        }
    }
}