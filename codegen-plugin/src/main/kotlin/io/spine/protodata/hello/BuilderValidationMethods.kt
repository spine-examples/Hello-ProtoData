package io.spine.protodata.hello

import com.google.common.collect.ArrayListMultimap

/**
 * A container to gather the validation methods that should be invoked
 * in the `build` method of a message builder class.
 */
public class BuilderValidationMethods {

    private val methodMap = ArrayListMultimap.create<String, String>()

    /**
     * Associates a builder validation method with a Java source file.
     */
    public fun linkMethod(javaSourceFile: String, validationMethod: String) {
        methodMap.put(javaSourceFile, validationMethod)
    }

    /**
     * Checks that some builder validation methods are associated
     * with the given Java source file.
     */
    public fun hasMethods(javaSourceFile: String): Boolean {
        return methodMap.containsKey(javaSourceFile)
    }

    /**
     * Returns the list of builder validation methods that associated
     * with the given Java source file.
     */
    public fun methods(javaSourceFile: String): Iterable<String> {
        return methodMap.get(javaSourceFile)
    }
}
