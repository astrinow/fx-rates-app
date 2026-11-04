package com.fxrates.core.utils

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** The result of an asynchronous operation as the presentation layer sees it. */
sealed class Resource<out T> {

    data object Loading : Resource<Nothing>()

    data class Data<T>(val value: T) : Resource<T>()

    data class Error(val throwable: Throwable) : Resource<Nothing>()

}

fun <T> Resource<T>.valueOrNull(): T? = (this as? Resource.Data<T>)?.value

/**
 * Runs [block] and emits [Resource.Loading] followed by its result. A thrown exception becomes
 * [Resource.Error] instead of cancelling the collecting scope; cancellation is rethrown.
 */
fun <T> resourceFlow(block: suspend () -> T): Flow<Resource<T>> = flow {
    emit(Resource.Loading)
    val result = try {
        Resource.Data(block.invoke())
    } catch (exception: CancellationException) {
        throw exception
    } catch (@Suppress("TooGenericExceptionCaught") exception: Exception) {
        Resource.Error(exception)
    }
    emit(result)
}
