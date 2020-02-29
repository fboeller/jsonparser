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

    data class Person(
        val name: String,
        val surname: String?,
        val hobbies: List<String>
    )

    val personOf: Parser<Result<Person>> = obj(
        fieldsOf(::Person)(
            string.field("name").mandatory(),
            string.field("surname"),
            listOf(string).field("hobbies").mandatory()
        )
    )

    @Test
    fun `JSON structure with all fields is parsed correctly`() {
        val myJson: Json = JsonObj(
            mapOf(
                "name" to JsonPrimitive("b"),
                "surname" to JsonPrimitive("c"),
                "hobbies" to JsonList(listOf(JsonPrimitive("d")))
            )
        )
        Assertions.assertThat(personOf(myJson))
            .isEqualTo(Success(Person("b", "c", listOf("d"))))
    }

    @Test
    fun `JSON structure with null field is parsed correctly`() {
        val myJson: Json = JsonObj(
            mapOf(
                "name" to JsonPrimitive("b"),
                "hobbies" to JsonList(listOf(JsonPrimitive("d")))
            )
        )
        Assertions.assertThat(personOf(myJson))
            .isEqualTo(Success(Person("b", null, listOf("d"))))
    }

    @Test
    fun `Missing hobbies field is detected`() {
        val myJson: Json = JsonObj(
            mapOf(
                "name" to JsonPrimitive("b")
            )
        )
        Assertions.assertThat(personOf(myJson))
            .isEqualTo(Failure<Person>(listOf(Message("is mandatory but does not exist"))))
    }

    @Test
    fun `Wrong hobbies field type is detected`() {
        val myJson: Json = JsonObj(
            mapOf(
                "name" to JsonPrimitive("b"),
                "hobbies" to JsonPrimitive("d")
            )
        )
        Assertions.assertThat(personOf(myJson).reasons())
            .isEqualTo(listOf(FieldReason("hobbies", Message("is not a list but a string"))))
    }

    @Test
    fun `Hobbies field contains element of wrong type`() {
        val myJson: Json = JsonObj(
            mapOf(
                "name" to JsonPrimitive("b"),
                "hobbies" to JsonList(listOf(JsonPrimitive("d"), JsonList(listOf(JsonPrimitive("e")))))
            )
        )
        Assertions.assertThat(personOf(myJson).reasons())
            .isEqualTo(listOf(FieldReason("hobbies", IndexReason(1, Message("is not a string but a list")))))
    }

}