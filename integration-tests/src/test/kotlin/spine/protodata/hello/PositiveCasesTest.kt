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

import io.spine.protodata.hello.Address
import io.spine.protodata.hello.Contact
import io.spine.protodata.hello.validateAddressCount
import io.spine.protodata.hello.validateEmailCount
import io.spine.protodata.hello.validatePhoneCount
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Checks various test-cases on a valid usage of the `size` option.
 */
class `Size option test should` {

    @Nested
    inner class ` validate several options within the same message` {

        @Test
        fun `with the generated methods`() {
            buildContact()
                .validateAddressCount()
                .validateEmailCount()
                .validatePhoneCount()
        }

        @Test
        fun `with the build method`() {
            buildContact().build()
        }
    }

    @Nested
    inner class ` check that a validation error is raised` {

        @Test
        fun `by the generated methods`() {
            val builder = buildInvalidContact()

            assertThrows<IllegalStateException> {
                builder.validatePhoneCount()
            }
            assertThrows<IllegalStateException> {
                builder.validateAddressCount()
            }
            assertThrows<IllegalStateException> {
                builder.validateEmailCount()
            }
        }

        @Test
        fun `by the build method`() {
            assertThrows<IllegalStateException> {
                buildInvalidContact().build()
            }
        }
    }
}

private fun buildInvalidContact(): Contact.Builder {
    return Contact.newBuilder()
        .setElementCount(2)
        .addPhone("Phone")
        .addEmail("Email")
        .addAddress(buildAddress(1))
}

private fun buildContact(): Contact.Builder {
    val elementCount = 3

    val builder = Contact.newBuilder()
        .setElementCount(elementCount)

    repeat(elementCount) {
        builder
            .addPhone("Phone$it")
            .addEmail("Email$it")
            .addAddress(
                buildAddress(it)
            )
    }
    return builder
}

private fun buildAddress(seed: Int): Address.Builder {
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
