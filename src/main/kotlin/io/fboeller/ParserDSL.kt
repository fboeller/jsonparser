package io.fboeller

data class Parser<T>(val parse: (Json) -> Result<T>) {
    fun list(): Parser<List<T>> = LParser { list ->
        list.elements
            .map(this.parse)
            .mapIndexed { i, result -> result.at(Index(i)) }
            .sequence()
    }.list()

    fun field(name: String): OParser<T?> = OParser { json ->
        json.fields[name]
            ?.let(parse)
            .sequence()
            .at(Field(name))
    }
}

data class OParser<T>(val parse: (JsonObj) -> Result<T>) {
    fun obj(): Parser<T> = Parser { json ->
        when (json) {
            is JsonObj -> parse(json)
            is JsonList -> failure("is not an object but a list")
            is JsonPrimitive -> failure("is not an object but a string")
        }
    }
}

data class LParser<T>(val parse: (JsonList) -> Result<T>) {
    fun list(): Parser<T> = Parser { json ->
        when (json) {
            is JsonObj -> failure("is not a list but an object")
            is JsonList -> parse(json)
            is JsonPrimitive -> failure("is not a list but a string")
        }
    }
}

data class PParser<T>(val parse: (JsonPrimitive) -> Result<T>) {
    fun string(): Parser<T> = Parser { json ->
        when (json) {
            is JsonObj -> failure("is not a string but an object")
            is JsonList -> failure("is not a string but a list")
            is JsonPrimitive -> parse(json)
        }
    }
}


fun string(): Parser<String> =
    PParser { Success(emptyList(), it.value) }.string()

data class Fields2<T1, T2>(val p1: OParser<T1>, val p2: OParser<T2>) {
    fun <R> mapTo(f: ((T1, T2) -> R)): Parser<R> =
        liftOParser(f)(p1, p2).obj()
}

fun <T1, T2> fields(p1: OParser<T1>, p2: OParser<T2>): Fields2<T1, T2> =
    Fields2(p1, p2)

fun <T> OParser<T?>.mandatory(): OParser<T> = OParser { json ->
    this.parse(json)
        .flatMap { t ->
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

private fun <T1, T2, R> liftOParser(f: (T1, T2) -> R): (OParser<T1>, OParser<T2>) -> OParser<R> =
    { p1, p2 ->
        OParser { json -> liftResult(f)(p1.parse(json), p2.parse(json)) }
    }