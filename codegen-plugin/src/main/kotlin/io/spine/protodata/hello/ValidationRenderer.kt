package io.spine.protodata.hello

import io.spine.protodata.renderer.Renderer
import io.spine.protodata.renderer.SourceFile
import io.spine.protodata.renderer.SourceFileSet
import io.spine.tools.code.Java

public class ValidationRenderer(
    private val javaSourceData: JavaSourceData
) : Renderer<Java>(Java.lang()) {

    override fun render(sources: SourceFileSet) {
        if (!sources.outputRoot.endsWith("java")) {
            return
        }
        println("Java Renderer: " + sources.outputRoot)
        sources.forEach {
            if (javaSourceData.contains(it.relativePath.toString())) {
                insertBeforeBuild(it)
            }
        }
    }

    private fun insertBeforeBuild(sourceFile: SourceFile) {
        println("Source file: " + sourceFile.relativePath)

        println("Before check")

        sourceFile.at(BuildBeforeReturnInsertionPoint())
            .withExtraIndentation(2)
            .add("validate111()")
    }
}
