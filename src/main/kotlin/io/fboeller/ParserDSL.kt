package io.fboeller

typealias Parser<T> = (Json) -> Result<T>
typealias OParser<T> = (JsonObj) -> Result<T>
typealias LParser<T> = (JsonList) -> Result<T>
typealias PParser<T> = (JsonPrimitive) -> Result<T>

private fun <T> fold(fo: OParser<T>, fl: LParser<T>, fp: PParser<T>): Parser<T> = { json ->
    when (json) {
        is JsonObj -> fo(json)
        is JsonList -> fl(json)
        is JsonPrimitive -> fp(json)
    }
}

private fun <T> fail(reason: String): Parser<T> =
    { Failure(listOf(Message(reason))) }

private fun <T> onlylist(parse: LParser<T>): Parser<T> =
    fold(fail("is not a list but an object"), parse, fail("is not a list but a string"))

private fun <T> obj(parse: OParser<T>): Parser<T> =
    fold(parse, fail("is not an object but a list"), fail("is not an object but a string"))

private fun <T> string(parse: PParser<T>): Parser<T> =
    fold(fail("is not a string but an object"), fail("is not a string but a list"), parse)


fun string(): Parser<String> =
    string { Success(it.value) }

fun <T> Parser<T>.list(): Parser<List<T>> = onlylist { list ->
    sequence(
        list.elements
            .map(this)
            .mapIndexed { i, result -> result.mapFailures { IndexReason(i, it) } }
    )
}

data class Fields2<T1, T2>(val p1: OParser<T1>, val p2: OParser<T2>) {
    fun <R> mapTo(f: ((T1, T2) -> R)): Parser<R> =
        obj(liftOParser(f)(p1, p2))
}

fun <T1, T2> fields(p1: OParser<T1>, p2: OParser<T2>): Fields2<T1, T2> =
    Fields2(p1, p2)

fun <T> Parser<T>.field(name: String): OParser<T?> = { json ->
    sequence(json.fields[name]?.let(this))
        .mapFailures { FieldReason(name, it) }
}

fun <T> OParser<T?>.mandatory(): OParser<T> = { json ->
    this(json).flatMap<T?, T> { t ->
        t?.let { Success(it) }
            ?: Failure(listOf(Message("is mandatory but does not exist")))
    }
}


private fun <T1, T2, R> liftResult(f: (T1, T2) -> R): (Result<T1>, Result<T2>) -> Result<R> =
    { result1, result2 ->
        when (result1) {
            is Success -> when (result2) {
                is Success -> Success(f(result1.t, result2.t))
                is Failure -> Failure(result2.reasons)
            }
            is Failure -> when (result2) {
                is Success -> Failure(result1.reasons)
                is Failure -> Failure(result1.reasons.union(result2.reasons).toList())
            }
        }
    }

private fun <T1, T2, R> liftOParser(f: (T1, T2) -> R): (OParser<T1>, OParser<T2>) -> OParser<R> =
    { p1, p2 ->
        { json -> liftResult(f)(p1(json), p2(json)) }
    }