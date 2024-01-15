package io.spine.protodata.hello

import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.JavaSource
import org.jboss.forge.roaster.model.source.MethodSource

/**
 * A `JavaClassSource` which caches some of its parsed components.
 *
 * Please note that, because of the caching, sources updated in an underlying
 * file may not be updated in this model.
 */
internal class CachingJavaClassSource(
    private val delegate: JavaClassSource
) : JavaClassSource by delegate {

    /**
     * Cached value of [JavaClassSource.getNestedTypes()][JavaClassSource.getNestedTypes].
     */
    private val cachedNestedTypes: List<JavaSource<*>> by lazy {
        delegate.nestedTypes
    }

    override fun getNestedTypes(): List<JavaSource<*>> {
        return cachedNestedTypes
    }

    // Specifying the desired implementation for `getMethod(..)` overloads explicitly because of
    // a clash between the supertype and the delegate. For more info, remove the explicit overrides
    // and see the warning.

    override fun getMethod(
        name: String?,
        vararg paramTypes: Class<*>?
    ): MethodSource<JavaClassSource> {
        return delegate.getMethod(name, *paramTypes)
    }

    override fun getMethod(
        name: String?,
        vararg paramTypes: String?
    ): MethodSource<JavaClassSource> {
        return delegate.getMethod(name, *paramTypes)
    }
}
