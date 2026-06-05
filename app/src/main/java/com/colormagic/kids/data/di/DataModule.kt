package com.colormagic.kids.data.di

import com.colormagic.kids.data.repository.AuthRepositoryImpl
import com.colormagic.kids.data.repository.BillingRepositoryImpl
import com.colormagic.kids.data.repository.CreditRepositoryImpl
import com.colormagic.kids.data.repository.FeedbackRepositoryImpl
import com.colormagic.kids.data.repository.GalleryRepositoryImpl
import com.colormagic.kids.data.repository.SketchRepositoryImpl
import com.colormagic.kids.domain.repository.AuthRepository
import com.colormagic.kids.domain.repository.BillingRepository
import com.colormagic.kids.domain.repository.CreditRepository
import com.colormagic.kids.domain.repository.FeedbackRepository
import com.colormagic.kids.domain.repository.GalleryRepository
import com.colormagic.kids.domain.repository.SketchRepository
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

    @Binds
    @Singleton
    abstract fun bindSketchRepository(impl: SketchRepositoryImpl): SketchRepository

    @Binds
    @Singleton
    abstract fun bindBillingRepository(impl: BillingRepositoryImpl): BillingRepository

    @Binds
    @Singleton
    abstract fun bindGalleryRepository(impl: GalleryRepositoryImpl): GalleryRepository

    @Binds
    @Singleton
    abstract fun bindCreditRepository(impl: CreditRepositoryImpl): CreditRepository

    @Binds
    @Singleton
    abstract fun bindFeedbackRepository(impl: FeedbackRepositoryImpl): FeedbackRepository
}
