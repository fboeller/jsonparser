package io.fboeller

private fun <T> fold(
    fo: OParser<Result<T>>,
    fl: LParser<Result<T>>,
    fp: PParser<Result<T>>
): Parser<Result<T>> =
    { json ->
        when (json) {
            is JsonObj -> fo(json)
            is JsonList -> fl(json)
            is JsonPrimitive -> fp(json)
        }
    }

private fun <T> fail(reason: String): Parser<Result<T>> =
    { Failure(listOf(Message(reason))) }

private fun <T> onlylist(parse: LParser<Result<T>>): Parser<Result<T>> =
    fold(fail("is not a list but an object"), parse, fail("is not a list but a string"))

private fun <T> obj(parse: OParser<Result<T>>): Parser<Result<T>> =
    fold(parse, fail("is not an object but a list"), fail("is not an object but a string"))

private fun <T> string(parse: PParser<Result<T>>): Parser<Result<T>> =
    fold(fail("is not a string but an object"), fail("is not a string but a list"), parse)


fun string(): Parser<Result<String>> =
    string { Success(it.value) }

fun <T> Parser<Result<T>>.list(): Parser<Result<List<T>>> = onlylist { list ->
    sequence(
        list.elements
            .map(this)
            .mapIndexed { i, result -> result.mapFailures { IndexReason(i, it) } }
    )
}

data class Fields2<T1, T2>(val p1: OParser<Result<T1>>, val p2: OParser<Result<T2>>) {
    fun <R> mapTo(f: ((T1, T2) -> R)): Parser<Result<R>> =
        obj(liftOParser(liftResult(f))(p1, p2))
}

fun <T1, T2> fields(p1: OParser<Result<T1>>, p2: OParser<Result<T2>>): Fields2<T1, T2> =
    Fields2(p1, p2)

fun <T> Parser<Result<T>>.field(name: String): OParser<Result<T?>> = { json ->
    sequence(json.fields[name]?.let(this))
        .mapFailures { FieldReason(name, it) }
}

fun <T> OParser<Result<T?>>.mandatory(): OParser<Result<T>> = { json ->
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
        { json -> f(p1(json), p2(json)) }
    }