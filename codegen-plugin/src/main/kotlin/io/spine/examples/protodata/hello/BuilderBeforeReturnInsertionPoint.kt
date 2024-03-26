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

import io.spine.protodata.renderer.InsertionPoint
import io.spine.text.Text
import io.spine.text.TextCoordinates
import io.spine.text.TextFactory
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.JavaSource
import org.jboss.forge.roaster.model.source.MethodSource
import java.util.regex.Pattern

/**
 * [InsertionPoint] that is the line just before `return` statement
 * in the `build` method of the message class builder.
 */
public class BuilderBeforeReturnInsertionPoint : InsertionPoint {

    public override val label: String =
        BuilderBeforeReturnInsertionPoint::class.java.simpleName

    private val returnLinePattern = Pattern.compile(
        "\\s*return .+;.*", Pattern.UNICODE_CASE or Pattern.DOTALL
    )

    /**
     * Returns the position of the line just before the `return` statement
     * of the `build` method of the message builder class.
     *
     * The position is being searched within the given [text],
     * taking into account the following:
     *
     *  - The builder class is the nested within class of some `Message`.
     *  - The class of `Message` is the only top-level class in the passed
     *  [text].
     *
     * This implementation assumes that Proto file has its `java_multiple_files`
     * option set to `true`.
     */
    public override fun locate(text: Text): Set<TextCoordinates> {
        val messageClass = parseMessageClass(text)
        val builderClass = loadBuilderClass(messageClass)
        val builderMethod = builderClass.getMethod("build")
        val lineBeforeReturn = lineBeforeReturn(builderMethod, text)
        return setOf(lineBeforeReturn)
    }

    /**
     * Returns the [TextCoordinates] that points to the line before
     * the `return` statement of the `build` method.
     */
    private fun lineBeforeReturn(
        method: MethodSource<*>,
        sourceCode: Text
    ): TextCoordinates {
        val methodCode = sourceCode.value.substring(
            method.startPosition, method.endPosition
        )
        val returnIndex = findReturnLine(methodCode)
        val beforeReturnLine = method.lineNumber + returnIndex - 1
        return atLine(beforeReturnLine)
    }

    /**
     * Searches for a line with `return` statement in the provided method.
     */
    private fun findReturnLine(methodCode: String): Int {
        val methodLines = TextFactory.lineSplitter().split(methodCode)
        for ((returnIndex, line) in methodLines.withIndex()) {
            if (returnLinePattern.matcher(line).matches()) {
                return returnIndex
            }
        }
        throw IllegalArgumentException("No `return` statement found.")
    }

    /**
     * Parses a message class from the text provided.
     */
    private fun parseMessageClass(code: Text): JavaClassSource {
        val result = Roaster.parse(JavaSource::class.java, code.value)
        check(result.isClass) { "No message class found." }
        return result as JavaClassSource
    }

    /**
     * Loads the nested builder class from the provided message class.
     */
    private fun loadBuilderClass(cls: JavaClassSource): JavaClassSource {
        val builder = cls.getNestedType("Builder")
        check(builder != null && builder.isClass)
        { "No builder class found." }
        return builder as JavaClassSource
    }
}
