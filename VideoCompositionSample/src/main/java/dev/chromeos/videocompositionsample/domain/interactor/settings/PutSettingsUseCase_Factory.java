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

// Generated by Dagger (https://dagger.dev).
package dev.chromeos.videocompositionsample.domain.interactor.settings;

import dagger.internal.Factory;
import dev.chromeos.videocompositionsample.domain.repository.ISettingsRepo;
import dev.chromeos.videocompositionsample.domain.schedulers.ISchedulerProvider;
import javax.inject.Provider;

@SuppressWarnings({
    "unchecked",
    "rawtypes"
})
public final class PutSettingsUseCase_Factory implements Factory<PutSettingsUseCase> {
  private final Provider<ISchedulerProvider> schedulerProvider;

  private final Provider<ISettingsRepo> settingRepoProvider;

  public PutSettingsUseCase_Factory(Provider<ISchedulerProvider> schedulerProvider,
      Provider<ISettingsRepo> settingRepoProvider) {
    this.schedulerProvider = schedulerProvider;
    this.settingRepoProvider = settingRepoProvider;
  }

  @Override
  public PutSettingsUseCase get() {
    return newInstance(schedulerProvider.get(), settingRepoProvider.get());
  }

  public static PutSettingsUseCase_Factory create(Provider<ISchedulerProvider> schedulerProvider,
      Provider<ISettingsRepo> settingRepoProvider) {
    return new PutSettingsUseCase_Factory(schedulerProvider, settingRepoProvider);
  }

  public static PutSettingsUseCase newInstance(ISchedulerProvider schedulerProvider,
      ISettingsRepo settingRepo) {
    return new PutSettingsUseCase(schedulerProvider, settingRepo);
  }
}
