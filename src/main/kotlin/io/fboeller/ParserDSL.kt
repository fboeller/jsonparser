package io.fboeller

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.core.io.SerializedString

data class Parser<T>(val parse: (JsonParser) -> Result<T>?) {
    fun list(): Parser<Sequence<T>> = LParser { p ->
        generateSequence(0, { it + 1 })
            .map { parse(p) }
            .takeWhile { result -> result != null }
            .mapIndexed { i, result -> result!!.at(Index(i)) }
            .sequence()
    }.list()

    fun field(name: String): OParser<T?> = OParser { p ->
        when (p.nextFieldName(SerializedString(name))) {
            true -> parse(p)
                .sequence()
            false -> Success(emptyList(), null)
        }.at(Field(name))
    }

    fun filter(predicate: (T) -> Boolean, message: String?): Parser<T?> = Parser { p ->
        parse(p)?.filter(predicate, message)
    }
}

data class OParser<T>(val parse: (JsonParser) -> Result<T>?) {
    fun obj(): Parser<T> = Parser { p ->
        when (p.nextToken()) {
            JsonToken.START_OBJECT -> parse(p)
            JsonToken.START_ARRAY -> {
                p.skipChildren()
                failure("is not an object but a list")
            }
            JsonToken.VALUE_STRING -> failure("is not an object but a string")
            JsonToken.END_ARRAY -> null
            JsonToken.END_OBJECT -> null
            else -> failure("is not currently parsable by this version")
        }
    }
}

data class LParser<T>(val parse: (JsonParser) -> Result<T>?) {
    fun list(): Parser<T> = Parser { p ->
        when (p.nextToken()) {
            JsonToken.START_OBJECT -> {
                p.skipChildren()
                failure("is not a list but an object")
            }
            JsonToken.START_ARRAY -> parse(p)
            JsonToken.VALUE_STRING -> failure("is not a list but a string")
            JsonToken.END_ARRAY -> null
            JsonToken.END_OBJECT -> null
            else -> failure("is not currently parsable by this version")
        }
    }
}

data class PParser<T>(val parse: (JsonParser) -> Result<T>?) {
    fun string(): Parser<T> = Parser { p ->
        when (p.nextToken()) {
            JsonToken.START_OBJECT -> {
                p.skipChildren()
                failure("is not a string but an object")
            }
            JsonToken.START_ARRAY -> {
                p.skipChildren()
                failure("is not a string but a list")
            }
            JsonToken.VALUE_STRING -> parse(p)
            JsonToken.END_ARRAY -> null
            JsonToken.END_OBJECT -> null
            else -> failure("is not currently parsable by this version")
        }
    }
}


fun string(): Parser<String> =
    PParser { Success(emptyList(), it.valueAsString) }.string()

data class Fields2<T1, T2>(val p1: OParser<T1>, val p2: OParser<T2>) {
    fun <R> mapTo(f: ((T1, T2) -> R)): Parser<R> =
        liftOParser(f)(p1, p2).obj()
}

fun <T1, T2> fields(p1: OParser<T1>, p2: OParser<T2>): Fields2<T1, T2> =
    Fields2(p1, p2)

fun <T> OParser<T?>.mandatory(): OParser<T> = OParser { json ->
    this.parse(json)?.flatMap { t ->
        t?.let { Success(emptyList(), it) }
            ?: failure("is mandatory but does not exist")
    }
}


private fun <T1, T2, R> liftResult(f: (T1, T2) -> R): (Result<T1>, Result<T2>) -> Result<R> =
    { result1, result2 ->
        when (result1) {
            is Success -> when (result2) {
                is Success -> Success(emptyList(), f(result1.t, result2.t))
                is Failure -> Failure(result2.reasons)
            }
            is Failure -> when (result2) {
                is Success -> Failure(result1.reasons)
                is Failure -> Failure(result1.reasons.union(result2.reasons).toList())
            }
        }
    }

private fun <T1, T2, R> liftOption(f: (T1, T2) -> R): (T1?, T2?) -> R? =
    { option1, option2 -> option1?.let { option2?.let { f(option1, option2) } } }

private fun <T1, T2, R> liftOParser(f: (T1, T2) -> R): (OParser<T1>, OParser<T2>) -> OParser<R> =
    { p1, p2 ->
        OParser { json -> liftOption(liftResult(f))(p1.parse(json), p2.parse(json)) }
    }