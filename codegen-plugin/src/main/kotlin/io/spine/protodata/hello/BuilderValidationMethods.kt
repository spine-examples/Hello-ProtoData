package io.spine.protodata.hello

import com.google.common.collect.ArrayListMultimap
import java.nio.file.Path

/**
 * A container to gather the validation methods that should be invoked
 * in the `build` method of the message builder class.
 */
public class BuilderValidationMethods {

    private val methodMap = ArrayListMultimap.create<Path, String>()

    /**
     * Associates a builder validation method with the Java source file.
     *
     * @param javaSourceFilePath
     *          a [Path] to the generated java file.
     * @param validationMethod
     *          a string that represents a call of the validation method.
     */
    public fun linkMethod(javaSourceFilePath: Path, validationMethod: String) {
        methodMap.put(javaSourceFilePath, validationMethod)
    }

    /**
     * Checks that some builder validation methods are associated
     * with the given Java source file.
     *
     * @param javaSourceFilePath
     *          a [Path] to the generated java file.
     */
    public fun hasMethods(javaSourceFilePath: Path): Boolean {
        return methodMap.containsKey(javaSourceFilePath)
    }

    /**
     * Returns the list of validation methods that are associated
     * with the given Java source file.
     *
     * @param javaSourceFilePath
     *          a [Path] to the generated java file.
     */
    public fun methods(javaSourceFilePath: Path): Iterable<String> {
        return methodMap.get(javaSourceFilePath)
    }
}
