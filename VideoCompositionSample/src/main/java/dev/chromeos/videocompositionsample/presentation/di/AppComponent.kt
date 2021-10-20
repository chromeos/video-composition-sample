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

package dev.chromeos.videocompositionsample.presentation.di

import dev.chromeos.videocompositionsample.presentation.di.modules.CommonModule
import dev.chromeos.videocompositionsample.presentation.di.modules.RepoModule
import dev.chromeos.videocompositionsample.presentation.di.modules.SchedulerModule
import dev.chromeos.videocompositionsample.presentation.ui.screens.main.MainActivity
import dev.chromeos.videocompositionsample.presentation.ui.screens.player.PlayerFragment
import dev.chromeos.videocompositionsample.presentation.ui.screens.settings.SettingsFragment
import dev.chromeos.videocompositionsample.presentation.ui.screens.share.ShareFragment
import dev.chromeos.videocompositionsample.presentation.ui.screens.splash.SplashFragment
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [
    CommonModule::class,
    RepoModule::class,
    SchedulerModule::class
])
interface AppComponent {

    fun inject(where: MainActivity)

    fun inject(where: SplashFragment)
    fun inject(where: PlayerFragment)
    fun inject(where: SettingsFragment)
    fun inject(where: ShareFragment)

}