package io.spine.protodata.hello

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import io.spine.text.Text
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.JavaSource


/**
 * Parses the source code via `Roaster` and caches the results for further use.
 */
internal class ParsedSources {

    private object CacheParams {
        const val size: Long = 300
    }

    /**
     * Cached results of parsing the Java source code.
     */
    private val cache = CacheBuilder.newBuilder()
        .maximumSize(CacheParams.size)
        .build(CacheLoaderExt())

    /**
     * Parses the Java code and returns it as the parsed `JavaSource`,
     * caching it for future use.
     *
     * If the code was parsed previously, most likely the cached result
     * is returned right away, as the cache stores 300 items max.
     */
    operator fun get(code: Text): JavaSource<*> {
        return cache.getUnchecked(code)
    }

    class CacheLoaderExt : CacheLoader<Text, JavaSource<*>>() {
        override fun load(key: Text): JavaSource<*> {
            val result = Roaster.parse(JavaSource::class.java, key.value)
            if (result.isClass) {
                return CachingJavaClassSource(result as JavaClassSource)
            }
            return result
        }
    }
}
