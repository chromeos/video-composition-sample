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

package dev.chromeos.videocompositionsample.presentation.ui.screens.share

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import dev.chromeos.videocompositionsample.presentation.R
import dev.chromeos.videocompositionsample.presentation.tools.extensions.putStringBuild
import dev.chromeos.videocompositionsample.presentation.ui.base.BaseFragment
import dev.chromeos.videocompositionsample.presentation.ui.screens.main.MainActivity
import java.io.File

/**
 * Separated fragment for sharing
 * TODO: investigate why on some devices the second 4k export throws MediaCodec error
 *  if share inside PlayerFragment, despite the release of codecs
 */
class ShareFragment: BaseFragment() {

    companion object {

        private const val ARGUMENT_ABSOLUTE_FILE_PATH = "ShareFragment.ARGUMENT_ABSOLUTE_FILE_PATH"

        fun newInstance(absoluteFilePath: String): Fragment {
            val fragment = ShareFragment()
            val bundle = Bundle()
                    .putStringBuild(ARGUMENT_ABSOLUTE_FILE_PATH, absoluteFilePath)

            fragment.arguments = bundle
            return fragment
        }
    }

    private val absoluteFilePath: String
        get() = arguments?.getString(ARGUMENT_ABSOLUTE_FILE_PATH)?: throw IllegalArgumentException("Can't get file path")


    override fun onCreate(savedInstanceState: Bundle?) {
        component.inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_share, container, false)
    }

    override fun onResume() {
        super.onResume()
        val file = File(absoluteFilePath)
        val fileUri: Uri = FileProvider.getUriForFile(requireContext(), dev.chromeos.videocompositionsample.presentation.BuildConfig.APPLICATION_ID + ".provider", file)
        val shareFileIntent = ShareCompat.IntentBuilder.from(requireActivity())
                .setStream(fileUri)
                .setType("text/html")
                .intent
                .setAction(Intent.ACTION_SEND)
                .setDataAndType(fileUri, "video/*")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share__subject))
                .putExtra(Intent.EXTRA_TEXT, getString(R.string.share__text))

        requireActivity().startActivityForResult(
                Intent.createChooser(shareFileIntent, getString(R.string.share__title)),
                MainActivity.SHARE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == MainActivity.SHARE_REQUEST_CODE) {
            navigator.toPlayer(fragmentManager)
        }
    }
}