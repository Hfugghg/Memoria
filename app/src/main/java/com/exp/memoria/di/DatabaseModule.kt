package com.exp.memoria.di

import android.content.Context
import androidx.room.Room
import com.exp.memoria.data.local.MemoriaDatabase
import com.exp.memoria.data.local.dao.RawMemoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * [数据库依赖注入模块]
 *
 * 职责:
 * 1. 专门负责提供所有与本地数据库(Room)相关的依赖 [cite: 51, 81]。
 * 2. 提供 MemoriaDatabase 的单例实例，确保整个应用只使用一个数据库连接。
 * 3. 提供各个DAO（Data Access Object）的实例，如 RawMemoryDao 和 CondensedMemoryDao。Hilt会从数据库实例中获取这些DAO。
 *
 * 关联:
 * - @Module 和 @InstallIn(SingletonComponent::class) 确保数据库实例是全局单例。
 * - 此模块提供的DAO实例将被注入到 MemoryRepository 中，用于执行实际的数据库读写操作。
 */

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMemoriaDatabase(@ApplicationContext context: Context): MemoriaDatabase {
        return Room.databaseBuilder(
            context,
            MemoriaDatabase::class.java,
            "memoria_database"
        ).build()
    }

    @Provides
    fun provideRawMemoryDao(database: MemoriaDatabase): RawMemoryDao {
        return database.rawMemoryDao()
    }
}