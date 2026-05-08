package com.colormagic.kids.domain.usecase.base

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class UseCase<in P, R>(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend operator fun invoke(params: P): Result<R> = withContext(dispatcher) {
        runCatching { execute(params) }
    }

    protected abstract suspend fun execute(params: P): R
}

// Convenience alias for use cases that require no input parameters.
abstract class NoParamUseCase<R>(
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : UseCase<Unit, R>(dispatcher) {
    suspend operator fun invoke(): Result<R> = invoke(Unit)
}
