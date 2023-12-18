package spine.protodata.hello

import io.spine.protodata.hello.Address
import io.spine.protodata.hello.Contact
import io.spine.protodata.hello.validateAddressCount
import io.spine.protodata.hello.validateEmailCount
import io.spine.protodata.hello.validatePhoneCount
import org.junit.jupiter.api.Test

/**
 * Checks various use-cases on `size` option validation.
 */
class `Size option test should` {

    @Test
    fun `check validation of several fields within one message type`() {
        val elementCount = 3

        val builder = Contact.newBuilder()
            .setElementCount(elementCount)

        repeat(elementCount) {
            builder
                .addPhone("Phone$it")
                .addAddress(
                    Address.newBuilder()
                        .setLine1("Line1$it")
                        .setLine2("Line2$it")
                        .setZipcode("Zipcode$it")
                        .setCountry("Country$it")
                )
                .addEmail("Email$it")
        }

        builder
            .validatePhoneCount()
            .validateAddressCount()
            .validateEmailCount()
    }
}
