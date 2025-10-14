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