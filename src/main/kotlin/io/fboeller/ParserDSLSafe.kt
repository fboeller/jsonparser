package io.fboeller

class ParserDSLSafe {

    companion object {

        fun <T> fold(fo: OParser<Result<T>>, fl: LParser<Result<T>>, fp: PParser<Result<T>>): Parser<Result<T>> =
            { json ->
                when (json) {
                    is JsonObj -> fo(json)
                    is JsonList -> fl(json)
                    is JsonPrimitive -> fp(json)
                }
            }

        fun <T> fail(reason: String): (Json) -> Result<T> =
            { Failure(listOf(reason)) }

        fun <T> list(parse: LParser<Result<T>>): Parser<Result<T>> =
            fold(fail("Expected list but found object!"), parse, fail("Expected list but found string!"))

        fun <T> obj(parse: OParser<Result<T>>): Parser<Result<T>> =
            fold(parse, fail("Expected object but found list!"), fail("Expected object but found string!"))

        fun <T> string(parse: PParser<Result<T>>): Parser<Result<T>> =
            fold(fail("Expected string but found object!"), fail("Expected string but found list!"), parse)

        fun <T> Parser<Result<T>>.field(field: String): OParser<Result<T?>> = { json ->
            sequence(json.fields[field]?.let(this))
        }


        val string: Parser<Result<String>> =
            string { Success(it.value) }

        fun <T> listOf(parse: Parser<Result<T>>): Parser<Result<List<T>>> =
            list { list -> sequence(list.elements.map(parse)) }


        fun <T1, T2, R> liftOption(f: (T1, T2) -> R): (T1?, T2?) -> R? = { t1, t2 ->
            t1?.let { s1 -> t2?.let { s2 -> f(s1, s2) } }
        }

        fun <T1, T2, R> liftFallible(f: (T1, T2) -> R): (Result<T1>, Result<T2>) -> Result<R> = { f1, f2 ->
            f1.flatMap { t1 -> f2.map { t2 -> f(t1, t2) } }
        }

        fun <T1, T2, R> liftParser(f: (T1, T2) -> R): (Parser<T1>, Parser<T2>) -> Parser<R> = { p1, p2 ->
            { json -> f(p1(json), p2(json)) }
        }

        fun <T1, T2, R> liftOParser(f: (T1, T2) -> R): (OParser<T1>, OParser<T2>) -> OParser<R> = { p1, p2 ->
            { json -> f(p1(json), p2(json)) }
        }

        fun <T1, T2, R> fieldsOf(f: (T1, T2) -> R): (OParser<Result<T1>>, OParser<Result<T2>>) -> OParser<Result<R>> =
            liftOParser(liftFallible(f))

        fun <T> OParser<Result<T?>>.mandatory(): OParser<Result<T>> = { json ->
            this(json).flatMap<T?, T> { t ->
                t?.let { Success(it) } ?: Failure(listOf("Expected mandatory field but found nothing!"))
            }
        }

    }

}