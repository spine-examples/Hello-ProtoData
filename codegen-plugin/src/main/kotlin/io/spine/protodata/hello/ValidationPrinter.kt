package io.spine.protodata.hello

import io.spine.protodata.renderer.InsertionPoint
import io.spine.protodata.renderer.InsertionPointPrinter
import io.spine.tools.code.Java

public class ValidationPrinter: InsertionPointPrinter<Java>(Java.lang()) {

    override fun supportedInsertionPoints(): Set<InsertionPoint> {
        return setOf(BuildBeforeReturnInsertionPoint())
    }
}
