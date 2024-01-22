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

import io.spine.protodata.FilePath
import io.spine.protodata.ProtobufSourceFile
import io.spine.protodata.renderer.Renderer
import io.spine.protodata.renderer.SourceFileSet
import io.spine.server.query.select
import io.spine.tools.code.Kotlin

/**
 * ProtoData [Renderer] that generates message builder extension for types
 * in which the [ArrayOfSizeOption] is used.
 *
 * For example, if the option is applied to `Board.cell` field then
 * `Board.Builder.validateCellCount()` function will be generated.
 *
 * Applicable to [Kotlin] language only.
 */
public class ValidateSizeOptionRenderer(
    private val javaSourceData: JavaSourceData
) : Renderer<Kotlin>(Kotlin.lang()) {

    override fun render(sources: SourceFileSet) {
        // Generate code for Kotlin output root only
        if (!sources.outputRoot.endsWith("kotlin")) {
            return
        }

        select(SizeOption::class.java).all()
            // Separate all size options by pair File+Type, so we can
            // generate one builder extension for options within one Type.
            .groupBy { sizeOption ->
                sizeOption.id.filePath to sizeOption.id.typeName
            }.entries.forEach { mapEntry ->

                val generator = BuilderExtensionGenerator(
                    findSourceFile(mapEntry.key.first),
                    mapEntry.key.second,
                    mapEntry.value,
                    javaSourceData
                )

                sources.createFile(
                    generator.filePath(),
                    generator.fileContent()
                )
            }
    }

    /**
     * Returns the [ProtobufSourceFile] by the [FilePath] provided.
     */
    private fun findSourceFile(filePath: FilePath): ProtobufSourceFile {
        val sourceFile = select<ProtobufSourceFile>().all().find {
            it.file.path == filePath
        }
        checkNotNull(sourceFile) {
            "Cannot find 'ProtobufSourceFile' for ${filePath.value}."
        }
        return sourceFile
    }
}
