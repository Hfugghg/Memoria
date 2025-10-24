package com.exp.memoria.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.exp.memoria.data.local.dao.CondensedMemoryDao
import com.exp.memoria.data.local.dao.ConversationHeaderDao
import com.exp.memoria.data.local.dao.MessageFileDao
import com.exp.memoria.data.local.dao.RawMemoryDao
import com.exp.memoria.data.remote.api.LlmApiService
import com.exp.memoria.data.repository.*
import com.exp.memoria.data.repository.llmrepository.*
import com.exp.memoria.domain.usecase.GetChatResponseUseCase
import com.exp.memoria.domain.usecase.ProcessMemoryUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

    // 1. 提供 LlmRepositoryHelpers 实例 (如果它被 @Inject 构造函数注解，则此步可选，
    //    但为了清晰和保证单例，建议保留)
    @Provides
    @Singleton
    fun provideLlmRepositoryHelpers(settingsRepository: SettingsRepository): LlmRepositoryHelpers {
        return LlmRepositoryHelpers(settingsRepository)
    }

    // 2. 提供 ChatService 实例 (委托给 ChatServiceImpl)
    @Provides
    @Singleton
    fun provideChatService(
        llmApiService: LlmApiService,
        helpers: LlmRepositoryHelpers // 依赖于 Helpers
    ): ChatService {
        return ChatServiceImpl(llmApiService, helpers)
    }

    // 3. 提供 UtilityService 实例 (委托给 UtilityServiceImpl)
    @Provides
    @Singleton
    fun provideUtilityService(
        llmApiService: LlmApiService,
        helpers: LlmRepositoryHelpers // 依赖于 Helpers
    ): UtilityService {
        return UtilityServiceImpl(llmApiService, helpers)
    }

    // 4. 修改后的 LlmRepository 提供函数
    @Provides
    @Singleton
    fun provideLlmRepository(
        chatService: ChatService, // 注入 ChatService
        utilityService: UtilityService // 注入 UtilityService
    ): LlmRepository {
        // LlmRepository 的构造函数现在接受 ChatService 和 UtilityService
        return LlmRepository(chatService, utilityService)
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("settings")
        }
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(dataStore: DataStore<Preferences>): SettingsRepository {
        return SettingsRepositoryImpl(dataStore)
    }

    @Provides
    @Singleton
    fun provideFileAttachmentRepository(messageFileDao: MessageFileDao): FileAttachmentRepository {
        return FileAttachmentRepositoryImpl(messageFileDao)
    }

    @Provides
    @Singleton
    fun provideMessageRepository(rawMemoryDao: RawMemoryDao, condensedMemoryDao: CondensedMemoryDao, fileAttachmentRepository: FileAttachmentRepository): MessageRepository {
        return MessageRepositoryImpl(rawMemoryDao, condensedMemoryDao, fileAttachmentRepository)
    }

    @Provides
    @Singleton
    fun provideConversationRepository(conversationHeaderDao: ConversationHeaderDao, rawMemoryDao: RawMemoryDao): ConversationRepository {
        return ConversationRepositoryImpl(conversationHeaderDao, rawMemoryDao)
    }

    @Provides
    @Singleton
    fun provideMemoryRepository(
        fileAttachmentRepository: FileAttachmentRepository,
        messageRepository: MessageRepository,
        conversationRepository: ConversationRepository
    ): MemoryRepository {
        return MemoryRepositoryImpl(fileAttachmentRepository, messageRepository, conversationRepository)
    }

    @Provides
    fun provideGetChatResponseUseCase(
        memoryRepository: MemoryRepository,
        llmRepository: LlmRepository
    ): GetChatResponseUseCase {
        return GetChatResponseUseCase(memoryRepository, llmRepository)
    }

    @Provides
    fun provideProcessMemoryUseCase(
        memoryRepository: MemoryRepository,
        llmRepository: LlmRepository
    ): ProcessMemoryUseCase {
        return ProcessMemoryUseCase(memoryRepository, llmRepository)
    }
}
