package io.fboeller

sealed class Fallible<T>
data class Success<T>(val t: T) : Fallible<T>()
data class Failure<T>(val reasons: List<String>): Fallible<T>()

fun <T, R> Fallible<T>.flatMap(f: (T) -> Fallible<R>): Fallible<R> = when (this) {
    is Success -> f(this.t)
    is Failure -> Failure(this.reasons)
}

fun <T, R> Fallible<T>.map(f: (T) -> R): Fallible<R> =
    flatMap { Success(f(it)) }

typealias FallibleParser<T> = (Json) -> Fallible<T>
typealias FallibleOParser<T> = (JsonObject) -> Fallible<T>
typealias FallibleLParser<T> = (JsonList) -> Fallible<T>
typealias FalliblePParser<T> = (JsonPrimitive) -> Fallible<T>

class ParserDSLSafe {

    companion object {

        fun <T> fold(fo: FallibleOParser<T>, fl: FallibleLParser<T>, fp: FalliblePParser<T>): FallibleParser<T> =
            { json ->
                when (json) {
                    is JsonObject -> fo(json)
                    is JsonList -> fl(json)
                    is JsonPrimitive -> fp(json)
                }
            }

        fun <T> fail(reason: String): FallibleParser<T> =
            { Failure(listOf(reason)) }

        fun <T> list(parse: FallibleLParser<T>): FallibleParser<T> =
            fold(fail("Expected list but found object!"), parse, fail("Expected list but found string!"))

        fun <T> obj(parse: FallibleOParser<T>): FallibleParser<T> =
            fold(parse, fail("Expected object but found list!"), fail("Expected object but found string!"))

        fun <T> string(parse: FalliblePParser<T>): FallibleParser<T> =
            fold(fail("Expected string but found object!"), fail("Expected string but found list!"), parse)

        fun <T> field(field: String, parse: (Json?) -> Fallible<T>): FallibleOParser<T> = { json ->
            parse(json.fields[field])
        }

        fun <T> merge(fallible1: Fallible<List<T>>, fallible2: Fallible<List<T>>): Fallible<List<T>> = when(fallible1) {
            is Success -> when(fallible2) {
                is Success -> Success(fallible1.t.union(fallible2.t).toList())
                is Failure -> Failure(fallible2.reasons)
            }
            is Failure -> when(fallible2) {
                is Success -> Failure(fallible1.reasons)
                is Failure -> Failure(fallible1.reasons.union(fallible2.reasons).toList())
            }
        }

        fun <T> sequence(fallibles: List<Fallible<T>>): Fallible<List<T>> =
            fallibles.fold(Success(listOf()), { acc, fallible -> merge(acc, fallible.map { listOf(it) }) })


        val string: FallibleParser<String> =
            string { Success(it.value) }

        fun <T> listOf(parse: FallibleParser<T>): FallibleParser<List<T>> =
            list { list -> sequence(list.elements.map(parse)) }


        fun <T1, T2, R> liftOption(f: (T1, T2) -> R): (T1?, T2?) -> R? = { t1, t2 ->
            t1?.let { s1 -> t2?.let { s2 -> f(s1, s2) } }
        }

        fun <T1, T2, R> liftFallible(f: (T1, T2) -> R): (Fallible<T1>, Fallible<T2>) -> Fallible<R> = { f1, f2 ->
            f1.flatMap { t1 -> f2.map { t2 -> f(t1, t2) } }
        }

        fun <T1, T2, R> liftParser(f: (T1, T2) -> R): (Parser<T1>, Parser<T2>) -> Parser<R> = { p1, p2 ->
            { json -> f(p1(json), p2(json)) }
        }

        fun <T1, T2, R> liftOParser(f: (T1, T2) -> R): (OParser<T1>, OParser<T2>) -> OParser<R> = { p1, p2 ->
            { json -> f(p1(json), p2(json)) }
        }

        fun <T1, T2, R> fieldsOf(f: (T1, T2) -> R): (FallibleOParser<T1>, FallibleOParser<T2>) -> FallibleOParser<R> =
            liftOParser(liftFallible(f))

        fun <T, R> mandatory(f: (T) -> Fallible<R>): (T?) -> Fallible<R> =
            { t -> t?.let(f) ?: Failure(listOf("Expected mandatory field but found nothing!")) }

    }

}