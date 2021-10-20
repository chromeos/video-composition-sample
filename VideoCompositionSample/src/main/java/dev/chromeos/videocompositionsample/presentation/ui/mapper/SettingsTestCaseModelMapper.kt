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

package dev.chromeos.videocompositionsample.presentation.ui.mapper

import dev.chromeos.videocompositionsample.domain.entity.Settings
import dev.chromeos.videocompositionsample.presentation.ui.model.SettingsTestCaseModel

class SettingsTestCaseModelMapper {

    private fun map(entity: Settings, isSelected: Boolean, isExportEnabled: Boolean): SettingsTestCaseModel {

        return SettingsTestCaseModel(
                settings = entity,
                isSelected = isSelected,
                isExportEnabled = isExportEnabled
        )
    }

    fun mapToModel(entityList: List<Settings>, selectedTestCaseIds: List<Int>, selectedExportCaseIds: List<Int>): List<SettingsTestCaseModel> {
        return entityList.mapIndexed { index: Int, settings: Settings ->
            map(settings, selectedTestCaseIds.contains(index), selectedExportCaseIds.contains(index))
        }
    }
}