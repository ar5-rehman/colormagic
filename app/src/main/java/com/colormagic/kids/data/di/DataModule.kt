package com.colormagic.kids.data.di

import com.colormagic.kids.data.repository.AuthRepositoryImpl
import com.colormagic.kids.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Binds repository interfaces (domain) to their implementations (data).
// Add new bindings here as repositories grow.
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
