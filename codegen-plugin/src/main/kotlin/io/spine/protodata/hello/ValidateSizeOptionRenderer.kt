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

import com.google.protobuf.StringValue
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import io.spine.protobuf.AnyPacker
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
                renderSizeOptionValidationCode(it, sources)
            }
        }
    }

    private fun renderSizeOptionValidationCode(
        sizeOption: SizeOption,
        sources: SourceFileSet
    ) {
        val protobufSourceFile = findSourceFile(sizeOption)

        val packageName = protobufSourceFile.javaPackage()
        val shortClassName = sizeOption.id.typeName.simpleName
        val fieldName = sizeOption.id.fieldName.value
        val validationExpression = buildExpression(
            sizeOption.validationExpression,
            protobufSourceFile.fieldNames(shortClassName)
        )

        sources.createFile(
            Path.of(
                packageName.replace('.', '/'),
                shortClassName + "Ext.kt"
            ),
            generateBuilderExtension(
                packageName,
                shortClassName,
                fieldName,
                validationExpression
            )
        )
    }

    private fun findSourceFile(sizeOption: SizeOption): ProtobufSourceFile {
        val protobufSourceFile = select<ProtobufSourceFile>().all().find {
            it.file.path.value == sizeOption.id.filePath.value
        }
        check(protobufSourceFile != null) {
            "Cannot find 'ProtobufSourceFile' for " +
                    sizeOption.id.filePath.value
        }
        return protobufSourceFile
    }
}

private fun generateBuilderExtension(
    packageName: String,
    shortClassName: String,
    fieldName: String,
    validationExpression: String
) = FileSpec.builder(ClassName(packageName, shortClassName))
    .indent("    ")
    .addFunction(
        FunSpec.builder("validate" + fieldName.camelCase() + "Count")
            .receiver(ClassName(packageName, shortClassName, "Builder"))
            .addModifiers(KModifier.INTERNAL)
            .addStatement("val expectedValue = $validationExpression")
            .beginControlFlow(
                "check(%LCount == expectedValue)",
                fieldName.propertyName()
            )
            .addStatement(
                "\"Invalid number of '%1L' elements. Expected \$expectedValue" +
                        ", but actual \$%1LCount.\"",
                fieldName.propertyName()
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

private fun ProtobufSourceFile.javaPackage(): String {
    val optionName = "java_package"

    val option = file.optionList.find { it.name == optionName }
    check(option != null) { "Cannot find option '$optionName'" }

    return AnyPacker.unpack(option.value, StringValue::class.java).value
}

private fun ProtobufSourceFile.fieldNames(typeName: String): Iterable<String> {
    val type = typeMap.values.find { it.name.simpleName == typeName }
    check(type != null) { "Cannot find type '$typeName' in $filePath" }

    return type.fieldList.map { it.name.value }
}
