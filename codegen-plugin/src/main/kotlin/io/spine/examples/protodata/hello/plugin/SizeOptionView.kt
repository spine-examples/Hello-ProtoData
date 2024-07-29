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
package io.spine.examples.protodata.hello.plugin

import io.spine.core.External
import io.spine.core.Subscribe
import io.spine.core.Where
import io.spine.examples.protodata.hello.ArrayOfSizeOption
import io.spine.protobuf.AnyPacker.unpack
import io.spine.protodata.event.FieldOptionDiscovered
import io.spine.protodata.plugin.View

/**
 * Records the [ArrayOfSizeOption] options that are applied to repeated fields.
 */
internal class SizeOptionView : View<SizeOptionId,
        SizeOption,
        SizeOption.Builder>() {

    /**
     * Parameters to filter the `size` option among the other options.
     */
    private companion object FilterParams {
        const val FIELD_NAME = "option.name"
        const val FIELD_VALUE = "size"
    }

    @Subscribe
    internal fun on(@External
/*        @External @Where(
            field = FIELD_NAME,
            equals = FIELD_VALUE
        )*/
        event: FieldOptionDiscovered
    ) {
        val option = unpack(event.option.value, ArrayOfSizeOption::class.java)

        println("============================== Option Read: ${option.value}")

        builder().setExpression(option.value)
    }
}
