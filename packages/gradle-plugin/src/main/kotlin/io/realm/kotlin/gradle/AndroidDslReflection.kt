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
import java.lang.reflect.Field
import java.lang.reflect.Method

internal fun extractAndroidTargetInfo(androidExtension: Any?): TargetInfo {
    val defaultConfig = readMember(androidExtension, "defaultConfig")
    val minSdk = readSdkVersion(
        readMember(defaultConfig, "minSdk")
            ?: readMember(defaultConfig, "minSdkVersion")
            ?: readMember(defaultConfig, "minSdkPreview")
    )
    val targetSdk = readSdkVersion(
        readMember(defaultConfig, "targetSdk")
            ?: readMember(defaultConfig, "targetSdkPreview")
            ?: readMember(defaultConfig, "targetSdkVersion")
    )
    val targetCpuArch = readStringCollection(readMember(readMember(defaultConfig, "ndk"), "abiFilters"))
        ?.singleOrNull()
        ?.let(::androidArch)
        ?: "Universal"
    return TargetInfo("Android", targetCpuArch, targetSdk, minSdk)
}

private fun readSdkVersion(value: Any?): String? = when (value) {
    null -> null
    is Number -> value.toString()
    is String -> value
    else -> readMember(value, "apiString", "codename")?.toString()
}

private fun readStringCollection(value: Any?): List<String>? = when (value) {
    null -> null
    is Iterable<*> -> value.mapNotNull { it?.toString() }
    is Array<*> -> value.mapNotNull { it?.toString() }
    else -> null
}

private fun readMember(instance: Any?, vararg names: String): Any? {
    if (instance == null) {
        return null
    }
    names.forEach { name ->
        findNoArgMethod(instance.javaClass, name)?.let { method ->
            return method.invoke(instance)
        }
        findField(instance.javaClass, name)?.let { field ->
            return field.get(instance)
        }
    }
    return null
}

private fun findNoArgMethod(type: Class<*>, memberName: String): Method? {
    val getterNames = listOf(
        memberName,
        "get${memberName.replaceFirstChar(Char::uppercaseChar)}",
        "is${memberName.replaceFirstChar(Char::uppercaseChar)}"
    )
    var current: Class<*>? = type
    while (current != null) {
        current.declaredMethods.firstOrNull { method ->
            method.parameterCount == 0 && getterNames.contains(method.name)
        }?.let {
            it.isAccessible = true
            return it
        }
        current = current.superclass
    }
    return null
}

private fun findField(type: Class<*>, memberName: String): Field? {
    var current: Class<*>? = type
    while (current != null) {
        current.declaredFields.firstOrNull { it.name == memberName }?.let {
            it.isAccessible = true
            return it
        }
        current = current.superclass
    }
    return null
}
