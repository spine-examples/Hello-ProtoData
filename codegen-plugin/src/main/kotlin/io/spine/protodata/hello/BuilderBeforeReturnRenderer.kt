package io.spine.protodata.hello

import io.spine.protodata.renderer.Renderer
import io.spine.protodata.renderer.SourceFile
import io.spine.protodata.renderer.SourceFileSet
import io.spine.tools.code.Java

/**
 * The [Renderer] that inserts validation methods calls into the `build`
 * method of a message class builder just before the `return` statement.
 */
public class BuilderBeforeReturnRenderer(
    private val builderValidationMethods: BuilderValidationMethods
) : Renderer<Java>(Java.lang()) {

    override fun render(sources: SourceFileSet) {
        if (!sources.outputRoot.endsWith("java")) {
            return
        }
        sources.forEach(::insertValidationMethodsInvocation)
    }

    private fun insertValidationMethodsInvocation(sourceFile: SourceFile) {
        val fullJavaSourceFileName = sourceFile.relativePath.toString()
        assert(builderValidationMethods.hasMethods(fullJavaSourceFileName))

        val builder = sourceFile.at(BuilderBeforeReturnInsertionPoint())
            .withExtraIndentation(2)

        builderValidationMethods.methods(fullJavaSourceFileName)
            .forEach(builder::add)
    }
}
