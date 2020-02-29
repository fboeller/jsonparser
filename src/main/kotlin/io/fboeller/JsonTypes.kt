package io.fboeller

sealed class Json
data class JsonObj(val fields: Map<String, Json>) : Json()
data class JsonList(val elements: List<Json>) : Json()
data class JsonPrimitive(val value: String) : Json()

typealias Parser<T> = (Json) -> T
typealias OParser<T> = (JsonObj) -> T
typealias LParser<T> = (JsonList) -> T
typealias PParser<T> = (JsonPrimitive) -> T