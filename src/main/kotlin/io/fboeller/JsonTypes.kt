package io.fboeller

import java.lang.RuntimeException

sealed class Json
data class JsonObject(val fields: Map<String, Json>) : Json()
data class JsonList(val elements: List<Json>) : Json()
data class JsonPrimitive(val value: String) : Json()

fun <T> Json.fold(fo: (JsonObject) -> T, fl: (JsonList) -> T, fp: (JsonPrimitive) -> T): T = when (this) {
    is JsonObject -> fo(this)
    is JsonList -> fl(this)
    is JsonPrimitive -> fp(this)
}

fun <T> Json.expectList(parse: (JsonList) -> T): T =
    this.fold({ fail() }, parse, { fail() })

fun <T> Json.expectObject(parse: (JsonObject) -> T): T =
    this.fold(parse, { fail() }, { fail() })

fun <T> Json.expectString(parse: (JsonPrimitive) -> T): T =
    this.fold({ fail() }, { fail() }, parse)

fun <T1, T2, R> lift(f: (T1, T2) -> R): (T1?, T2?) -> R? = { t1, t2 ->
    t1?.let { s1 -> t2?.let { s2 -> f(s1, s2) } }
}


val myJson: Json = JsonObject(mapOf(
    "name" to JsonPrimitive("b"),
    "hobbies" to JsonList(listOf(JsonPrimitive("d")))
))

data class Person(val name: String, val hobbies: List<String>)

fun fail(): Nothing = throw RuntimeException()

fun parseJson(json: Json) =
    json.expectObject(::parsePerson)

fun parsePerson(o: JsonObject): Person? =
    lift(::Person)(
        parseName(o.fields["name"]),
        parseHobbies(o.fields["hobbies"])
    )

fun parseName(maybeJson: Json?): String? =
    maybeJson?.let {json -> json.expectString(JsonPrimitive::value) }

fun parseHobbies(maybeJson: Json?): List<String>? =
    maybeJson?.let { json -> json.expectList { list -> list.elements.map { json -> json.expectString(JsonPrimitive::value) } } }

fun main() {
    println(parseJson(myJson))
}