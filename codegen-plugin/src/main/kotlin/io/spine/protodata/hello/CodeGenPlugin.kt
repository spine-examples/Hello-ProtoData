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

package io.spine.protodata.hello

import io.spine.core.EventContext
import io.spine.core.External
import io.spine.core.Subscribe
import io.spine.core.Where
import io.spine.protobuf.AnyPacker
import io.spine.protodata.event.FieldOptionDiscovered
import io.spine.protodata.plugin.Plugin
import io.spine.protodata.plugin.View
import io.spine.protodata.plugin.ViewRepository
import io.spine.protodata.renderer.Renderer
import io.spine.protodata.renderer.SourceFileSet
import io.spine.server.route.EventRoute
import io.spine.server.route.EventRouting
import io.spine.tools.code.Java
import io.spine.tools.code.Kotlin

public class CodeGenPlugin : Plugin {

    override fun renderers(): List<Renderer<*>> {
        return listOf(
            KotlinRenderer(),
            JavaRenderer()
        )
    }

    override fun viewRepositories(): Set<ViewRepository<*, *, *>> {
        return setOf(Repository())
    }
}

public class SizeOptionView : View<SizeOptionProjectionId,
        SizeOptionProjection,
        SizeOptionProjection.Builder>() {

    private companion object FieldOptions {
        const val NAME = "option.name"
        const val VALUE = "size"
    }

    @Subscribe
    internal fun on(
        @External @Where(
            field = NAME,
            equals = VALUE
        )
        event: FieldOptionDiscovered
    ) {
        println("Size option discovered: " + event.option.value)
        val value = AnyPacker.unpack(
            event.option.value,
            ArrayOfSizeOption::class.java
        )
        println("Size option expression: " + value.value)
        builder().setSizeExpression(value.value)
    }
}

private class Repository : ViewRepository<SizeOptionProjectionId,
        SizeOptionView,
        SizeOptionProjection>() {
    override fun setupEventRouting(
        routing: EventRouting<SizeOptionProjectionId>
    ) {
        super.setupEventRouting(routing)
        println("Event routing: $routing")
        routing.route(FieldOptionDiscovered::class.java)
        { message: FieldOptionDiscovered, _: EventContext? ->
            EventRoute.withId(
                SizeOptionProjectionId.newBuilder()
                    .setFile(message.file)
                    .setType(message.type)
                    .setField(message.field)
                    .build()
            )
        }
    }
}

private class KotlinRenderer : Renderer<Kotlin>(Kotlin.lang()) {
    override fun render(sources: SourceFileSet) {
        sources.forEach { sourceFile ->
            run {
                println("Rendering Kotlin file: " + sourceFile.relativePath)
            }
        }
    }
}

private class JavaRenderer : Renderer<Java>(Java.lang()) {
    override fun render(sources: SourceFileSet) {
        sources.forEach { sourceFile ->
            run {
                println("Rendering Java file: " + sourceFile.relativePath)
            }
        }
    }
}
