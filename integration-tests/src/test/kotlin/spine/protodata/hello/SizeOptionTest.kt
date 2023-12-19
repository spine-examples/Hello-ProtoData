package spine.protodata.hello

import io.spine.protodata.hello.Address
import io.spine.protodata.hello.Contact
import io.spine.protodata.hello.validateAddressCount
import io.spine.protodata.hello.validateAddressLineCount
import io.spine.protodata.hello.validateEmailCount
import io.spine.protodata.hello.validatePhoneCount
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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

    @Test
    fun `fail if size of a repeated field is wrong`() {
        val addressBuilder = Address.newBuilder()
            .setZipcode("Zipcode")
            .setCountry("Country")
            .setNumberOfLines(2)
            .addAddressLine("AddressLine")

        val contactBuilder = Contact.newBuilder()
            .setElementCount(2)
            .addPhone("Phone")
            .addEmail("Email")
            .addAddress(addressBuilder)

        assertThrows<IllegalStateException> {
            addressBuilder.validateAddressLineCount()
        }
        assertThrows<IllegalStateException> {
            contactBuilder.validatePhoneCount()
        }
        assertThrows<IllegalStateException> {
            contactBuilder.validateAddressCount()
        }
        assertThrows<IllegalStateException> {
            contactBuilder.validateEmailCount()
        }
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
