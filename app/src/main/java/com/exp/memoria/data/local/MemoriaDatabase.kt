package com.exp.memoria.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.exp.memoria.data.local.dao.CondensedMemoryDao
import com.exp.memoria.data.local.dao.RawMemoryDao
import com.exp.memoria.data.local.entity.CondensedMemory
import com.exp.memoria.data.local.entity.RawMemory

/**
 * [Room数据库主类]
 *
 * 职责:
 * 1. 定义应用的Room数据库配置。
 * 2. 使用 @Database 注解，并列出所有的实体类（Entities），如 RawMemory 和 CondensedMemory [cite: 51]。
 * 3. 提供所有DAO的抽象访问方法。
 * 4. 配置数据库的版本号和迁移策略。
 * 5. 在此文件中，还需要通过特定方式关联 FTS5 虚拟表 (FTSMemoryIndex) 与 CondensedMemory 表 [cite: 69, 71]。
 *
 * 关联:
 * - 这是应用本地数据持久化的核心。
 * - DatabaseModule 会创建这个类的单例实例，并从中获取DAO提供给其他部分使用。
 *
 * 实现指导:
 * - @Database(entities = [RawMemory::class, CondensedMemory::class, FTSMemoryIndex::class], version = 1)
 * abstract class MemoriaDatabase : RoomDatabase()
 * - abstract fun rawMemoryDao(): RawMemoryDao
 * - abstract fun condensedMemoryDao(): CondensedMemoryDao
 * - FTSMemoryIndex 需要被定义为一个 @Entity 且使用 @Fts5 注解，并关联到 CondensedMemory 的 `summary_text` 字段 [cite: 64, 71]。
 */

@Database(entities = [RawMemory::class, CondensedMemory::class], version = 1, exportSchema = false)
abstract class MemoriaDatabase : RoomDatabase() {
    abstract fun rawMemoryDao(): RawMemoryDao
    abstract fun condensedMemoryDao(): CondensedMemoryDao
}