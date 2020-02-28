package io.fboeller

sealed class Fallible<T>
data class Success<T>(val t: T) : Fallible<T>()
data class Failure<T>(val reasons: List<String>): Fallible<T>()

fun <T, R> Fallible<T>.flatMap(f: (T) -> Fallible<R>): Fallible<R> = when (this) {
    is Success -> f(this.t)
    is Failure -> Failure(this.reasons)
}

fun <T, R> Fallible<T>.map(f: (T) -> R): Fallible<R> =
    flatMap { Success(f(it)) }

fun <T> merge(fallible1: Fallible<List<T>>, fallible2: Fallible<List<T>>): Fallible<List<T>> = when(fallible1) {
    is Success -> when(fallible2) {
        is Success -> Success(fallible1.t.union(fallible2.t).toList())
        is Failure -> Failure(fallible2.reasons)
    }
    is Failure -> when(fallible2) {
        is Success -> Failure(fallible1.reasons)
        is Failure -> Failure(fallible1.reasons.union(fallible2.reasons).toList())
    }
}

fun <T> sequence(fallibles: List<Fallible<T>>): Fallible<List<T>> =
    fallibles.fold(Success(listOf()), { acc, fallible -> merge(acc, fallible.map { listOf(it) }) })

fun <T> sequenceO(fallible: Fallible<T?>): Fallible<T>? = when(fallible) {
    is Success -> fallible.t?.let { Success(it) }
    is Failure -> Failure(fallible.reasons)
}

fun <T> sequence(maybe: Fallible<T>?): Fallible<T?> = when(maybe) {
    null -> Success(null)
    else -> maybe.map { it }
}
