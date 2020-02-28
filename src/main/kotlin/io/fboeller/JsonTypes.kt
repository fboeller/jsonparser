package io.fboeller

sealed class Json
data class JsonObject(val fields: Map<String, Json>) : Json()
data class JsonList(val elements: List<Json>) : Json()
data class JsonPrimitive(val value: String) : Json()

typealias Parser<T> = (Json) -> T
typealias OParser<T> = (JsonObject) -> T
typealias LParser<T> = (JsonList) -> T
typealias PParser<T> = (JsonPrimitive) -> T