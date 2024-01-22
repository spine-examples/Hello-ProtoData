package io.spine.protodata.hello

import com.google.common.collect.ArrayListMultimap

public class JavaValidationMethods {

    private val methodMap = ArrayListMultimap.create<String, String>()

    public fun addMethod(javaSourceFile: String, validationMethod: String) {

        methodMap.put(javaSourceFile, validationMethod)
    }

    public fun contains(javaSourceFile: String): Boolean {
        return methodMap.containsKey(javaSourceFile)
    }

    public fun getMethods(javaSourceFile: String): List<String> {
        return methodMap.get(javaSourceFile)
    }
}
