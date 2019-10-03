@file:Suppress("unused")

package by.shostko.rxvalidation

import io.reactivex.Completable

abstract class Validator<T> : Validation.Delegate<T>() {

    @Throws(Throwable::class)
    abstract fun validate(value: T)

    final override val validator: Validator<T>
        get() = this

    internal fun validateAsCompletable(value: T): Completable = Completable.fromAction { validate(value) }

    abstract class Predicate<T>(private val message: String? = null) : Validator<T>() {
        final override fun validate(value: T) {
            if (!isValid(value)) {
                throw ValidationException(message)
            }
        }

        protected abstract fun isValid(value: T): Boolean
    }

    class SimpleAction<T>(private val function: (T) -> Unit) : Validator<T>() {
        override fun validate(value: T) = function(value)
    }

    class SimplePredicate<T>(private val message: String?, private val function: (T) -> Boolean) : Predicate<T>() {
        constructor(function: (T) -> Boolean) : this(null, function)

        override fun isValid(value: T): Boolean = function(value)
    }
}

private class CompositeValidator<T>(private vararg val validators: Validator<in T>) : Validator<T>() {
    override fun validate(value: T) = validators.forEach { it.validate(value) }
}

private class IterableValidator<T>(private val validators: Iterable<Validator<in T>>) : Validator<T>() {
    override fun validate(value: T) = validators.forEach { it.validate(value) }
}

fun <T> validators(vararg validators: Validator<in T>): Validator<T> = CompositeValidator(*validators)

fun <T> validators(vararg validators: (T) -> Boolean): Validator<T> = IterableValidator(validators.map { Validator.SimplePredicate(it) })

fun <T> validators(validators: Iterable<Validator<in T>>): Validator<T> = IterableValidator(validators)