package io.spine.protodata.hello

import com.google.common.collect.ArrayListMultimap

public class JavaSourceData {

    private val data = ArrayListMultimap.create<String, String>()

    public fun addMethod(javaFile: String, validationMethod: String) {

        data.put(javaFile, validationMethod)
    }

    public fun contains(javaSourceFile: String): Boolean {
        return data.containsKey(javaSourceFile)
    }
}
