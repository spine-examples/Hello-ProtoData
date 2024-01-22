package spine.protodata.hello

import io.spine.protodata.hello.Address
import io.spine.protodata.hello.Contact
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
                    buildAddress(it).build()
                )
        }

        builder.build()
    }

    @Test
    fun `fail if size of a repeated field is wrong`() {
        val addressBuilder = Address.newBuilder()
            .setZipcode("Zipcode")
            .setCountry("Country")
            .setNumberOfLines(2)
            .addAddressLine("AddressLine")

        assertThrows<IllegalStateException> {
            addressBuilder.build()
        }

        val contactBuilder = Contact.newBuilder()
            .setElementCount(2)
            .addPhone("Phone")
            .addEmail("Email")

        assertThrows<IllegalStateException> {
            contactBuilder.addAddress(addressBuilder)
        }
        assertThrows<IllegalStateException> {
            contactBuilder.build()
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
