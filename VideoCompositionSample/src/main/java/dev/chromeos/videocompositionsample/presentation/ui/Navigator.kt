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

package dev.chromeos.videocompositionsample.presentation.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import dev.chromeos.videocompositionsample.presentation.R
import dev.chromeos.videocompositionsample.presentation.ui.screens.dialog.InfoDialogFragment
import dev.chromeos.videocompositionsample.presentation.ui.screens.player.PlayerFragment
import dev.chromeos.videocompositionsample.presentation.ui.screens.settings.SettingsFragment
import dev.chromeos.videocompositionsample.presentation.ui.screens.share.ShareFragment
import dev.chromeos.videocompositionsample.presentation.ui.screens.splash.SplashFragment
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Navigator @Inject constructor() {

    companion object {
        val TAG_SPLASH = SplashFragment::class.java.simpleName
        val TAG_PLAYER = PlayerFragment::class.java.simpleName
        val TAG_SETTINGS = SettingsFragment::class.java.simpleName
        val TAG_SHARE = ShareFragment::class.java.simpleName
    }

    fun toSplash(fm: FragmentManager?) {
        val fragment = SplashFragment()
        fm?.beginTransaction()
                ?.setCustomAnimations(R.anim.fade_in_300, R.anim.fade_out_300, R.anim.fade_in_300, R.anim.fade_out_300)
                ?.add(R.id.fragment, fragment, TAG_SPLASH)
                ?.commitAllowingStateLoss()
    }

    fun toPlayer(fm: FragmentManager?) {
        val fragment = PlayerFragment()
        fm?.beginTransaction()
                ?.setCustomAnimations(R.anim.fade_in_300, R.anim.fade_out_300, R.anim.fade_in_300, R.anim.fade_out_300)
                ?.replace(R.id.fragment, fragment, TAG_PLAYER)
                ?.addToBackStack(TAG_PLAYER)
                ?.commitAllowingStateLoss()
    }

    fun toSettings(fragmentFrom: Fragment, fm: FragmentManager?, requestCode: Int) {
        val fragment = SettingsFragment.newInstance(requestCode)
        fragment.setTargetFragment(fragmentFrom, requestCode)
        fm?.beginTransaction()
                ?.setCustomAnimations(R.anim.fade_in_300, R.anim.fade_out_300, R.anim.fade_in_300, R.anim.fade_out_300)
                ?.add(R.id.fragment, fragment, TAG_SETTINGS)
                ?.addToBackStack(TAG_SETTINGS)
                ?.commitAllowingStateLoss()
    }

    fun toShare(fragmentFrom: Fragment, fm: FragmentManager?, absoluteFilePath: String) {
        val fragment = ShareFragment.newInstance(absoluteFilePath)
        fm?.beginTransaction()
                ?.setCustomAnimations(R.anim.fade_in_300, R.anim.fade_out_300, R.anim.fade_in_300, R.anim.fade_out_300)
                ?.remove(fragmentFrom)
                ?.add(R.id.fragment, fragment, TAG_SHARE)
                ?.commitAllowingStateLoss()
    }

    fun toInfoDialog(fm: FragmentManager?,
                     title: String? = null,
                     subtitle: String? = null,
                     positiveButtonText: String? = null,
                     negativeButtonText: String? = null,
                     positiveKey: String? = null,
                     negativeKey: String? = null
    ) {
        val fragment = InfoDialogFragment.newInstance(
                title = title,
                subTitle = subtitle,
                positiveButtonText = positiveButtonText,
                negativeButtonText = negativeButtonText,
                positiveKey = positiveKey,
                negativeKey = negativeKey
        )
        fm?.beginTransaction()
                ?.add(fragment, null)
                ?.commitAllowingStateLoss()
    }
}