package com.exp.memoria.di

import com.exp.memoria.data.remote.api.LlmApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * [网络依赖注入模块]
 *
 * 职责:
 * 1. 专门为应用提供所有与网络请求相关的依赖，特别是Retrofit实例和ApiService接口。
 * 2. 使用 @Module 和 @InstallIn(SingletonComponent::class) 声明这是一个Hilt模块，其提供的实例将在应用生命周期内保持单例。
 * 3. 提供配置好的Retrofit单例，包括Base URL、JSON转换器(kotlinx.serialization)等。
 * 4. 基于配置好的Retrofit实例，提供 LlmApiService 的具体实现。
 *
 * 关联:
 * - 这个模块提供的 LlmApiService 将被注入到 LlmRepository 中，用于执行实际的API调用。
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        val json = Json { ignoreUnknownKeys = true }
        return Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideLlmApiService(retrofit: Retrofit): LlmApiService {
        return retrofit.create(LlmApiService::class.java)
    }
}