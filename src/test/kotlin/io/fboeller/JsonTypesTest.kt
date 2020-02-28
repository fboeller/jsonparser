package io.fboeller

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JsonTypesTest {

    data class Person(val name: String, val hobbies: List<String>)

    @Test
    fun `JSON structure is parsed correctly`() {
        val myJson: Json = JsonObject(
            mapOf(
                "name" to JsonPrimitive("b"),
                "hobbies" to JsonList(listOf(JsonPrimitive("d")))
            )
        )
        val personOf: Parser<Person?> = obj(
            fieldsOf(::Person)(
                field("name", maybe(string)),
                field("hobbies", maybe(listOf(string)))
            )
        )
        Assertions.assertThat(personOf(myJson))
            .isEqualTo(Person("b", listOf("d")))
    }

}