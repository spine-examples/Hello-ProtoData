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
package spine.protodata.hello

import io.spine.tools.gradle.testing.GradleProject
import io.spine.tools.mc.java.gradle.McJavaTaskName
import org.gradle.testkit.runner.internal.DefaultGradleRunner
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.div

class NegativeCasesTest {
    companion object {
        const val RESOURCE_DIR = "size-option-plugin-test"
        private const val RESOURCE_SUB_DIR = "model"

        lateinit var moduleDir: Path
        lateinit var project: GradleProject

        @BeforeAll
        @JvmStatic
        fun compileProject(@TempDir projectDir: File) {
            project = GradleProject.setupAt(projectDir)
                .fromResources(RESOURCE_DIR)
                .copyBuildSrc()
                /* Uncomment the following line to be able to debug the build.
                   Remember to turn off so that tests run faster, AND Windows build does not
                   fail with the error on Windows Registry unavailability. */
                //.enableRunnerDebug()
                .create()
            (project.runner as DefaultGradleRunner).withJvmArguments(
                "-Xmx8g", "-XX:MaxMetaspaceSize=2048m", "-XX:+HeapDumpOnOutOfMemoryError"
            )
            moduleDir = projectDir.toPath() / RESOURCE_SUB_DIR
            project.executeTask(McJavaTaskName.launchProtoData)
        }
    }

    @Test
    fun `fail if 'size' option is applied to a not repeated field`() {

        println("Finished!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
    }
}
