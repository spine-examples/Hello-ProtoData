/*
 * Copyright 2024, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import Build_gradle.Module
import io.spine.internal.dependency.Protobuf
import io.spine.internal.dependency.Spine
import io.spine.internal.gradle.javac.configureJavac
import io.spine.internal.gradle.kotlin.applyJvmToolchain
import io.spine.internal.gradle.kotlin.setFreeCompilerArgs
import io.spine.internal.gradle.standardToSpineSdk
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    standardSpineSdkRepositories()

    val spine = io.spine.internal.dependency.Spine
    val validation = io.spine.internal.dependency.Validation
    val protoData = io.spine.internal.dependency.ProtoData
    val logging = io.spine.internal.dependency.Spine.Logging

    doForceVersions(configurations)
    configurations {
        all {
            exclude(group = "io.spine", module = "spine-logging-backend")

            resolutionStrategy {
                force(
                    io.spine.internal.dependency.Grpc.api,
                    spine.reflect,
                    spine.base,
                    spine.toolBase,
                    spine.server,
                    protoData.pluginLib(protoData.dogfoodingVersion),
                    logging.lib,
                    validation.runtime
                )
            }
        }
    }
    dependencies {
        classpath(io.spine.internal.dependency.Spine.McJava.pluginLib)
    }
}

plugins {
    kotlin("jvm")
    id("com.google.protobuf")
    id("io.spine.protodata") version "0.20.7"
}

object BuildSettings {
    private const val JAVA_VERSION = 11

    val javaVersion: JavaLanguageVersion = JavaLanguageVersion.of(JAVA_VERSION)
}


allprojects {

    // Define the repositories universally for all modules, including the root.
    repositories.standardToSpineSdk()

    val spine = io.spine.internal.dependency.Spine
    val validation = io.spine.internal.dependency.Validation
    val protoData = io.spine.internal.dependency.ProtoData
    val logging = io.spine.internal.dependency.Spine.Logging

    doForceVersions(configurations)
    configurations {
        all {
            exclude(group = "io.spine", module = "spine-logging-backend")

            resolutionStrategy {
                force(
                    io.spine.internal.dependency.Grpc.api,
                    spine.reflect,
                    spine.base,
                    spine.toolBase,
                    spine.server,
                    protoData.pluginLib(protoData.dogfoodingVersion),
                    logging.lib,
                    validation.runtime
                )
            }
        }
    }
}

// It is assumed that every module in the project requires
// a typical configuration.
subprojects {

    apply {
        plugin("kotlin")
        plugin("com.google.protobuf")
        plugin("io.spine.mc-java")
    }

    dependencies {
        api(Spine.base)

        Protobuf.libs.forEach { implementation(it) }

        // Force versions for compilation/runtime as well.
        //
        // Maybe, not all of them are required in this scope.
        // This is to investigate later.
        //
        doForceVersions(configurations)
    }

    protobuf {
        protoc {
            artifact = Protobuf.compiler
        }
    }

    // Apply a typical configuration to every module.
    applyConfiguration()
}

/**
 * The alias for typed extensions functions related to modules of this project.
 */
typealias Module = Project

fun Module.applyConfiguration() {
    configureJava()
    configureKotlin()
    setUpTests()
}

fun Module.configureKotlin() {
    kotlin {
        explicitApi()
        applyJvmToolchain(BuildSettings.javaVersion.asInt())
    }

    tasks.withType<KotlinCompile> {
        setFreeCompilerArgs()
    }
}

fun Module.setUpTests() {
    tasks.test {
        useJUnitPlatform()

        testLogging {
            events = setOf(
                TestLogEvent.PASSED,
                TestLogEvent.FAILED,
                TestLogEvent.SKIPPED
            )
            showExceptions = true
            showCauses = true
        }
    }
}

fun Module.configureJava() {
    java {
        toolchain.languageVersion.set(BuildSettings.javaVersion)
    }
    tasks {
        withType<JavaCompile>().configureEach {
            configureJavac()
        }
        withType<org.gradle.jvm.tasks.Jar>().configureEach {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }
}
