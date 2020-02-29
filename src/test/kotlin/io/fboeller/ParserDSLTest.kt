package io.fboeller

import io.fboeller.ParserDSL.Companion.fieldsOf
import io.fboeller.ParserDSL.Companion.string
import io.fboeller.ParserDSL.Companion.list
import io.fboeller.ParserDSL.Companion.field
import io.fboeller.ParserDSL.Companion.mandatory
import io.fboeller.ParserDSL.Companion.obj
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe

class ParserDSLTest : StringSpec({

    "String parser succeeds" {
        forAll(
            row(JsonPrimitive("str"), "str")
        ) { json, result ->
            string(json) shouldBe Success(result)
        }
    }

    "String parser fails" {
        forAll(
            row(JsonList(emptyList()), listOf("<root> is not a string but a list")),
            row(JsonObj(emptyMap()), listOf("<root> is not a string but an object"))
        ) { json, result ->
            string(json).reasons().map { it.print() } shouldBe result
        }
    }

    "List parser succeeds" {
        forAll(
            row(JsonList(emptyList()), emptyList()),
            row(JsonList(listOf(JsonPrimitive("str"))), listOf("str")),
            row(JsonList(listOf(JsonPrimitive("str1"), JsonPrimitive("str2"))), listOf("str1", "str2"))
        ) { json, result ->
            string.list()(json) shouldBe Success(result)
        }
    }

    "List parser fails" {
        forAll(
            row(JsonPrimitive("str"), listOf("<root> is not a list but a string")),
            row(JsonList(listOf(JsonList(emptyList()))), listOf("<root>[0] is not a string but a list")),
            row(JsonList(listOf(JsonObj(emptyMap()))), listOf("<root>[0] is not a string but an object")),
            row(
                JsonList(listOf(JsonList(emptyList()), JsonPrimitive("str"), JsonObj(emptyMap()))),
                listOf("<root>[0] is not a string but a list", "<root>[2] is not a string but an object")
            )
        ) { json, result ->
            string.list()(json).reasons().map { it.print() } shouldBe result
        }
    }

    data class Person(
        val firstName: String,
        val lastName: String?
    )

    val personOf = obj(
        fieldsOf(::Person)(
            string.field("firstName").mandatory(),
            string.field("lastName")
        )
    )

    "Object parser succeeds" {
        forAll(
            row(JsonObj(mapOf("firstName" to JsonPrimitive("Heinz"))), Person("Heinz", null)),
            row(
                JsonObj(mapOf("firstName" to JsonPrimitive("Heinz"), "lastName" to JsonPrimitive("Schmidt"))),
                Person("Heinz", "Schmidt")
            ),
            row(
                JsonObj(mapOf("firstName" to JsonPrimitive("Heinz"), "otherField" to JsonPrimitive("Schmidt"))),
                Person("Heinz", null)
            )
        ) { json, result ->
            personOf(json) shouldBe Success(result)
        }
    }

    "Object parser fails" {
        forAll(
            row(
                JsonObj(mapOf("lastName" to JsonPrimitive("Schmidt"))),
                listOf("<root>.firstName is mandatory but does not exist")
            )
        ) { json, result ->
            personOf(json).reasons().map { it.print() } shouldBe result
        }
    }

})