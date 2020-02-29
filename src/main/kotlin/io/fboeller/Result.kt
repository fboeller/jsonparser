package io.fboeller

sealed class Result<T>
data class Success<T>(val t: T) : Result<T>()
data class Failure<T>(val reasons: List<String>): Result<T>()

fun <T, R> Result<T>.flatMap(f: (T) -> Result<R>): Result<R> = when (this) {
    is Success -> f(this.t)
    is Failure -> Failure(this.reasons)
}

fun <T, R> Result<T>.map(f: (T) -> R): Result<R> =
    flatMap { Success(f(it)) }

fun <T> merge(result1: Result<List<T>>, result2: Result<List<T>>): Result<List<T>> = when(result1) {
    is Success -> when(result2) {
        is Success -> Success(result1.t.union(result2.t).toList())
        is Failure -> Failure(result2.reasons)
    }
    is Failure -> when(result2) {
        is Success -> Failure(result1.reasons)
        is Failure -> Failure(result1.reasons.union(result2.reasons).toList())
    }
}

fun <T> sequence(results: List<Result<T>>): Result<List<T>> =
    results.fold(Success(listOf()), { acc, fallible -> merge(acc, fallible.map { listOf(it) }) })

fun <T> sequenceO(result: Result<T?>): Result<T>? = when(result) {
    is Success -> result.t?.let { Success(it) }
    is Failure -> Failure(result.reasons)
}

fun <T> sequence(maybe: Result<T>?): Result<T?> = when(maybe) {
    null -> Success(null)
    else -> maybe.map { it }
}
