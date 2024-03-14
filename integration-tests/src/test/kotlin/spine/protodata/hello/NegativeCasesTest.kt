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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.StringWriter

/**
 * Checks for various cases where the `size` option is used incorrectly.
 */
class `NegativeCasesTest should` {

    companion object {
        private const val TEST_PROJECT_DIR: String = "test-project"

        private const val TEST_CASES_DIR: String = "src/test/resources/cases/"

        private const val PROTO_MODEL_DIR: String = "model/src/main/proto/"

        private const val PROTO_SOURCE_FILE: String = "echo.proto"

        private const val EMPTY_EXPRESSION_VALUE_DIR: String = TEST_CASES_DIR +
                "empty-expression-value"

        private const val NOT_REPEATED_FIELD_DIR: String = TEST_CASES_DIR +
                "not-repeated-field"
    }

    @Test
    fun ` fail if 'size' option value is not set`(@TempDir projectDir: File) {

        val expectedExceptionMessage = "Value of `size` option " +
                "for field `Echo.message` is not set."

        assertBuildFailed(
            projectDir,
            File(EMPTY_EXPRESSION_VALUE_DIR),
            expectedExceptionMessage
        )
    }

    @Test
    fun ` fail if 'size' option is applied to not repeated field`(
        @TempDir projectDir: File
    ) {

        val expectedExceptionMessage = "Field `Echo.message` is not repeated " +
                "and therefore cannot be validated with `size` option."

        assertBuildFailed(
            projectDir,
            File(NOT_REPEATED_FIELD_DIR),
            expectedExceptionMessage
        )
    }

    private fun assertBuildFailed(
        projectDir: File,
        protoSourceDir: File,
        expectedExceptionMessage: String
    ) {

        val project = GradleProject.setupAt(projectDir)
            .fromResources(TEST_PROJECT_DIR)
            .copyBuildSrc()
            .create()

        val protoSourceFile = File(protoSourceDir, PROTO_SOURCE_FILE)

        val protoDestinationFile = File(
            File(projectDir, PROTO_MODEL_DIR),
            PROTO_SOURCE_FILE
        )

        protoSourceFile.copyTo(protoDestinationFile, true)

        val stderr = StringWriter()

        (project.runner as DefaultGradleRunner)
            .withJvmArguments(
                "-Xmx4g",
                "-XX:MaxMetaspaceSize=512m",
                "-XX:+HeapDumpOnOutOfMemoryError"
            )
            .forwardStdError(stderr)

        try {
            project.executeTask(McJavaTaskName.launchProtoData)
            fail("Build should be failed.")
        } catch (_: UnexpectedBuildFailure) {
        }

        assertTrue(
            stderr.toString().contains(expectedExceptionMessage),
            "Required exception not found."
        )
    }
}
