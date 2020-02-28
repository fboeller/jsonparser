package io.fboeller

import io.fboeller.ParserDSLSafe.Companion.field
import io.fboeller.ParserDSLSafe.Companion.fieldsOf
import io.fboeller.ParserDSLSafe.Companion.string
import io.fboeller.ParserDSLSafe.Companion.listOf
import io.fboeller.ParserDSLSafe.Companion.mandatory
import io.fboeller.ParserDSLSafe.Companion.obj
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ParserDSLSafeTest {

    data class Person(val name: String, val hobbies: List<String>)

    val personOf: FallibleParser<Person> = obj(
        fieldsOf(::Person)(
            field("name", mandatory(string)),
            field("hobbies", mandatory(listOf(string)))
        )
    )

    @Test
    fun `JSON structure is parsed correctly`() {
        val myJson: Json = JsonObject(
            mapOf(
                "name" to JsonPrimitive("b"),
                "hobbies" to JsonList(listOf(JsonPrimitive("d")))
            )
        )
        Assertions.assertThat(personOf(myJson))
            .isEqualTo(Success(Person("b", listOf("d"))))
    }

    @Test
    fun `Missing hobbies field is detected`() {
        val myJson: Json = JsonObject(
            mapOf(
                "name" to JsonPrimitive("b")
            )
        )
        Assertions.assertThat(personOf(myJson))
            .isEqualTo(Failure<Person>(listOf("Expected mandatory field but found nothing!")))
    }

    @Test
    fun `Wrong hobbies field type is detected`() {
        val myJson: Json = JsonObject(
            mapOf(
                "name" to JsonPrimitive("b"),
                "hobbies" to JsonPrimitive("d")
            )
        )
        Assertions.assertThat(personOf(myJson))
            .isEqualTo(Failure<Person>(listOf("Expected list but found string!")))
    }

}