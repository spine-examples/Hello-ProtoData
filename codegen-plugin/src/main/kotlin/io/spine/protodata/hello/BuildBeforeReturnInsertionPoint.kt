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

        println("Locate!!!!!!!!!!!!!!!!!!")

        val builders = findBuilders(text)

        println("builders: $builders")

        val map = builders.map { it.getMethod("build") }

        println("map: $map")

        val filter = map.filter { Objects.nonNull(it) }

        println("filter: $filter")

        val line = filter.map { findLine(it, text) }

        println("lines: $line")

        val result = line.collect(Collectors.toSet())

        println("Coordinates!!!!!!!!!!!!!!!!!!!!!: $result")

        return result
    }

    private fun findLine(method: MethodSource<*>, code: Text): TextCoordinates {

        println("findLine: $method")

        val methodSource = code.value.substring(
            method.startPosition, method.endPosition
        )

        println("methodSource: $methodSource")

        val returnIndex = returnLineIndex(methodSource)

        println("returnIndex: $returnIndex")

        val returnLineNumber = method.lineNumber + returnIndex

        println("returnLineNumber: $returnLineNumber")

        val result = atLine(returnLineNumber - 1)

        println("result: $result")

        return result
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

        println("findBuilders!!!!!!")

        val classSources = findMessageClasses(code)

        println("Class Sources: $classSources")

        val result = classSources.flatMap {

            println("Canonical name: " + it.canonicalName)

            findBuilder(it).stream()
        }
        return result
    }

    private fun findMessageClasses(code: Text): Stream<JavaClassSource> {

        println("1")

        val javaSource = parseSource(code)

        println("2")

        if (!javaSource.isClass) {
            return Stream.empty()
        }

        val javaClass = javaSource as JavaClassSource

        println("3")

        val nestedTypes: Deque<JavaSource<*>> = ArrayDeque()
        nestedTypes.add(javaClass)

        println("4")

        val types: Stream<JavaSource<*>?> = Stream.generate {
            if (nestedTypes.isEmpty()) null else nestedTypes.poll()
        }.filter { Objects.nonNull(it) }

        println("5")

        val allClasses = types.filter { it != null && it.isClass }
            .map { JavaClassSource::class.java.cast(it) }
            .peek { nestedTypes.addAll(it.nestedTypes) }

        println("6")

        val result = allClasses.filter {
            println("Message class: " + it.canonicalName)
            isMessageClass(it)
        }

        println("7")

        return result
    }

    private fun parseSource(code: Text): JavaSource<*> {
        return parsedSources[code]
    }

    private fun isMessageClass(cls: JavaClassSource): Boolean {

        println("Super type: " + cls.superType)

        val result = GeneratedMessageV3::class.java.canonicalName == cls.superType

        println("Result: $result")

        return result
    }

    private fun findBuilder(cls: JavaClassSource): Optional<JavaClassSource> {

        println("findBuilder: " + cls.canonicalName)

        val builder = cls.getNestedType("Builder")

        println("after nestedClass!!!!")

        if (builder == null || !builder.isClass) {
            return Optional.empty()
        }
        val builderClass = builder as JavaClassSource

        println("Builder class: " + builderClass.canonicalName)

        return Optional.of(builderClass)
    }
}
