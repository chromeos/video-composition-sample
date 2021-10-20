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

package dev.chromeos.videocompositionsample.domain.interactor

import dev.chromeos.videocompositionsample.domain.exception.NullParamsUseCaseException
import dev.chromeos.videocompositionsample.domain.interactor.base.SingleUseCase
import dev.chromeos.videocompositionsample.domain.repository.IFileRepo
import dev.chromeos.videocompositionsample.domain.schedulers.ISchedulerProvider
import io.reactivex.Single
import javax.inject.Inject

class SaveDataUseCase
@Inject constructor(schedulerProvider: ISchedulerProvider,
                    private val fileRepo: IFileRepo
) : SingleUseCase<SaveDataUseCase.RequestValues, SaveDataUseCase.ResponseValues>(schedulerProvider) {

    override fun buildUseCase(requestValues: RequestValues?): Single<ResponseValues> {
        return if (requestValues != null) {
            fileRepo.save(requestValues.byteArray, requestValues.fileName)
                .map { ResponseValues(it) }
        } else {
            Single.error(NullParamsUseCaseException())
        }
    }

    data class RequestValues(val byteArray: ByteArray, val fileName: String): SingleUseCase.RequestValues
    data class ResponseValues(val absoluteFilePath: String): SingleUseCase.ResponseValues
}