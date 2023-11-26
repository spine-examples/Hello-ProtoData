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
import io.spine.protodata.Field
import io.spine.protodata.MessageType
import io.spine.protodata.ProtobufSourceFile
import io.spine.protodata.isRepeated
import io.spine.protodata.renderer.Renderer
import io.spine.protodata.renderer.SourceFileSet
import io.spine.server.query.select
import io.spine.string.camelCase
import io.spine.tools.code.Kotlin
import io.spine.type.typeName
import java.nio.file.Path

public class ValidateSizeOptionRenderer : Renderer<Kotlin>(Kotlin.lang()) {

    override fun render(sources: SourceFileSet) {

        // Generate code for kotlin output root only
        if (sources.outputRoot.endsWith("kotlin")) {

            select(SizeOption::class.java).all()
                .onEach { checkFieldIsRepeated(it) }
                .groupBy { it.id.typeName }.values
                .forEach() {
                    renderValidationCode(it, sources)
                }
        }
    }

    private fun renderValidationCode(
        sizeOptionsWithinOneType: List<SizeOption>,
        sources: SourceFileSet
    ) {
        check(sizeOptionsWithinOneType.isNotEmpty()) {
            "Empty list of 'size' options passed."
        }

        val protobufSourceFile = findSourceFile(sizeOptionsWithinOneType.first())
        val packageName = protobufSourceFile.javaPackage()
        val className = sizeOptionsWithinOneType.first().id.typeName.simpleName
        val fullClassName = ClassName(packageName, className)
        val fileBuilder = FileSpec.builder(fullClassName)
            .indent("    ")

        sizeOptionsWithinOneType.forEach { sizeOption ->

            val fieldName = sizeOption.id.fieldName.value.propertyName()
            val validationExpression = buildExpression(
                sizeOption.validationExpression,
                protobufSourceFile.fieldNames(className)
            )

            generateValidationFunction(
                fileBuilder,
                fullClassName,
                fieldName,
                validationExpression
            )
        }

        sources.createFile(
            Path.of(
                packageName.replace('.', '/'),
                className + "BuilderExt.kt"
            ),
            fileBuilder.build().toString()
        )
    }

    private fun checkFieldIsRepeated(sizeOption: SizeOption) {

        val sourceFile = findSourceFile(sizeOption)
        val typeName = sizeOption.id.typeName.simpleName
        val fieldName = sizeOption.id.fieldName.value

        check(sourceFile.type(typeName).field(fieldName).isRepeated()) {
            "'$typeName.$fieldName' is not repeated and therefore " +
                    "cannot be validated with 'size' option."
        }
    }

    private fun findSourceFile(sizeOption: SizeOption): ProtobufSourceFile {
        val filePath = sizeOption.id.filePath.value
        val protobufSourceFile = select<ProtobufSourceFile>().all().find {
            it.file.path.value == filePath
        }
        check(protobufSourceFile != null) {
            "Cannot find 'ProtobufSourceFile' for $filePath"
        }
        return protobufSourceFile
    }
}

private fun generateValidationFunction(
    builder: FileSpec.Builder,
    className: ClassName,
    fieldName: String,
    validationExpression: String
) = builder.addFunction(
    FunSpec.builder("validate" + fieldName.camelCase() + "Count")
        .receiver(className.nestedClass("Builder"))
        .addModifiers(KModifier.INTERNAL)
        .addStatement("val expectedValue = $validationExpression")
        .beginControlFlow(
            "check(%LCount == expectedValue)",
            fieldName
        )
        .addStatement(
            "\"Invalid number of '%L' elements: \" +",
            fieldName
        )
        .addStatement(
            "\"expected \$expectedValue, but actual \$%LCount.\"",
            fieldName
        )
        .endControlFlow()
        .build()
)

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

private fun ProtobufSourceFile.type(typeName: String): MessageType {
    val type = typeMap.values.find { it.name.simpleName == typeName }
    check(type != null) { "Cannot find type '$typeName' in $filePath" }
    return type
}

private fun ProtobufSourceFile.fieldNames(typeName: String): Iterable<String> {
    return type(typeName).fieldList.map { it.name.value }
}

private fun MessageType.field(fieldName: String): Field {
    val field = fieldList.find { it.name.value == fieldName }
    check(field != null) {
        "Cannot find field '$fieldName' in type '${typeName.simpleName()}'."
    }
    return field
}
