package com.exp.memoria.di

import com.exp.memoria.data.local.dao.CondensedMemoryDao
import com.exp.memoria.data.local.dao.RawMemoryDao
import com.exp.memoria.data.remote.api.LlmApiService
import com.exp.memoria.data.repository.LlmRepository
import com.exp.memoria.data.repository.MemoryRepository
import com.exp.memoria.data.repository.MemoryRepositoryImpl
import com.exp.memoria.domain.usecase.GetChatResponseUseCase
import com.exp.memoria.domain.usecase.ProcessMemoryUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * [应用级依赖注入模块]
 *
 * 职责:
 * 1. 使用Hilt为整个应用提供单例(Singleton)级别的依赖实例。
 * 2. 主要负责提供数据仓库(Repository)的实例，例如 MemoryRepository 和 LlmRepository。
 * 3. 提供Retrofit实例、OkHttpClient实例以及其他需要全局复用的网络相关对象 [cite: 80]。
 * 4. 提供Use Case的实例，例如 GetChatResponseUseCase 和 ProcessMemoryUseCase。
 *
 * 关联:
 * - @Module 和 @InstallIn(SingletonComponent::class) 注解告诉Hilt这是一个应用生命周期内的模块。
 * - @Provides 注解的方法是Hilt创建实例的"配方"。例如，一个方法会接收 LlmApiService 和 MemoryDao 作为参数，并返回一个 LlmRepository 的实例。
 */

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideMemoryRepository(rawMemoryDao: RawMemoryDao, condensedMemoryDao: CondensedMemoryDao): MemoryRepository {
        return MemoryRepositoryImpl(rawMemoryDao, condensedMemoryDao)
    }

    @Provides
    @Singleton
    fun provideLlmRepository(llmApiService: LlmApiService): LlmRepository {
        //  请在这里提供您的API密钥
        return LlmRepository(llmApiService, "apikey")
    }

    @Provides
    fun provideGetChatResponseUseCase(memoryRepository: MemoryRepository, llmRepository: LlmRepository): GetChatResponseUseCase {
        return GetChatResponseUseCase(memoryRepository, llmRepository)
    }

    @Provides
    fun provideProcessMemoryUseCase(memoryRepository: MemoryRepository, llmRepository: LlmRepository): ProcessMemoryUseCase {
        return ProcessMemoryUseCase(memoryRepository, llmRepository)
    }
}