/*
 * Copyright 2026 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.realm.kotlin.compiler

import io.realm.kotlin.compiler.fir.model.RealmModelRegistrar
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension

@OptIn(ExperimentalCompilerApi::class)
class RealmCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String = "io.realm.kotlin"
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(
        configuration: CompilerConfiguration
    ) {
        messageCollector =
            configuration.get(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
        SchemaCollector.properties.clear()

        SyntheticResolveExtension.registerExtension(RealmModelSyntheticCompanionExtension())
        SyntheticResolveExtension.registerExtension(RealmModelSyntheticMethodsExtension())
        FirExtensionRegistrarAdapter.registerExtension(RealmModelRegistrar())
        IrGenerationExtension.registerExtension(RealmModelLoweringExtension())
        configuration.get(bundleIdConfigurationKey)?.let { bundleId ->
            IrGenerationExtension.registerExtension(SyncLoweringExtension(bundleId))
        }
    }
}
