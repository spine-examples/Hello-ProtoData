/*
 * Copyright 2024, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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
package io.spine.examples.protodata.hello.plugin

import io.spine.examples.protodata.hello.ArrayOfSizeOption
import io.spine.protodata.plugin.Plugin
import io.spine.protodata.plugin.ViewRepository
import io.spine.protodata.render.Renderer

/**
 * ProtoData [Plugin] that renders validation code for the [ArrayOfSizeOption]
 * which is applied to a repeated field.
 *
 * Value of the option is an expression that may refer to other fields
 * of the message type and supports basic math operations,
 * such as `+`, `-`, `*`, `/`.
 *
 * Example of definition:
 * ```
 * message Board {
 *     repeated Cell cell = 1 [(required) = true,
 *         (size).value = "side_size * side_size"];
 *
 *     int32 side_size = 2 [(required) = true, (min).value = "3"];
 * }
 * ```
 */
public class ApplySizeOptionPlugin : Plugin {

    override fun renderers(): List<Renderer<*>> {
        // A container for collecting generated validation methods, which should
        // later be added to the `build` method of the message builder class.
        val builderValidationMethods = BuilderValidationMethods()

        return listOf(
            ValidateSizeOptionRenderer(builderValidationMethods),
            BuilderBeforeReturnRenderer(builderValidationMethods)
        )
    }

    override fun viewRepositories(): Set<ViewRepository<*, *, *>> {
        return setOf(SizeOptionViewRepository())
    }
}
