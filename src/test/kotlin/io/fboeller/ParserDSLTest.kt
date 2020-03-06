package io.fboeller

import com.fasterxml.jackson.core.JsonFactory
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe

class ParserDSLTest : StringSpec({

    "String parser succeeds" {
        forAll(
            row("\"str\"", "str")
        ) { json, result ->
            string().parse(JsonFactory().createParser(json)) shouldBe Success(emptyList(), result)
        }
    }

    "String parser fails" {
        forAll(
            row("[]", listOf("<root> is not a string but a list")),
            row("{}", listOf("<root> is not a string but an object"))
        ) { json, result ->
            string().parse(JsonFactory().createParser(json))?.reasons()?.map { it.print() } shouldBe result
        }
    }

    "List parser succeeds" {
        forAll(
            row("[]", emptyList()),
            row("[\"str\"]", listOf("str")),
            row("[\"str1\", \"str2\"]", listOf("str1", "str2"))
        ) { json, result ->
            string().list().parse(JsonFactory().createParser(json))?.map { it.toList() } shouldBe Success(emptyList(), result)
        }
    }

    "List parser fails" {
        forAll(
            row("\"str\"", listOf("<root> is not a list but a string")),
            row("[[]]", listOf("<root>[0] is not a string but a list")),
            row("[{}]", listOf("<root>[0] is not a string but an object")),
            row(
                "[[], \"str\", {}]",
                listOf("<root>[0] is not a string but a list", "<root>[2] is not a string but an object")
            )
        ) { json, result ->
            string().list().parse(JsonFactory().createParser(json))?.reasons()?.map { it.print() } shouldBe result
        }
    }

    data class Person(
        val firstName: String,
        val lastName: String?
    )

    val person = fields(
        string().field("firstName").mandatory(),
        string().field("lastName")
    ).mapTo(::Person)

    "Object parser succeeds" {
        forAll(
            row("{\"firstName\": \"Heinz\"}", Person("Heinz", null)),
            row("{\"firstName\": \"Heinz\", \"lastName\": \"Schmidt\"}", Person("Heinz", "Schmidt")),
            row("{\"firstName\": \"Heinz\", \"otherField\": \"Schmidt\"}", Person("Heinz", null))
        ) { json, result ->
            person.parse(JsonFactory().createParser(json)) shouldBe Success(emptyList(), result)
        }
    }

    "Object parser fails" {
        forAll(
            row("{\"lastName\": \"Schmidt\"}", listOf("<root>.firstName is mandatory but does not exist"))
        ) { json, result ->
            person.parse(JsonFactory().createParser(json))?.reasons()?.map { it.print() } shouldBe result
        }
    }

    "filter() succeeds" {
        forAll(
            row(string().filter({ true }, null)),
            row(string().filter({ true }, "does not match"))
        ) { parser ->
            parser.parse(JsonFactory().createParser("\"str\"")) shouldBe Success(emptyList(), "str")
        }
    }

    "filter() parser fails" {
        forAll(
            row(string().filter({ false }, null), listOf("<root> does not meet the criteria")),
            row(string().filter({ false }, "does not match"), listOf("<root> does not match"))
        ) { parser, result ->
            parser.parse(JsonFactory().createParser("\"str\""))?.reasons()?.map { it.print() } shouldBe result
        }
    }

})