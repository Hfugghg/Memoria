package com.exp.memoria

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * [应用入口]
 *
 * 职责:
 * 1. 作为整个应用的入口点和全局上下文提供者。
 * 2. 初始化Hilt依赖注入框架。
 * 3. 修正：实现 Configuration.Provider 接口，并重写 workManagerConfiguration 属性。
 * 4. 注入 HiltWorkerFactory，并将其设置为 WorkManager 的默认 WorkerFactory。
 * 这样，所有被 @HiltWorker 注解的 Worker 在创建时，Hilt 都能自动为其注入所需的依赖。
 *
 * 关联:
 * - @HiltAndroidApp 注解是Hilt的起点。
 * - 必须在 AndroidManifest.xml 的 <application> 标签中注册。
 */

@HiltAndroidApp
class MemoriaApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}