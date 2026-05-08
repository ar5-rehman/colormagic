package com.colormagic.kids.domain.usecase.base

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

abstract class FlowUseCase<in P, R>(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    operator fun invoke(params: P): Flow<Result<R>> = execute(params)
        .map { Result.success(it) }
        .catch { emit(Result.failure(it)) }
        .flowOn(dispatcher)

    protected abstract fun execute(params: P): Flow<R>
}

// Convenience alias for flow use cases that require no input parameters.
abstract class NoParamFlowUseCase<R>(
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : FlowUseCase<Unit, R>(dispatcher) {
    operator fun invoke(): Flow<Result<R>> = invoke(Unit)
}
