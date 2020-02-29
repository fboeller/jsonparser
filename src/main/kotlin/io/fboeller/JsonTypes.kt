package io.fboeller

sealed class Json
data class JsonObj(val fields: Map<String, Json>) : Json()
data class JsonList(val elements: List<Json>) : Json()
data class JsonPrimitive(val value: String) : Json()

// obj.list[3].name
sealed class Path
object PathRoot : Path()
data class PathField(val field: String, val path: Path): Path()
data class PathIndex(val index: Int, val path: Path): Path()

fun Path.print(): String = when(this) {
    is PathRoot -> ""
    is PathField -> path.print() + "." + field
    is PathIndex -> path.print() + "[" + index + "]"
}

val a = PathField("name", PathIndex(3, PathField("list", PathField("obj", PathRoot))))

typealias Parser<T> = (Json) -> T
typealias OParser<T> = (JsonObj) -> T
typealias LParser<T> = (JsonList) -> T
typealias PParser<T> = (JsonPrimitive) -> T