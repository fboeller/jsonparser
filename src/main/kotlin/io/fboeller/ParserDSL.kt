package io.fboeller

class ParserDSL {

    companion object {

        private fun <T> fold(fo: OParser<Result<T>>, fl: LParser<Result<T>>, fp: PParser<Result<T>>): Parser<Result<T>> =
            { json ->
                when (json) {
                    is JsonObj -> fo(json)
                    is JsonList -> fl(json)
                    is JsonPrimitive -> fp(json)
                }
            }

        private fun <T> fail(reason: String): Parser<Result<T>> =
            { Failure(listOf(Message(reason))) }

        fun <T> list(parse: LParser<Result<T>>): Parser<Result<T>> =
            fold(fail("is not a list but an object"), parse, fail("is not a list but a string"))

        fun <T> obj(parse: OParser<Result<T>>): Parser<Result<T>> =
            fold(parse, fail("is not an object but a list"), fail("is not an object but a string"))

        fun <T> string(parse: PParser<Result<T>>): Parser<Result<T>> =
            fold(fail("is not a string but an object"), fail("is not a string but a list"), parse)


        val string: Parser<Result<String>> =
            string { Success(it.value) }

        fun <T> listOf(parse: Parser<Result<T>>): Parser<Result<List<T>>> =
            list { list -> sequence(list.elements.map(parse)) }

        fun <T> Parser<Result<T>>.field(name: String): OParser<Result<T?>> = { json ->
            sequence(json.fields[name]?.let(this))(name)
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

        private fun <T1, T2, R> liftResult(f: (T1, T2) -> R)
                : (Result<T1>, Result<T2>) -> Result<R> =
            { f1, f2 ->
                f1.flatMap { t1 -> f2.map { t2 -> f(t1, t2) } }
            }

        private fun <T1, T2, T3, R> liftResult(f: (T1, T2, T3) -> R)
                : (Result<T1>, Result<T2>, Result<T3>) -> Result<R> =
            { f1, f2, f3 ->
                f1.flatMap { t1 -> f2.flatMap { t2 -> f3.map { t3 -> f(t1, t2, t3) } } }
            }

        private fun <T1, T2, R> liftOParser(f: (T1, T2) -> R)
                : (OParser<T1>, OParser<T2>) -> OParser<R> =
            { p1, p2 ->
                { json -> f(p1(json), p2(json)) }
            }

        private fun <T1, T2, T3, R> liftOParser(f: (T1, T2, T3) -> R)
                : (OParser<T1>, OParser<T2>, OParser<T3>) -> OParser<R> =
            { p1, p2, p3 ->
                { json -> f(p1(json), p2(json), p3(json)) }
            }

    }

}