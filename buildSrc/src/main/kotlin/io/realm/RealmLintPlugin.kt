/*
 * Copyright 2020 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.realm.kotlin

import Versions
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Bundling
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.fileTree
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

class RealmLintPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("io.gitlab.arturbosch.detekt")
        val configDir = locateConfigDir(target.rootDir).path

        target.allprojects {
            plugins.apply("io.gitlab.arturbosch.detekt")
            configureKtlint(this, configDir)
            configureDetekt(this, configDir)
        }
    }

    private fun configureKtlint(project: Project, configDir: String) {
        val ktlint = project.configurations.create("ktlint")
        val ktlintDependency = project.dependencies.add(
            ktlint.name,
            "com.pinterest:ktlint:${Versions.ktlint}"
        )
        if (ktlintDependency is ModuleDependency) {
            ktlintDependency.attributes {
                attribute(
                    Bundling.BUNDLING_ATTRIBUTE,
                    project.objects.named(Bundling.EXTERNAL)
                )
            }
        }

        val outputDir = "${project.buildDir}/reports/ktlint/"
        val inputFiles = project.fileTree(
            mapOf("dir" to "src", "include" to "**/*.kt")
        )

        project.tasks.register<JavaExec>("ktlintCheck") {
            inputs.files(inputFiles)
            outputs.dir(outputDir)

            description = "Check Kotlin code style."
            classpath = ktlint
            jvmArgs = listOf("--add-opens=java.base/java.lang=ALL-UNNAMED")
            mainClass.set("com.pinterest.ktlint.Main")
            args(
                "src/**/*.kt",
                "!src/**/generated/**",
                "!src/**/resources/**",
                "--reporter=plain",
                "--reporter=html,output=${project.buildDir}/reports/ktlint/ktlint.html",
                "--reporter=checkstyle,output=${project.buildDir}/reports/ktlint/ktlint.xml",
                "--editorconfig=${configDir}/ktlint/.editorconfig"
            )
        }

        project.tasks.register<JavaExec>("ktlintFormat") {
            inputs.files(inputFiles)
            outputs.dir(outputDir)

            description = "Fix Kotlin code style deviations."
            classpath = ktlint
            jvmArgs = listOf("--add-opens=java.base/java.lang=ALL-UNNAMED")
            mainClass.set("com.pinterest.ktlint.Main")
            args(
                "-F",
                "src/**/*.kt",
                "!src/**/resources/**"
            )
        }
    }

    private fun configureDetekt(project: Project, configDir: String) {
        project.extensions.configure<DetektExtension> {
            buildUponDefaultConfig = true
            config.from(project.files("$configDir/detekt/detekt.yml"))
            baseline = project.file("$configDir/detekt/baseline.xml")
            source.setFrom(
                project.files(
                    project.file("src/androidMain/kotlin"),
                    project.file("src/androidAndroidTest/kotlin"),
                    project.file("src/androidTest/kotlin"),
                    project.file("src/commonMain/kotlin"),
                    project.file("src/commonTest/kotlin"),
                    project.file("src/darwin/kotlin"),
                    project.file("src/ios/kotlin"),
                    project.file("src/iosMain/kotlin"),
                    project.file("src/iosTest/kotlin"),
                    project.file("src/jvm/kotlin"),
                    project.file("src/jvmMain/kotlin"),
                    project.file("src/main/kotlin"),
                    project.file("src/macosMain/kotlin"),
                    project.file("src/macosTest/kotlin"),
                    project.file("src/test/kotlin")
                )
            )
        }

        project.tasks.withType<Detekt>().configureEach {
            reports {
                xml.required.set(true)
                html.required.set(true)
                txt.required.set(true)
                sarif.required.set(true)
                md.required.set(true)
            }
        }
    }

    private fun locateConfigDir(current: File): File {
        val configDir = Paths.get(current.path, "config")
        return if (Files.exists(configDir) && File(configDir.toUri()).isDirectory) {
            configDir.toFile()
        } else {
            val parent = current.parentFile ?: error("Couldn't locate config folder upwards in the file tree")
            locateConfigDir(parent)
        }
    }
}
