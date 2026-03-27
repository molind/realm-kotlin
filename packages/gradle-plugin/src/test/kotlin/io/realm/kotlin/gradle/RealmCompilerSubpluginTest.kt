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

package io.realm.kotlin.gradle

import io.realm.kotlin.gradle.analytics.TargetInfo
import kotlin.test.Test
import kotlin.test.assertEquals

class RealmCompilerSubpluginTest {

    @Test
    fun extractAndroidTargetInfo_supportsLegacyAndroidDslObjects() {
        val targetInfo = extractAndroidTargetInfo(
            LegacyAndroidExtension(
                defaultConfig = LegacyDefaultConfig(
                    minSdkVersion = LegacyApiVersion("24"),
                    targetSdkVersion = LegacyApiVersion("35"),
                    ndk = LegacyNdk(setOf("arm64-v8a"))
                )
            )
        )

        assertEquals(TargetInfo("Android", "Arm64", "35", "24"), targetInfo)
    }

    @Test
    fun extractAndroidTargetInfo_supportsNewAndroidDslObjects() {
        val targetInfo = extractAndroidTargetInfo(
            NewAndroidExtension(
                defaultConfig = NewDefaultConfig(
                    minSdk = 26,
                    targetSdk = 36,
                    ndk = NewNdk(setOf("x86_64"))
                )
            )
        )

        assertEquals(TargetInfo("Android", "x64", "36", "26"), targetInfo)
    }

    @Test
    fun extractAndroidTargetInfo_defaultsWhenPropertiesAreMissing() {
        val targetInfo = extractAndroidTargetInfo(object {})

        assertEquals(TargetInfo("Android", "Universal", null, null), targetInfo)
    }
}

private class LegacyAndroidExtension(val defaultConfig: LegacyDefaultConfig)
private class LegacyDefaultConfig(
    val minSdkVersion: LegacyApiVersion?,
    val targetSdkVersion: LegacyApiVersion?,
    val ndk: LegacyNdk?
)
private class LegacyApiVersion(val apiString: String)
private class LegacyNdk(val abiFilters: Set<String>)

private class NewAndroidExtension(val defaultConfig: NewDefaultConfig)
private class NewDefaultConfig(
    val minSdk: Int?,
    val targetSdk: Int?,
    val ndk: NewNdk?
)
private class NewNdk(val abiFilters: Set<String>)
