package com.exp.memoria.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.exp.memoria.data.local.dao.CondensedMemoryDao
import com.exp.memoria.data.local.dao.RawMemoryDao
import com.exp.memoria.data.local.entity.CondensedMemory
import com.exp.memoria.data.local.entity.RawMemory

@Database(entities = [RawMemory::class, CondensedMemory::class], version = 1, exportSchema = false)
abstract class MemoriaDatabase : RoomDatabase() {
    abstract fun rawMemoryDao(): RawMemoryDao
    abstract fun condensedMemoryDao(): CondensedMemoryDao
}