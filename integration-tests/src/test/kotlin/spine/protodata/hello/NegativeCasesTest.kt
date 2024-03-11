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
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.gradle.testkit.runner.internal.DefaultGradleRunner
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.StringWriter
import java.nio.file.Path
import kotlin.io.path.div

class NegativeCasesTest {

    companion object {
        private const val RESOURCE_DIR = "size-option-plugin-test"
        private const val RESOURCE_SUB_DIR = "model"

        private lateinit var moduleDir: Path
        lateinit var project: GradleProject
        lateinit var buildException: Exception
        val buildStderr: StringWriter = StringWriter()

        @BeforeAll
        @JvmStatic
        fun compileProject(@TempDir projectDir: File) {
            project = GradleProject.setupAt(projectDir)
                .fromResources(RESOURCE_DIR)
                .copyBuildSrc()
                .create()

            (project.runner as DefaultGradleRunner)
                .withJvmArguments(
                    "-Xmx4g",
                    "-XX:MaxMetaspaceSize=512m",
                    "-XX:+HeapDumpOnOutOfMemoryError"
                )
                .forwardStdError(buildStderr)

            moduleDir = projectDir.toPath() / RESOURCE_SUB_DIR

            try {
                project.executeTask(McJavaTaskName.launchProtoData)
            } catch (e: UnexpectedBuildFailure) {
                buildException = e
            }
        }
    }

    @Test
    fun `fail if 'size' option is applied to a not repeated field`() {
        assertNotNull(
            buildException,
            "Build is finished without exception."
        )

        assertTrue(
            buildStderr.toString().contains(
                "java.lang.IllegalStateException: " +
                        "Field 'Echo.message' is not repeated and therefore " +
                        "cannot be validated with 'size' option."
            ),
            "Required exception message not found."
        )
    }
}
