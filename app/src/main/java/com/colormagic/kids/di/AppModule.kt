package com.colormagic.kids.di

import android.content.Context
import androidx.room.Room
import com.colormagic.kids.data.local.AppDatabase
import com.colormagic.kids.data.local.dao.PlaceholderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()

    @Provides
    @Singleton
    fun providePlaceholderDao(db: AppDatabase): PlaceholderDao = db.placeholderDao()

    // Repository and DAO providers will be added here as features are defined.
}
