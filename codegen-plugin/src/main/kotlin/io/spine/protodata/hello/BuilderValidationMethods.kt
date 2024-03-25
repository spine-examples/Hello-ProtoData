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

import com.google.common.collect.ArrayListMultimap
import java.nio.file.Path

/**
 * A container to gather the validation methods that should be invoked
 * in the `build` method of the message builder class.
 */
public class BuilderValidationMethods {

    private val methodMap = ArrayListMultimap.create<Path, String>()

    /**
     * Associates a builder validation method with the Java source file.
     *
     * @param javaSourceFilePath
     *          a [Path] to the generated java file.
     * @param validationMethod
     *          a string that represents a call of the validation method.
     */
    public fun linkMethod(javaSourceFilePath: Path, validationMethod: String) {
        methodMap.put(javaSourceFilePath, validationMethod)
    }

    /**
     * Checks that some builder validation methods are associated
     * with the given Java source file.
     *
     * @param javaSourceFilePath
     *          a [Path] to the generated java file.
     */
    public fun hasMethods(javaSourceFilePath: Path): Boolean {
        return methodMap.containsKey(javaSourceFilePath)
    }

    /**
     * Returns the list of validation methods that are associated
     * with the given Java source file.
     *
     * @param javaSourceFilePath
     *          a [Path] to the generated java file.
     */
    public fun methods(javaSourceFilePath: Path): Iterable<String> {
        return methodMap.get(javaSourceFilePath)
    }
}
