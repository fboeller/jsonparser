package io.fboeller

// obj.list[3].name
sealed class Reason

data class Message(val message: String) : Reason()
data class FieldReason(val field: String, val reason: Reason) : Reason()
data class IndexReason(val index: Int, val reason: Reason) : Reason()

fun Reason.print(): String = when (this) {
    is Message -> " $message"
    is FieldReason -> "." + field + reason.print()
    is IndexReason -> "[" + index + "]" + reason.print()
}

val a = FieldReason("obj", FieldReason("list", IndexReason(3, FieldReason("name", Message(" can not be null")))))

sealed class Result<T>
data class Success<T>(val t: T) : Result<T>()
data class Failure<T>(val reasons: List<Reason>) : Result<T>()

fun <T> Result<T>.reasons() = when(this) {
    is Success -> emptyList()
    is Failure -> reasons
}

fun <T, R> Result<T>.flatMap(f: (T) -> Result<R>): Result<R> = when (this) {
    is Success -> f(t)
    is Failure -> Failure(reasons)
}

fun <T, R> Result<T>.map(f: (T) -> R): Result<R> =
    flatMap { Success(f(it)) }

fun <T> Result<T>.mapFailures(f: (Reason) -> Reason): Result<T> = when (this) {
    is Success -> this
    is Failure -> Failure(reasons.map(f))
}

fun <T> merge(result1: Result<List<T>>, result2: Result<List<T>>): Result<List<T>> = when (result1) {
    is Success -> when (result2) {
        is Success -> Success(result1.t.union(result2.t).toList())
        is Failure -> Failure(result2.reasons)
    }
    is Failure -> when (result2) {
        is Success -> Failure(result1.reasons)
        is Failure -> Failure(result1.reasons.union(result2.reasons).toList())
    }
}

fun <T> sequence(results: List<Result<T>>): Result<List<T>> =
    results.foldIndexed(
        Success(listOf()),
        { i, acc, result ->
            merge(
                acc,
                result.mapFailures { IndexReason(i, it) }.map { listOf(it) }
            )
        }
    )

fun <T> sequenceO(result: Result<T?>): Result<T>? = when (result) {
    is Success -> result.t?.let { Success(it) }
    is Failure -> Failure(result.reasons)
}

fun <T> sequence(maybe: Result<T>?): (String) -> Result<T?> = { name ->
    when (maybe) {
        null -> Success(null)
        else -> maybe.mapFailures { FieldReason(name, it) }.map { it }
    }
}