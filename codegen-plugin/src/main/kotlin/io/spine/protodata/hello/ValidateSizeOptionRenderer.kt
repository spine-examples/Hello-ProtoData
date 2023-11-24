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

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import io.spine.protodata.ProtobufSourceFile
import io.spine.protodata.renderer.Renderer
import io.spine.protodata.renderer.SourceFileSet
import io.spine.server.query.select
import io.spine.string.camelCase
import io.spine.tools.code.Kotlin
import java.nio.file.Path

public class ValidateSizeOptionRenderer : Renderer<Kotlin>(Kotlin.lang()) {

    override fun render(sources: SourceFileSet) {

        // Generate code for kotlin output root only
        if (sources.outputRoot.endsWith("kotlin")) {

            select(SizeOption::class.java).all().forEach {
                renderValidationCode(it, sources)
            }
        }
    }

    private fun renderValidationCode(
        sizeOption: SizeOption,
        sources: SourceFileSet
    ) {
        val protobufSourceFile = select<ProtobufSourceFile>().all().find {
            it.file.path.value == sizeOption.id.filePath.value
        }
        check(protobufSourceFile != null) {
            "Cannot find 'ProtobufSourceFile' for " +
                    sizeOption.id.filePath.value
        }

        val packageName = protobufSourceFile.javaPackage()
        val typeName = sizeOption.id.typeName.simpleName
        val fieldName = sizeOption.id.fieldName.value.camelCase()
        val validationExpression = buildExpression(
            sizeOption.validationExpression,
            protobufSourceFile.fieldNames(typeName)
        )
        val filePath = Path.of(
            packageName.replace('.', '/'),
            typeName + "Ext.kt"
        )

        sources.createFile(
            filePath,
            generateFileContent(
                packageName,
                typeName,
                fieldName,
                validationExpression
            )
        )
    }
}

private fun generateFileContent(
    packageName: String,
    typeName: String,
    fieldName: String,
    expression: String
) = FileSpec.builder(ClassName(packageName, typeName))
    .indent("    ")
    .addFunction(
        FunSpec.builder("validate" + fieldName + "Count")
            .receiver(ClassName(packageName, typeName, "Builder"))
            .beginControlFlow(
                "check(%LCount == %L)",
                fieldName.propertyName(),
                expression
            )
            .addStatement(
                "\"%L count does not match the validation expression.\"",
                fieldName
            )
            .endControlFlow()
            .build()
    )
    .build()
    .toString()

private fun buildExpression(
    expression: String,
    snakeCaseFieldNames: Iterable<String>
) = mutableListOf(expression)
    .plus(snakeCaseFieldNames)
    .reduce { result, snakeCaseName ->
        result.replace(snakeCaseName, snakeCaseName.propertyName())
    }

private fun String.propertyName() =
    camelCase().replaceFirstChar { it.lowercase() }
