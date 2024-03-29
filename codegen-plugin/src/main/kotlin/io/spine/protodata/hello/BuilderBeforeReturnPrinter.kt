package io.spine.protodata.hello

import io.spine.protodata.renderer.InsertionPoint
import io.spine.protodata.renderer.InsertionPointPrinter
import io.spine.tools.code.Java

/**
 * [InsertionPointPrinter] that prints the `size` option validation code
 * in the `build` method of the message class builder.
 */
public class BuilderBeforeReturnPrinter :
    InsertionPointPrinter<Java>(Java.lang()) {

    override fun supportedInsertionPoints(): Set<InsertionPoint> {
        return setOf(BuilderBeforeReturnInsertionPoint())
    }
}
