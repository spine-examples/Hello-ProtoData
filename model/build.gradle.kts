/* * Copyright 2023, TeamDev. All rights reserved.
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

import io.spine.internal.dependency.ErrorProne
import io.spine.internal.dependency.Protobuf
import io.spine.internal.dependency.Spine
import io.spine.internal.gradle.javac.configureErrorProne
import io.spine.internal.gradle.javac.configureJavac
import io.spine.internal.gradle.kotlin.applyJvmToolchain
import io.spine.internal.gradle.kotlin.setFreeCompilerArgs
import io.spine.internal.gradle.standardToSpineSdk
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    standardSpineSdkRepositories()
}

plugins {
    kotlin("jvm")
    id("net.ltgt.errorprone")
    id("detekt-code-analysis")
    id("com.google.protobuf")
    id("io.spine.protodata") version "0.13.2"
    idea
}

repositories {
    mavenLocal()
    standardToSpineSdk()
}

dependencies {
    // Depend on Spine Base to grab the Proto options
    // from `spine/options.proto`.
    api(Spine.base)

    protoData(project(":codegen-plugin"))

    Protobuf.libs.forEach { implementation(it) }

    ErrorProne.apply {
        errorprone(core)
    }
}

protoData {
    plugins(
        "io.spine.protodata.hello.CodeGenPlugin"
    )
}

protobuf {
    protoc {
        artifact = Protobuf.compiler
    }
}

/**
 * The alias for typed extensions functions related to modules of this project.
 */
typealias Module = Project

project.run {
    configureJava()
    configureKotlin()
    setupTests()
    applyGeneratedDirectories()
}

fun Module.configureKotlin() {
    kotlin {
        explicitApi()
        applyJvmToolchain(BuildSettings.javaVersion.asInt())
    }

    tasks.withType<KotlinCompile> {
        setFreeCompilerArgs()
        // https://stackoverflow.com/questions/38298695/gradle-disable-all-incremental-compilation-and-parallel-builds
        incremental = false
    }
}

fun Module.setupTests() {
    tasks.test {
        useJUnitPlatform()

        testLogging {
            events = setOf(TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.SKIPPED)
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
            configureErrorProne()
            // https://stackoverflow.com/questions/38298695/gradle-disable-all-incremental-compilation-and-parallel-builds
            options.isIncremental = false
        }
        withType<org.gradle.jvm.tasks.Jar>().configureEach {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }
}

/**
 * Adds directories with the generated source code to source sets of the project and
 * to IntelliJ IDEA module settings.
 */
fun Module.applyGeneratedDirectories() {

    /* The name of the root directory with the generated code. */
    val generatedDir = "${projectDir}/generated"

    val generatedMain = "$generatedDir/main"
    val generatedJava = "$generatedMain/java"
    val generatedKotlin = "$generatedMain/kotlin"
    val generatedGrpc = "$generatedMain/grpc"
    val generatedSpine = "$generatedMain/spine"

    val generatedTest = "$generatedDir/test"
    val generatedTestJava = "$generatedTest/java"
    val generatedTestKotlin = "$generatedTest/kotlin"
    val generatedTestGrpc = "$generatedTest/grpc"
    val generatedTestSpine = "$generatedTest/spine"

    idea {
        module {
            generatedSourceDirs.addAll(
                files(
                    generatedJava,
                    generatedKotlin,
                    generatedGrpc,
                    generatedSpine,
                )
            )
            testSources.from(
                generatedTestJava,
                generatedTestKotlin,
                generatedTestGrpc,
                generatedTestSpine,
            )
            isDownloadJavadoc = true
            isDownloadSources = true
        }
    }
}