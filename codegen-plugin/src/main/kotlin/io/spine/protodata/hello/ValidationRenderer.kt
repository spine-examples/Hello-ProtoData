package io.spine.protodata.hello

import io.spine.protodata.renderer.Renderer
import io.spine.protodata.renderer.SourceFile
import io.spine.protodata.renderer.SourceFileSet
import io.spine.tools.code.Java

public class ValidationRenderer(
    private val javaValidationMethods: JavaValidationMethods
) : Renderer<Java>(Java.lang()) {

    override fun render(sources: SourceFileSet) {
        if (!sources.outputRoot.endsWith("java")) {
            return
        }
        sources.forEach(::insertBeforeBuild)
    }

    private fun insertBeforeBuild(sourceFile: SourceFile) {
        val javaFile = sourceFile.relativePath.toString()

        if (javaValidationMethods.contains(javaFile)) {

            val builder = sourceFile.at(BuilderBeforeReturnInsertionPoint())
            javaValidationMethods.getMethods(javaFile).forEach {
                builder.add(it)
            }
        }
    }
}
