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
                renderValidationCode(it, sources)
            }
        }
    }

    private fun renderValidationCode(
        sizeOption: SizeOption,
        sources: SourceFileSet
    ) {
        val protoFile = select<ProtobufSourceFile>().all().find {
            it.file.path.value == sizeOption.id.file.value
        }
        check(protoFile != null) {
            "Cannot find file: " + sizeOption.id.file.value
        }

        val className = sizeOption.id.type.simpleName
        val fieldName = propertyName(sizeOption.id.field.value)
        val packageName = javaPackageOption(protoFile)
        val protoFieldNames = collectFieldNamesForType(className, protoFile)
        val expression = buildExpression(
            sizeOption.sizeExpression,
            protoFieldNames
        )
        val filePath = Path.of(
            packageName.replace('.', '/'),
            className + "Ext.kt"
        )

        sources.createFile(
            filePath,
            generateFileContent(
                packageName,
                className,
                fieldName,
                expression
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
        FunSpec.builder("validate" + fieldName.camelCase() + "Count")
            .receiver(ClassName(packageName, typeName, "Builder"))
            .beginControlFlow(
                "check(%LCount == %L)", fieldName, expression
            )
            .addStatement(
                "\"'%L' count does not match the validation expression.\"",
                fieldName
            )
            .endControlFlow()
            .build()
    )
    .build()
    .toString()

private fun buildExpression(
    expression: String,
    protoFieldNames: List<String>
): String {
    var result = expression
    protoFieldNames.forEach { result = result.replace(it, propertyName(it)) }
    return result
}

private fun javaPackageOption(protoFile: ProtobufSourceFile): String {

    val optionName = "java_package"

    val option = protoFile.file.optionList.find {
        it.name == optionName
    }
    check(option != null) { "Cannot find option: $optionName" }

    return AnyPacker.unpack(
        option.value,
        StringValue::class.java
    ).value
}

private fun collectFieldNamesForType(
    simpleTypeName: String,
    protoFile: ProtobufSourceFile
): List<String> {

    val type = protoFile.typeMap.values.find {
        it.name.simpleName == simpleTypeName
    }
    check(type != null) {
        "Cannot find type '$simpleTypeName' in $protoFile"
    }
    return type.fieldList.map { it.name.value }
}

private fun propertyName(protoFieldName: String) =
    protoFieldName.camelCase().replaceFirstChar { it.lowercase() }
