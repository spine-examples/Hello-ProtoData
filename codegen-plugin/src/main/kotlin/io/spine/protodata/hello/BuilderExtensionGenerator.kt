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
import io.spine.protobuf.AnyPacker.unpack
import io.spine.protodata.Field
import io.spine.protodata.FieldName
import io.spine.protodata.MessageType
import io.spine.protodata.ProtobufSourceFile
import io.spine.protodata.TypeName
import io.spine.protodata.isRepeated
import io.spine.string.Indent
import io.spine.string.camelCase
import io.spine.type.typeName
import java.nio.file.Path

/**
 * Generates message builder extension with a separate validation function
 * for every repeated field in which the `size` option is used.
 */
internal class BuilderExtensionGenerator(
    private val sourceFile: ProtobufSourceFile,
    private val typeName: TypeName,
    private val sizeOptions: Iterable<SizeOption>,
    private val javaSourceData: JavaSourceData

) {
    private val javaPackage = sourceFile.javaPackage()
    private val simpleTypeName = typeName.simpleName

    /**
     * Returns a [Path] to the generated file
     * that is relative to the source root.
     */
    internal fun filePath(): Path {
        return Path.of(
            javaPackage.replace('.', '/'),
            simpleTypeName + "BuilderExt.kt"
        )
    }

    /**
     * Generates content of the file with builder extension declaration.
     */
    internal fun fileContent(): String {

        val fullClassName = ClassName(javaPackage, simpleTypeName)
        val builder = FileSpec.builder(fullClassName)
            .indent(Indent.defaultJavaIndent.toString())

        sizeOptions.forEach { sizeOption ->

            checkFieldIsRepeated(sizeOption)

            val fieldName = sizeOption.id.fieldName.value.propertyName()
            val expression = buildExpression(
                sizeOption.expression,
                sourceFile.type(typeName).fieldNames()
            )
            val functionName = "validate" + fieldName.camelCase() + "Count"
            val builderClass = fullClassName.nestedClass("Builder")

            val javaFileName = Path.of(
                javaPackage.replace('.', '/'),
                "$simpleTypeName.java"
            ).toString()

            javaSourceData.addMethod(javaFileName, functionName)

            builder.addFunction(
                FunSpec.builder(functionName)
                    .returns(builderClass)
                    .receiver(builderClass)
                    .addModifiers(KModifier.INTERNAL)
                    .addStatement("val expected = $expression")
                    .beginControlFlow(
                        "check(%LCount == expected)",
                        fieldName
                    )
                    .addStatement(
                        "\"Invalid number of '%L' elements: \" +",
                        fieldName
                    )
                    .addStatement(
                        "    \"expected \$expected, but actual \$%LCount.\"",
                        fieldName
                    )
                    .endControlFlow()
                    .addStatement("return this")
                    .build()
            )
        }
        return builder.build().toString()
    }

    private fun checkFieldIsRepeated(sizeOption: SizeOption) {
        val field = sourceFile
            .type(typeName)
            .field(sizeOption.id.fieldName)

        check(field.isRepeated()) {
            "Field '$simpleTypeName.${field.name.value}' is not repeated" +
                    " and therefore cannot be validated with 'size' option."
        }
    }
}

private fun buildExpression(
    expression: String,
    snakeCaseFieldNames: Iterable<String>
) = mutableListOf(expression)
    .plus(snakeCaseFieldNames)
    .reduce { result, snakeCaseFieldName ->
        result.replace(snakeCaseFieldName, snakeCaseFieldName.propertyName())
    }

private fun String.propertyName() =
    camelCase().replaceFirstChar { it.lowercase() }

private fun ProtobufSourceFile.javaPackage(): String {
    val optionName = "java_package"
    val option = file.optionList.find { it.name == optionName }
    checkNotNull(option) {
        "Cannot find option '$optionName' in file $filePath."
    }
    return unpack(option.value, StringValue::class.java).value
}

private fun ProtobufSourceFile.type(typeName: TypeName): MessageType {
    val type = typeMap.values.find { it.name == typeName }
    checkNotNull(type) {
        "Cannot find type '$typeName' in $filePath."
    }
    return type
}

private fun MessageType.field(fieldName: FieldName): Field {
    val field = fieldList.find { it.name == fieldName }
    checkNotNull(field) {
        "Cannot find field '${fieldName.value}' in type '${typeName.value}'."
    }
    return field
}

private fun MessageType.fieldNames(): Iterable<String> {
    return fieldList.map { it.name.value }
}
