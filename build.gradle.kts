/*
 * Copyright 2023, TeamDev. All rights reserved.
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
import io.spine.internal.dependency.ErrorProne
import io.spine.internal.dependency.Protobuf
import io.spine.internal.dependency.Spine
import io.spine.internal.gradle.javac.configureErrorProne
import io.spine.internal.gradle.javac.configureJavac
import io.spine.internal.gradle.kotlin.applyJvmToolchain
import io.spine.internal.gradle.kotlin.setFreeCompilerArgs
import io.spine.internal.gradle.publish.PublishingRepos
import io.spine.internal.gradle.publish.SpinePublishing
import io.spine.internal.gradle.publish.spinePublishing
import io.spine.internal.gradle.standardToSpineSdk
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    standardSpineSdkRepositories()

    // Force versions, as both ProtoData and Spine Model Compiler are under active development.
    doForceVersions(configurations)

    dependencies {
        classpath(io.spine.internal.dependency.Spine.McJava.pluginLib)
    }
}

plugins {
    kotlin("jvm")
    id("net.ltgt.errorprone")
    id("detekt-code-analysis")
    id("com.google.protobuf")
    id("io.spine.protodata") version "0.14.0"
    idea
}

object BuildSettings {
    private const val JAVA_VERSION = 11

    val javaVersion: JavaLanguageVersion = JavaLanguageVersion.of(JAVA_VERSION)
}

allprojects {
    // Define the repositories universally for all modules, including the root.
    repositories.standardToSpineSdk()
}

// It is assumed that every module in the project requires
// a typical configuration.
subprojects {

    apply {
        plugin("kotlin")
        plugin("net.ltgt.errorprone")
        plugin("detekt-code-analysis")
        plugin("com.google.protobuf")
        plugin("idea")
        plugin("io.spine.mc-java")
    }

    dependencies {
        api(Spine.base)

        Protobuf.libs.forEach { implementation(it) }

        ErrorProne.apply {
            errorprone(core)
        }

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
 * Adds directories with the generated source code to source sets
 * of the project and to IntelliJ IDEA module settings.
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

spinePublishing {
    modules = subprojects.map { it.name }
        // Do not publish the validation codegen module as it is deprecated in favor of
        // ProtoData-based code generation of the Validation library.
        // The module is still kept for the sake of historical reference.
        .filter { !it.contains("mc-java-validation") }
        .toSet()
    destinations = PublishingRepos.run {
        setOf(
            cloudRepo,
            cloudArtifactRegistry,
            gitHub("mc-java"),
        )
    }
}

/**
 * Collect `publishToMavenLocal` tasks for all subprojects that are specified for
 * publishing in the root project.
 */
val publishedModules: Set<String> = extensions.getByType<SpinePublishing>().modules

val localPublish by tasks.registering {
    val pubTasks = publishedModules.map { p ->
        val subProject = project(p)
        subProject.tasks["publishToMavenLocal"]
    }
    dependsOn(pubTasks)
}
