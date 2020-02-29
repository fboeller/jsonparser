package io.fboeller

import io.fboeller.ParserDSLUnsafe.Companion.field
import io.fboeller.ParserDSLUnsafe.Companion.fieldsOf
import io.fboeller.ParserDSLUnsafe.Companion.maybe
import io.fboeller.ParserDSLUnsafe.Companion.string
import io.fboeller.ParserDSLUnsafe.Companion.listOf
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ParserDSLUnsafeTest {

    data class Person(val name: String, val hobbies: List<String>)

    @Test
    fun `JSON structure is parsed correctly`() {
        val myJson: Json = JsonObj(
            mapOf(
                "name" to JsonPrimitive("b"),
                "hobbies" to JsonList(listOf(JsonPrimitive("d")))
            )
        )
        val personOf: Parser<Person?> = ParserDSLUnsafe.obj(
            fieldsOf(::Person)(
                field("name", maybe(string)),
                field("hobbies", maybe(listOf(string)))
            )
        )
        Assertions.assertThat(personOf(myJson))
            .isEqualTo(Person("b", listOf("d")))
    }

}