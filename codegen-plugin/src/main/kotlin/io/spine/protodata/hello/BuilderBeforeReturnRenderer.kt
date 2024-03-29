package io.spine.protodata.hello

import io.spine.protodata.renderer.Renderer
import io.spine.protodata.renderer.SourceFile
import io.spine.protodata.renderer.SourceFileSet
import io.spine.tools.code.Java

/**
 * The [Renderer] that inserts validation methods calls into the `build`
 * method of the message class builder.
 */
public class BuilderBeforeReturnRenderer(
    /**
     * A container with validation methods that are associated with
     * Java source file and should be inserted into the `build` method
     * of the message builder class.
     */
    private val builderValidationMethods: BuilderValidationMethods
) : Renderer<Java>(Java.lang()) {

    override fun render(sources: SourceFileSet) {
        if (!sources.outputRoot.endsWith("java")) {
            return
        }
        sources.filter {
            builderValidationMethods.hasMethods(it.relativePath)
        }.forEach(::insertValidationMethodsInvocation)
    }

    /**
     * Inserts validation method calls into the `build` method
     * of the message builder class.
     */
    private fun insertValidationMethodsInvocation(sourceFile: SourceFile) {
        val builder = sourceFile.at(BuilderBeforeReturnInsertionPoint())
            .withExtraIndentation(2)

        builderValidationMethods.methods(sourceFile.relativePath)
            .forEach(builder::add)
    }
}
