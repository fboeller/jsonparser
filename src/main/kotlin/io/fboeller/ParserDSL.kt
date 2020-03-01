package io.fboeller

data class Parser<T>(val parse: (Json) -> Result<T>) {
    fun list(): Parser<List<T>> = onlylist(LParser { list ->
        list.elements
            .map(this.parse)
            .mapIndexed { i, result -> result.mapFailures { IndexReason(i, it) } }
            .sequence()
    })

    fun field(name: String): OParser<T?> = OParser { json ->
        json.fields[name]
            ?.let(this.parse)
            .sequence()
            .mapFailures { FieldReason(name, it) }
    }
}

data class OParser<T>(val parse: (JsonObj) -> Result<T>)
data class LParser<T>(val parse: (JsonList) -> Result<T>)
data class PParser<T>(val parse: (JsonPrimitive) -> Result<T>)

private fun <T> fold(fo: OParser<T>, fl: LParser<T>, fp: PParser<T>): Parser<T> = Parser { json ->
    when (json) {
        is JsonObj -> fo.parse(json)
        is JsonList -> fl.parse(json)
        is JsonPrimitive -> fp.parse(json)
    }
}

private fun <T> failO(reason: String): OParser<T> =
    OParser { Failure<T>(listOf(Message(reason))) }

private fun <T> failL(reason: String): LParser<T> =
    LParser { Failure<T>(listOf(Message(reason))) }

private fun <T> failP(reason: String): PParser<T> =
    PParser { Failure<T>(listOf(Message(reason))) }

private fun <T> onlylist(parse: LParser<T>): Parser<T> =
    fold(failO("is not a list but an object"), parse, failP("is not a list but a string"))

private fun <T> obj(parse: OParser<T>): Parser<T> =
    fold(parse, failL("is not an object but a list"), failP("is not an object but a string"))

private fun <T> string(parse: PParser<T>): Parser<T> =
    fold(failO("is not a string but an object"), failL("is not a string but a list"), parse)


fun string(): Parser<String> =
    string (PParser { Success(it.value) })

data class Fields2<T1, T2>(val p1: OParser<T1>, val p2: OParser<T2>) {
    fun <R> mapTo(f: ((T1, T2) -> R)): Parser<R> =
        obj(liftOParser(f)(p1, p2))
}

fun <T1, T2> fields(p1: OParser<T1>, p2: OParser<T2>): Fields2<T1, T2> =
    Fields2(p1, p2)

fun <T> OParser<T?>.mandatory(): OParser<T> = OParser { json ->
    this.parse(json).flatMap { t ->
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
        OParser { json -> liftResult(f)(p1.parse(json), p2.parse(json)) }
    }