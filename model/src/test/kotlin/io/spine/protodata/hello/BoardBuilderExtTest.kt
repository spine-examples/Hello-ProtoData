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

package io.spine.protodata.hello

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Tests the generated validation code for the `size` option
 * that is applied to a repeated field.
 */
class `Board builder extension test should` {

    @Nested
    inner class ` validate cell count` {

        @Test
        fun `with the generated method`() {
            buildBoard(true).validateCellCount()
        }

        @Test
        fun `with the build method`() {
            buildBoard(true).build()
        }
    }

    @Nested
    inner class ` check that a validation error is raised` {

        @Test
        fun `by the generated method`() {
            assertThrows<IllegalStateException> {
                buildBoard(false).validateCellCount()
            }
        }

        @Test
        fun `by the build method`() {
            assertThrows<IllegalStateException> {
                buildBoard(false).build()
            }
        }
    }
}

private fun buildBoard(isValid: Boolean): Board.Builder {
    val sideSize = 3
    val builder = Board.newBuilder()
        .setSideSize(sideSize)

    val cellCount = if (isValid) sideSize * sideSize else 1
    repeat(cellCount) {
        builder.addCell(Cell.newBuilder())
    }
    return builder
}
