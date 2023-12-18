package spine.protodata.hello

import io.spine.protodata.hello.Address
import io.spine.protodata.hello.Contact
import io.spine.protodata.hello.validateAddressCount
import io.spine.protodata.hello.validateAddressLineCount
import io.spine.protodata.hello.validateEmailCount
import io.spine.protodata.hello.validatePhoneCount
import org.junit.jupiter.api.Test

/**
 * Checks various use-cases on `size` option validation.
 */
class `Size option test should` {

    @Test
    fun `check several options within the same message type and sub-type`() {
        val elementCount = 3

        val builder = Contact.newBuilder()
            .setElementCount(elementCount)

        repeat(elementCount) {
            builder
                .addPhone("Phone$it")
                .addEmail("Email$it")
                .addAddress(
                    buildAddress(it)
                        .validateAddressLineCount()
                )
        }

        builder
            .validatePhoneCount()
            .validateAddressCount()
            .validateEmailCount()
    }
}

private fun buildAddress(seed: Int): Address.Builder {
    val numberOfLines = 2

    val builder = Address.newBuilder()
        .setNumberOfLines(numberOfLines)
        .setZipcode("Zipcode$seed")
        .setCountry("Country$seed")

    repeat(numberOfLines) {
        builder.addAddressLine("AddressLine$seed$it")
    }

    return builder
}
