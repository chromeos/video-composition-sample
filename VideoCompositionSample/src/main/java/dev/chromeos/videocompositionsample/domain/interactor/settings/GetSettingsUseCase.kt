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

package dev.chromeos.videocompositionsample.domain.interactor.settings

import dev.chromeos.videocompositionsample.domain.interactor.base.SingleUseCase
import dev.chromeos.videocompositionsample.domain.entity.Settings
import dev.chromeos.videocompositionsample.domain.repository.ISettingsRepo
import dev.chromeos.videocompositionsample.domain.schedulers.ISchedulerProvider
import io.reactivex.Single
import javax.inject.Inject

class GetSettingsUseCase
@Inject constructor(schedulerProvider: ISchedulerProvider,
                    private val settingRepo: ISettingsRepo
) : SingleUseCase<SingleUseCase.RequestValues, GetSettingsUseCase.ResponseValues>(schedulerProvider) {

    override fun buildUseCase(requestValues: RequestValues?): Single<ResponseValues> {
        return Single.just(settingRepo.get())
                .map { ResponseValues((it)) }
    }

    data class ResponseValues(val settings: Settings) : SingleUseCase.ResponseValues
}