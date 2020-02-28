package io.fboeller

import java.lang.RuntimeException

class ParserDSLUnsafe {

    companion object {

        fun <T> fold(fo: OParser<T>, fl: LParser<T>, fp: PParser<T>): Parser<T> = { json ->
            when (json) {
                is JsonObject -> fo(json)
                is JsonList -> fl(json)
                is JsonPrimitive -> fp(json)
            }
        }

        fun <T> list(parse: LParser<T>): Parser<T> =
            fold({ fail() }, parse, { fail() })

        fun <T> obj(parse: OParser<T>): Parser<T> =
            fold(parse, { fail() }, { fail() })

        fun <T> string(parse: PParser<T>): Parser<T> =
            fold({ fail() }, { fail() }, parse)

        fun <T> field(field: String, parse: (Json?) -> T): OParser<T> = { json ->
            parse(json.fields[field])
        }


        val string: Parser<String> = string(JsonPrimitive::value)

        fun <T> listOf(parse: Parser<T>): Parser<List<T>> =
            list { list -> list.elements.map(parse) }


        fun <T1, T2, R> liftOption(f: (T1, T2) -> R): (T1?, T2?) -> R? = { t1, t2 ->
            t1?.let { s1 -> t2?.let { s2 -> f(s1, s2) } }
        }

        fun <T1, T2, R> liftParser(f: (T1, T2) -> R): (Parser<T1>, Parser<T2>) -> Parser<R> = { p1, p2 ->
            { json -> f(p1(json), p2(json)) }
        }

        fun <T1, T2, R> liftOParser(f: (T1, T2) -> R): (OParser<T1>, OParser<T2>) -> OParser<R> = { p1, p2 ->
            { json -> f(p1(json), p2(json)) }
        }

        fun <T1, T2, R> fieldsOf(f: (T1, T2) -> R): (OParser<T1?>, OParser<T2?>) -> OParser<R?> =
            liftOParser(liftOption(f))

        fun <T, R> maybe(f: (T) -> R): (T?) -> R? =
            { it?.let(f) }

        fun fail(): Nothing = throw RuntimeException()

    }

}