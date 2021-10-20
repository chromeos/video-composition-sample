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

package dev.chromeos.videocompositionsample.domain.interactor.base

import dev.chromeos.videocompositionsample.domain.interactor.base.observers.BaseSingleObserver
import dev.chromeos.videocompositionsample.domain.schedulers.ISchedulerProvider
import io.reactivex.Single
import io.reactivex.disposables.Disposable

abstract class SingleUseCase<Req : SingleUseCase.RequestValues, Resp : SingleUseCase.ResponseValues>
constructor(protected val schedulerProvider: ISchedulerProvider) {

    abstract fun buildUseCase(requestValues: Req?): Single<Resp>

    fun execute(singleSubscriber: BaseSingleObserver<Resp>,
                requestValues: Req? = null): Disposable {
        return buildUseCase(requestValues)
            .subscribeOn(schedulerProvider.background())
            .observeOn(schedulerProvider.main())
            .subscribeWith(singleSubscriber)
    }

    interface RequestValues
    interface ResponseValues
}