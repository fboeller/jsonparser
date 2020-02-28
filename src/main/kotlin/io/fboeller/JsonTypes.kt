package io.fboeller

import java.lang.RuntimeException

sealed class Json
data class JsonObject(val fields: Map<String, Json>) : Json()
data class JsonList(val elements: List<Json>) : Json()
data class JsonPrimitive(val value: String) : Json()

typealias Parser<T> = (Json) -> T
typealias OParser<T> = (JsonObject) -> T

fun <T> fold(fo: (JsonObject) -> T, fl: (JsonList) -> T, fp: (JsonPrimitive) -> T): Parser<T> = { json ->
    when (json) {
        is JsonObject -> fo(json)
        is JsonList -> fl(json)
        is JsonPrimitive -> fp(json)
    }
}

fun <T> expectList(parse: (JsonList) -> T): Parser<T> =
    fold({ fail() }, parse, { fail() })

fun <T> expectObject(parse: (JsonObject) -> T): Parser<T> =
    fold(parse, { fail() }, { fail() })

fun <T> expectString(parse: (JsonPrimitive) -> T): Parser<T> =
    fold({ fail() }, { fail() }, parse)

fun <T> expectField(field: String, parse: (Json?) -> T): OParser<T> = { json ->
    parse(json.fields[field])
}

fun <T1, T2, R> lift(f: (T1, T2) -> R): (T1?, T2?) -> R? = { t1, t2 ->
    t1?.let { s1 -> t2?.let { s2 -> f(s1, s2) } }
}

val myJson: Json = JsonObject(
    mapOf(
        "name" to JsonPrimitive("b"),
        "hobbies" to JsonList(listOf(JsonPrimitive("d")))
    )
)

data class Person(val name: String, val hobbies: List<String>)

fun fail(): Nothing = throw RuntimeException()

fun parseJson(json: Json): Person? =
    expectObject(::parsePerson)(json)

fun parsePerson(o: JsonObject): Person? =
    lift(::Person)(
        expectField("name", ::parseName)(o),
        expectField("hobbies", ::parseHobbies)(o)
    )

fun parseName(maybeJson: Json?): String? =
    maybeJson?.let(expectString(JsonPrimitive::value))

fun parseHobbies(maybeJson: Json?): List<String>? =
    maybeJson?.let(expectList { list -> list.elements.map(expectString(JsonPrimitive::value)) })

fun main() {
    println(parseJson(myJson))
}