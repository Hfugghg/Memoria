package com.exp.memoria.di

import android.util.Log
import com.exp.memoria.data.remote.api.LlmApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.ConnectionSpec
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
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
    fun provideOkHttpClient(): OkHttpClient {
        // 关键修复：添加现代化的 TLS 配置以解决 SSLHandshakeException
        val spec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2)
            .build()

        // 临时调试：添加一个安全的拦截器，只打印响应头，不影响流
        val headerLoggingInterceptor = Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            // 只在流式请求时打印日志以减少干扰
            if (request.url.toString().contains("streamGenerateContent")) {
                Log.d("OkHttpStreamDebug", "Response for ${request.url}: Code=${response.code}")
                Log.d("OkHttpStreamDebug", "Headers:\n${response.headers}")
            }
            response
        }

        return OkHttpClient.Builder()
            .addInterceptor(headerLoggingInterceptor) // 添加自定义拦截器
            .connectionSpecs(listOf(spec, ConnectionSpec.CLEARTEXT))
            .readTimeout(3, TimeUnit.MINUTES)
            .connectTimeout(3, TimeUnit.MINUTES)
            .writeTimeout(3, TimeUnit.MINUTES)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }
        return Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideLlmApiService(retrofit: Retrofit): LlmApiService {
        return retrofit.create(LlmApiService::class.java)
    }
}