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
package io.spine.examples.protodata.hello

import io.spine.tools.gradle.testing.GradleProject
import io.spine.tools.mc.java.gradle.McJavaTaskName
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.gradle.testkit.runner.internal.DefaultGradleRunner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.StringWriter

/**
 * Checks for various cases where the `size` option is used.
 *
 * Some tests check the cases when `size` option is used incorrectly.
 * In such cases the test configures and runs the build of the test project
 * in a separate Gradle process because it should be failed.
 * Such a test is accepted when the specific error message is found in stderr
 * stream of the failed build process.
 */
class `ApplySizeOptionPlugin should` {

    companion object {
        private const val TEST_PROJECT_DIR: String = "test-project"

        private const val PROTO_MODEL_FILE: String =
            "model/src/main/proto/size_option_test.proto"

        private const val TEST_CASES_DIR: String =
            "src/test/resources/negative-cases/"

        private const val EMPTY_EXPRESSION_VALUE_PROTO: String =
            TEST_CASES_DIR + "empty_expression_value.proto"

        private const val NON_REPEATED_FIELD_PROTO: String =
            TEST_CASES_DIR + "non_repeated_field.proto"
    }

    @Nested
    inner class `support several 'size' options within the same message` {

        @Test
        fun `by generating a separate validation method for every field`() {
            createValidContactBuilder()
                .validateAddressCount()
                .validateEmailCount()
                .validatePhoneCount()

            val invalidContactBuilder = createInvalidContactBuilder()
            assertThrows<IllegalStateException> {
                invalidContactBuilder
                    .validatePhoneCount()
            }
            assertThrows<IllegalStateException> {
                invalidContactBuilder
                    .validateAddressCount()
            }
            assertThrows<IllegalStateException> {
                invalidContactBuilder
                    .validateEmailCount()
            }
        }

        @Test
        fun `by calling field validation methods inside 'build()' method`() {
            createValidContactBuilder()
                .build()

            assertThrows<IllegalStateException> {
                createInvalidContactBuilder()
                    .build()
            }
        }
    }

    @Test
    fun `fail the build if 'size' option value is not set`(
        @TempDir projectDir: File
    ) {

        val expectedExceptionMessage = "Value of `size` option " +
                "for field `SizeOptionTest.empty_expression_field` is not set."

        assertBuildFailed(
            projectDir,
            File(EMPTY_EXPRESSION_VALUE_PROTO),
            expectedExceptionMessage
        )
    }

    @Test
    fun `fail the build if 'size' option is applied to non-repeated field`(
        @TempDir projectDir: File
    ) {

        val expectedExceptionMessage = "Field " +
                "`SizeOptionTest.non_repeated_field` is non-repeated " +
                "and therefore cannot be validated with `size` option."

        assertBuildFailed(
            projectDir,
            File(NON_REPEATED_FIELD_PROTO),
            expectedExceptionMessage
        )
    }

    private fun assertBuildFailed(
        projectDir: File,
        protoSourceFile: File,
        expectedExceptionMessage: String
    ) {

        val project = GradleProject.setupAt(projectDir)
            .fromResources(TEST_PROJECT_DIR)
            .copyBuildSrc()
            .create()

        val stderr = StringWriter()
        (project.runner as DefaultGradleRunner)
            .withJvmArguments(
                "-Xmx4g",
                "-XX:MaxMetaspaceSize=512m",
                "-XX:+HeapDumpOnOutOfMemoryError"
            )
            .forwardStdError(stderr)

        protoSourceFile.copyTo(File(projectDir, PROTO_MODEL_FILE), true)

        try {
            project.executeTask(McJavaTaskName.launchProtoData)
            fail("The build is unexpectedly successful.")
        } catch (_: UnexpectedBuildFailure) {
        }

        // Uncomment the line below to see the content of stderr stream.
        //File("stderr.log").writeText(stderr.toString())

        assertTrue(
            stderr.toString().contains(expectedExceptionMessage),
            "The expected exception was not thrown."
        )
    }
}

private fun createInvalidContactBuilder(): Contact.Builder {
    return Contact.newBuilder()
        .setElementCount(2)
        .addPhone("Phone")
        .addEmail("Email")
        .addAddress(createAddressBuilder(1))
}

private fun createValidContactBuilder(): Contact.Builder {
    val elementCount = 3

    val builder = Contact.newBuilder()
        .setElementCount(elementCount)

    repeat(elementCount) {
        builder
            .addPhone("Phone$it")
            .addEmail("Email$it")
            .addAddress(
                createAddressBuilder(it)
            )
    }
    return builder
}

private fun createAddressBuilder(seed: Int): Address.Builder {
    val numberOfLines = 2

    val builder = Address.newBuilder()
        .setNumberOfLines(numberOfLines)
        .setZipcode("Zipcode$seed")
        .setCountry("Country$seed")

    repeat(numberOfLines) {
        builder.addAddressLine("AddressLine$seed$it")
    }

    return builder
}