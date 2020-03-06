package io.fboeller

import com.fasterxml.jackson.core.JsonFactory

sealed class Json
data class JsonObj(val fields: Map<String, Json>) : Json()
data class JsonList(val elements: List<Json>) : Json()
data class JsonPrimitive(val value: String) : Json()

fun main() {
    val parser = JsonFactory().createParser("{}")
    val map = parser.nextToken()
    println(map)
}