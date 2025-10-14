package com.exp.memoria.di

import com.exp.memoria.data.local.dao.RawMemoryDao
import com.exp.memoria.data.repository.MemoryRepository
import com.exp.memoria.data.repository.MemoryRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideMemoryRepository(rawMemoryDao: RawMemoryDao): MemoryRepository {
        return MemoryRepositoryImpl(rawMemoryDao)
    }
}