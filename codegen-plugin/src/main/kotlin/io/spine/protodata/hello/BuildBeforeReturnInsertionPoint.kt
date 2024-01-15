package io.spine.protodata.hello

import com.google.protobuf.GeneratedMessageV3
import io.spine.protodata.renderer.InsertionPoint
import io.spine.text.Text
import io.spine.text.TextCoordinates
import io.spine.text.TextFactory
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.JavaSource
import org.jboss.forge.roaster.model.source.MethodSource
import java.util.*
import java.util.regex.Pattern
import java.util.stream.Collectors
import java.util.stream.Stream

public class BuildBeforeReturnInsertionPoint : InsertionPoint {

    private val parsedSources = ParsedSources()

    public override val label: String =
        BuildBeforeReturnInsertionPoint::class.java.simpleName

    private val returnLinePattern = Pattern.compile(
        "\\s*return .+;.*", Pattern.UNICODE_CASE or Pattern.DOTALL
    )

    public override fun locate(text: Text): Set<TextCoordinates> {
        return findBuilders(text)
            .map { it.getMethod("build") }
            .filter { Objects.nonNull(it) }
            .map { findLine(it, text) }
            .collect(Collectors.toSet())
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

    private fun findBuilders(code: Text): Stream<JavaClassSource> {
        val classSources = findMessageClasses(code)
        return classSources.flatMap { findBuilder(it).stream() }
    }

    private fun findMessageClasses(code: Text): Stream<JavaClassSource> {
        val javaSource = parseSource(code)
        if (!javaSource.isClass) {
            return Stream.empty()
        }
        val javaClass = javaSource as JavaClassSource
        val nestedTypes: Deque<JavaSource<*>> = ArrayDeque()
        nestedTypes.add(javaClass)

        val types: Stream<JavaSource<*>> = Stream.generate {
            if (nestedTypes.isEmpty()) null else nestedTypes.poll()
        }

        val allClasses = types.filter { it.isClass }
            .map { JavaClassSource::class.java.cast(it) }
            .peek { nestedTypes.addAll(it.nestedTypes) }

        return allClasses.filter { isMessageClass(it) }
    }

    private fun parseSource(code: Text): JavaSource<*> {
        return parsedSources[code]
    }

    private fun isMessageClass(cls: JavaClassSource): Boolean {
        return GeneratedMessageV3::class.java.canonicalName == cls.superType
    }

    private fun findBuilder(cls: JavaClassSource): Optional<JavaClassSource> {
        val builder = cls.getNestedType("Builder")
        if (builder == null || !builder.isClass) {
            return Optional.empty()
        }
        val builderClass = builder as JavaClassSource
        return Optional.of(builderClass)
    }
}
