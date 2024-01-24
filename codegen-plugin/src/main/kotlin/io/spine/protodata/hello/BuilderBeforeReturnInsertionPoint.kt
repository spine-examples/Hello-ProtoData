package io.spine.protodata.hello

import io.spine.protodata.renderer.InsertionPoint
import io.spine.text.Text
import io.spine.text.TextCoordinates
import io.spine.text.TextFactory
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.JavaSource
import org.jboss.forge.roaster.model.source.MethodSource
import java.util.regex.Pattern

/**
 * [InsertionPoint] that is the line just before `return` statement
 * in the `build` method of the message class builder.
 */
public class BuilderBeforeReturnInsertionPoint : InsertionPoint {

    public override val label: String =
        BuilderBeforeReturnInsertionPoint::class.java.simpleName

    private val returnLinePattern = Pattern.compile(
        "\\s*return .+;.*", Pattern.UNICODE_CASE or Pattern.DOTALL
    )

    /**
     * Returns the position of the line just before the `return` statement
     * of the `build` method of the message builder class. The builder class
     * is the nested within the message class. The message class is parsed
     * from the given text.
     */
    public override fun locate(text: Text): Set<TextCoordinates> {
        val messageClass = parseMessageClass(text)
        val builderClass = loadBuilderClass(messageClass)
        val builderMethod = builderClass.getMethod("build")
        val lineBeforeReturn = lineBeforeReturn(builderMethod, text)
        return setOf(lineBeforeReturn)
    }

    /**
     * Returns the [TextCoordinates] that points to the line before
     * the `return` statement of the `build` method.
     */
    private fun lineBeforeReturn(
        method: MethodSource<*>,
        sourceCode: Text
    ): TextCoordinates {
        val methodCode = sourceCode.value.substring(
            method.startPosition, method.endPosition
        )
        val returnIndex = findReturnLine(methodCode)
        val beforeReturnLine = method.lineNumber + returnIndex - 1
        return atLine(beforeReturnLine)
    }

    /**
     * Searches for a line with `return` statement in the provided method.
     */
    private fun findReturnLine(methodCode: String): Int {
        val methodLines = TextFactory.lineSplitter().split(methodCode)
        for ((returnIndex, line) in methodLines.withIndex()) {
            if (returnLinePattern.matcher(line).matches()) {
                return returnIndex
            }
        }
        throw IllegalArgumentException("No `return` statement found.")
    }

    /**
     * Parses a message class from the text provided.
     */
    private fun parseMessageClass(code: Text): JavaClassSource {
        val result = Roaster.parse(JavaSource::class.java, code.value)
        check(result.isClass) { "No message class found." }
        return result as JavaClassSource
    }

    /**
     * Loads the nested builder class from the provided message class.
     */
    private fun loadBuilderClass(cls: JavaClassSource): JavaClassSource {
        val builder = cls.getNestedType("Builder")
        check(builder != null && builder.isClass)
        { "No builder class found." }
        return builder as JavaClassSource
    }
}
