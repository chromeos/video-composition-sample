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

package dev.chromeos.videocompositionsample.presentation.di.modules

import dev.chromeos.videocompositionsample.domain.schedulers.IObserverSchedulerProvider
import dev.chromeos.videocompositionsample.domain.schedulers.ISchedulerProvider
import dev.chromeos.videocompositionsample.domain.schedulers.IWorkSchedulerProvider
import dev.chromeos.videocompositionsample.presentation.tools.schedulers.ObserverSchedulerProvider
import dev.chromeos.videocompositionsample.presentation.tools.schedulers.SchedulerProvider
import dev.chromeos.videocompositionsample.presentation.tools.schedulers.WorkSchedulerProvider
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class SchedulerModule {

    @Provides
    @Singleton
    fun provideWorkSchedulerProvider(workSchedulerProvider: WorkSchedulerProvider): IWorkSchedulerProvider {
        return workSchedulerProvider
    }

    @Provides
    @Singleton
    fun provideObserverSchedulerProvider(observerSchedulerProvider: ObserverSchedulerProvider): IObserverSchedulerProvider {
        return observerSchedulerProvider
    }

    @Provides
    @Singleton
    fun provideSchedulerProvider(schedulerProvider: SchedulerProvider): ISchedulerProvider {
        return schedulerProvider
    }
}