package io.fboeller

data class Parser<T>(val parse: (Json) -> Result<T>) {
    fun list(): Parser<List<T>> = onlylist { list ->
        sequence(
            list.elements
                .map(this.parse)
                .mapIndexed { i, result -> result.mapFailures { IndexReason(i, it) } }
        )
    }
    fun field(name: String): OParser<T?> = { json ->
        sequence(json.fields[name]?.let(this.parse))
            .mapFailures { FieldReason(name, it) }
    }
}

typealias OParser<T> = (JsonObj) -> Result<T>
typealias LParser<T> = (JsonList) -> Result<T>
typealias PParser<T> = (JsonPrimitive) -> Result<T>

private fun <T> fold(fo: OParser<T>, fl: LParser<T>, fp: PParser<T>): Parser<T> = Parser { json ->
    when (json) {
        is JsonObj -> fo(json)
        is JsonList -> fl(json)
        is JsonPrimitive -> fp(json)
    }
}

private fun <T> failO(reason: String): OParser<T> =
    { Failure(listOf(Message(reason))) }

private fun <T> failL(reason: String): LParser<T> =
    { Failure(listOf(Message(reason))) }

private fun <T> failP(reason: String): PParser<T> =
    { Failure(listOf(Message(reason))) }

private fun <T> onlylist(parse: LParser<T>): Parser<T> =
    fold(failO("is not a list but an object"), parse, failP("is not a list but a string"))

private fun <T> obj(parse: OParser<T>): Parser<T> =
    fold(parse, failL("is not an object but a list"), failP("is not an object but a string"))

private fun <T> string(parse: PParser<T>): Parser<T> =
    fold(failO("is not a string but an object"), failL("is not a string but a list"), parse)


fun string(): Parser<String> =
    string { Success(it.value) }

data class Fields2<T1, T2>(val p1: OParser<T1>, val p2: OParser<T2>) {
    fun <R> mapTo(f: ((T1, T2) -> R)): Parser<R> =
        obj(liftOParser(f)(p1, p2))
}

fun <T1, T2> fields(p1: OParser<T1>, p2: OParser<T2>): Fields2<T1, T2> =
    Fields2(p1, p2)

fun <T> OParser<T?>.mandatory(): OParser<T> = { json ->
    this(json).flatMap { t ->
        t?.let { Success(it) }
            ?: Failure<T>(listOf(Message("is mandatory but does not exist")))
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