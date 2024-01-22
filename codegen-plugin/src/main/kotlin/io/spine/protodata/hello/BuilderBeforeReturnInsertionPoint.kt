package io.spine.protodata.hello

import io.spine.protodata.renderer.InsertionPoint
import io.spine.text.Text
import io.spine.text.TextCoordinates
import io.spine.text.TextFactory
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.JavaSource
import org.jboss.forge.roaster.model.source.MethodSource
import java.util.*
import java.util.regex.Pattern
import kotlin.jvm.optionals.toSet

public class BuilderBeforeReturnInsertionPoint : InsertionPoint {

    private val parsedSources = ParsedSources()

    public override val label: String =
        BuilderBeforeReturnInsertionPoint::class.java.simpleName

    private val builderClassName = "Builder"

    private val builderMethodName = "build"

    private val returnLinePattern = Pattern.compile(
        "\\s*return .+;.*", Pattern.UNICODE_CASE or Pattern.DOTALL
    )

    public override fun locate(text: Text): Set<TextCoordinates> {
        return findBuilder(text).map {
            it.getMethod(builderMethodName)
        }.filter {
            Objects.nonNull(it)
        }.map {
            findLine(it, text)
        }.toSet()
    }

    private fun findLine(method: MethodSource<*>, code: Text): TextCoordinates {
        val methodSource = code.value.substring(
            method.startPosition, method.endPosition
        )
        val returnIndex = returnLineIndex(methodSource)
        val returnLineNumber = method.lineNumber + returnIndex
        return atLine(returnLineNumber - 1)
    }

    private fun returnLineIndex(code: String): Int {
        val methodLines = TextFactory.lineSplitter().split(code)
        for ((returnIndex, line) in methodLines.withIndex()) {
            if (returnLinePattern.matcher(line).matches()) {
                return returnIndex
            }
        }
        throw IllegalArgumentException("No return statement.")
    }

    private fun findBuilder(code: Text): Optional<JavaClassSource> {
        return findBuilder(findMessageClass(code))
    }

    private fun findMessageClass(code: Text): JavaClassSource {
        return parseSource(code) as JavaClassSource
    }

    private fun findBuilder(cls: JavaClassSource): Optional<JavaClassSource> {
        val builder = cls.getNestedType(builderClassName)
        if (builder == null || !builder.isClass) {
            return Optional.empty()
        }
        return Optional.of(builder as JavaClassSource)
    }

    private fun parseSource(code: Text): JavaSource<*> {
        return parsedSources[code]
    }
}
