package spine.protodata.hello

import io.spine.protodata.hello.Contact
import io.spine.protodata.hello.validateAddressCount
import io.spine.protodata.hello.validateEmailCount
import io.spine.protodata.hello.validatePhoneCount
import org.junit.jupiter.api.Test

class `Size option test should` {

    @Test
    fun `check validation of several fields within one message type`() {
        val elementCount = 5
        val builder = Contact.newBuilder()
            .setCount(elementCount)

        repeat(elementCount) {
            builder.addPhone("Phone$it")
            builder.addAddress("Address$it")
            builder.addEmail("Email$it")
        }

        builder.validatePhoneCount()
        builder.validateAddressCount()
        builder.validateEmailCount()
    }
}
