package spine.protodata.hello

import io.spine.protodata.hello.Address
import io.spine.protodata.hello.Contact
import io.spine.protodata.hello.validateAddressCount
import io.spine.protodata.hello.validateEmailCount
import io.spine.protodata.hello.validatePhoneCount
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Checks various use-cases on `size` option validation.
 */
class `Size option test should` {

    @Nested
    class ` validate several options within the same message` {

        @Test
        fun `with the generated methods`() {
            buildContact()
                .validateAddressCount()
                .validateEmailCount()
                .validatePhoneCount()
        }

        @Test
        fun `with the build method`() {
            buildContact().build()
        }
    }

    @Nested
    class ` check that a validation error is raised` {

        @Test
        fun `by the generated methods`() {
            val builder = buildInvalidContact()

            assertThrows<IllegalStateException> {
                builder.validatePhoneCount()
            }
            assertThrows<IllegalStateException> {
                builder.validateAddressCount()
            }
            assertThrows<IllegalStateException> {
                builder.validateEmailCount()
            }
        }

        @Test
        fun `by the build method`() {
            assertThrows<IllegalStateException> {
                buildInvalidContact().build()
            }
        }
    }
}

private fun buildInvalidContact(): Contact.Builder {
    return Contact.newBuilder()
        .setElementCount(2)
        .addPhone("Phone")
        .addEmail("Email")
        .addAddress(buildAddress(1))
}

private fun buildContact(): Contact.Builder {
    val elementCount = 3

    val builder = Contact.newBuilder()
        .setElementCount(elementCount)

    repeat(elementCount) {
        builder
            .addPhone("Phone$it")
            .addEmail("Email$it")
            .addAddress(
                buildAddress(it)
            )
    }
    return builder
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
