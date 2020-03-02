package io.fboeller

sealed class Reason

data class Message(val message: String) : Reason()
data class FieldReason(val field: String, val reason: Reason) : Reason()
data class IndexReason(val index: Int, val reason: Reason) : Reason()

fun Reason.print(): String =
    "<root>" + this.print0()

private fun Reason.print0(): String = when (this) {
    is Message -> " $message"
    is FieldReason -> "." + field + reason.print0()
    is IndexReason -> "[" + index + "]" + reason.print0()
}

sealed class Result<out T> {

    fun reasons() = when (this) {
        is Success -> emptyList()
        is Failure -> reasons
    }

    fun <R> flatMap(f: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> f(t)
        is Failure -> this
    }

    fun <R> map(f: (T) -> R): Result<R> = when(this) {
        is Success -> Success(f(t))
        is Failure -> this
    }

    fun mapFailures(f: (Reason) -> Reason): Result<T> = when (this) {
        is Success -> this
        is Failure -> Failure(reasons.map(f))
    }

}

data class Success<T>(val t: T) : Result<T>()
data class Failure(val reasons: List<Reason>) : Result<Nothing>()

fun failure(reason: String) =
    Failure(listOf(Message(reason)))

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

fun <T> List<Result<T>>.sequence(): Result<List<T>> =
    this.fold(
        Success(listOf()),
        { acc, result -> merge(acc, result.map { listOf(it) }) }
    )

fun <T> Result<T>?.sequence(): Result<T?> = when (this) {
    null -> Success(null)
    else -> map { it }
}