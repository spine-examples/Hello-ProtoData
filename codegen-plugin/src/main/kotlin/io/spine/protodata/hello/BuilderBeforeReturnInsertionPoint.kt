package io.spine.protodata.hello

import io.spine.protodata.renderer.InsertionPoint
import io.spine.text.Text
import io.spine.text.TextCoordinates
import io.spine.text.TextFactory
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.MethodSource
import java.util.*
import java.util.regex.Pattern
import kotlin.jvm.optionals.toSet

/**
 * [InsertionPoint] that is the line just before `return` statement
 * in the `build` method of the message class builder.
 */
public class BuilderBeforeReturnInsertionPoint : InsertionPoint {

    private val parsedSources = ParsedSources()

    public override val label: String =
        BuilderBeforeReturnInsertionPoint::class.java.simpleName

    private val returnLinePattern = Pattern.compile(
        "\\s*return .+;.*", Pattern.UNICODE_CASE or Pattern.DOTALL
    )

    public override fun locate(text: Text): Set<TextCoordinates> {
        val messageClass = findMessageClass(text)

        return findBuilderClass(messageClass)
            .map { it.getMethod("build") }
            .filter { Objects.nonNull(it) }
            .map { lineBeforeReturn(it, text) }
            .toSet()
    }

    private fun lineBeforeReturn(
        method: MethodSource<*>,
        sourceCode: Text
    ): TextCoordinates {
        val methodCode = sourceCode.value.substring(
            method.startPosition, method.endPosition
        )
        val returnIndex = returnLineIndex(methodCode)
        val beforeReturnLineNumber = method.lineNumber + returnIndex - 1
        return atLine(beforeReturnLineNumber)
    }

    private fun returnLineIndex(methodCode: String): Int {
        val methodLines = TextFactory.lineSplitter().split(methodCode)
        for ((returnIndex, line) in methodLines.withIndex()) {
            if (returnLinePattern.matcher(line).matches()) {
                return returnIndex
            }
        }
        throw IllegalArgumentException("No `return` statement found.")
    }

    private fun findMessageClass(code: Text): JavaClassSource {
        return parsedSources[code] as JavaClassSource
    }

    private fun findBuilderClass(
        cls: JavaClassSource
    ): Optional<JavaClassSource> {
        val builder = cls.getNestedType("Builder")
        if (builder == null || !builder.isClass) {
            return Optional.empty()
        }
        return Optional.of(builder as JavaClassSource)
    }
}
