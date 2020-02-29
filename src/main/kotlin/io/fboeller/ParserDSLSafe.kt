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

        fun <T> fail(reason: String): Parser<Result<T>> =
            { Failure(listOf(Message(reason))) }

        fun <T> list(parse: LParser<Result<T>>): Parser<Result<T>> =
            fold(fail("is not a list but an object"), parse, fail("is not a list but a string"))

        fun <T> obj(parse: OParser<Result<T>>): Parser<Result<T>> =
            fold(parse, fail("is not an object but a list"), fail("is not an object but a string"))

        fun <T> string(parse: PParser<Result<T>>): Parser<Result<T>> =
            fold(fail("is not a string but an object"), fail("is not a string but a list"), parse)

        fun <T> Parser<Result<T>>.field(field: String): OParser<Result<T?>> = { json ->
            sequence(json.fields[field]?.let(this))(field)
        }


        val string: Parser<Result<String>> =
            string { Success(it.value) }

        fun <T> listOf(parse: Parser<Result<T>>): Parser<Result<List<T>>> =
            list { list -> sequence(list.elements.map(parse)) }


        fun <T1, T2, R> liftOption(f: (T1, T2) -> R)
                : (T1?, T2?) -> R? =
            { t1, t2 ->
                t1?.let { s1 -> t2?.let { s2 -> f(s1, s2) } }
            }

        fun <T1, T2, T3, R> liftOption(f: (T1, T2, T3) -> R)
                : (T1?, T2?, T3?) -> R? =
            { t1, t2, t3 ->
                t1?.let { s1 -> t2?.let { s2 -> t3?.let { s3 -> f(s1, s2, s3) } } }
            }

        fun <T1, T2, R> liftResult(f: (T1, T2) -> R)
                : (Result<T1>, Result<T2>) -> Result<R> =
            { f1, f2 ->
                f1.flatMap { t1 -> f2.map { t2 -> f(t1, t2) } }
            }

        fun <T1, T2, T3, R> liftResult(f: (T1, T2, T3) -> R)
                : (Result<T1>, Result<T2>, Result<T3>) -> Result<R> =
            { f1, f2, f3 ->
                f1.flatMap { t1 -> f2.flatMap { t2 -> f3.map { t3 -> f(t1, t2, t3) } } }
            }

        fun <T1, T2, R> liftParser(f: (T1, T2) -> R)
                : (Parser<T1>, Parser<T2>) -> Parser<R> =
            { p1, p2 ->
                { json -> f(p1(json), p2(json)) }
            }

        fun <T1, T2, T3, R> liftParser(f: (T1, T2, T3) -> R)
                : (Parser<T1>, Parser<T2>, Parser<T3>) -> Parser<R> =
            { p1, p2, p3 ->
                { json -> f(p1(json), p2(json), p3(json)) }
            }

        fun <T1, T2, R> liftOParser(f: (T1, T2) -> R)
                : (OParser<T1>, OParser<T2>) -> OParser<R> =
            { p1, p2 ->
                { json -> f(p1(json), p2(json)) }
            }

        fun <T1, T2, T3, R> liftOParser(f: (T1, T2, T3) -> R)
                : (OParser<T1>, OParser<T2>, OParser<T3>) -> OParser<R> =
            { p1, p2, p3 ->
                { json -> f(p1(json), p2(json), p3(json)) }
            }

        fun <T1, T2, R> fieldsOf(f: (T1, T2) -> R)
                : (OParser<Result<T1>>, OParser<Result<T2>>) -> OParser<Result<R>> =
            liftOParser(liftResult(f))

        fun <T1, T2, T3, R> fieldsOf(f: (T1, T2, T3) -> R)
                : (OParser<Result<T1>>, OParser<Result<T2>>, OParser<Result<T3>>) -> OParser<Result<R>> =
            liftOParser(liftResult(f))

        fun <T> OParser<Result<T?>>.mandatory(): OParser<Result<T>> = { json ->
            this(json).flatMap<T?, T> { t ->
                t?.let { Success(it) }
                    ?: Failure(listOf(Message("is mandatory but does not exist")))
            }
        }

    }

}