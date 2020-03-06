package io.fboeller

data class Reason(val path: List<PathElement>, val message: String) {

    fun print(): String =
        "<root>" + path.joinToString { it.print() } + " " + message

    fun at(pathElement: PathElement): Reason =
        at(listOf(pathElement))

    fun at(pathElements: List<PathElement>): Reason =
        Reason(pathElements.plus(path), message)

}

fun reason(message: String) = Reason(emptyList(), message)

sealed class PathElement {

    fun print(): String = when (this) {
        is Field -> ".$field"
        is Index -> "[$index]"
    }

}

data class Field(val field: String) : PathElement()
data class Index(val index: Int) : PathElement()

sealed class Result<out T> {

    fun reasons() = when (this) {
        is Success -> emptyList()
        is Failure -> reasons
    }

    fun <R> flatMap(f: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> f(t).at(path)
        is Failure -> this
    }

    fun filter(p: (T) -> Boolean, message: String?): Result<T?> = when (this) {
        is Success -> when (p(t)) {
            true -> this
            false -> Failure(listOf(reason(message ?: "does not meet the criteria").at(path)))
        }
        is Failure -> this
    }

    fun <R> map(f: (T) -> R): Result<R> = when (this) {
        is Success -> Success(path, f(t))
        is Failure -> this
    }

    fun at(path: List<PathElement>): Result<T> = when (this) {
        is Success -> Success(path, t)
        is Failure -> Failure(reasons.map { it.at(path) })
    }

    fun at(pathElement: PathElement): Result<T> = at(listOf(pathElement))

}

data class Success<T>(val path: List<PathElement>, val t: T) : Result<T>()
data class Failure(val reasons: List<Reason>) : Result<Nothing>()

fun failure(message: String) =
    Failure(listOf(reason(message)))

fun <T> merge(result1: Result<Sequence<T>>, result2: Result<Sequence<T>>): Result<Sequence<T>> = when (result1) {
    is Success -> when (result2) {
        is Success -> Success(emptyList(), result1.t.plus(result2.t))
        is Failure -> Failure(result2.reasons)
    }
    is Failure -> when (result2) {
        is Success -> Failure(result1.reasons)
        is Failure -> Failure(result1.reasons.plus(result2.reasons))
    }
}

fun <T> Sequence<Result<T>>.sequence(): Result<Sequence<T>> =
    this.fold(
        Success(emptyList(), emptySequence()),
        { acc, result -> merge(acc, result.map { sequenceOf(it) }) }
    )

fun <T> Result<T>?.sequence(): Result<T?> = when (this) {
    null -> Success(emptyList(), null)
    else -> map { it }
}