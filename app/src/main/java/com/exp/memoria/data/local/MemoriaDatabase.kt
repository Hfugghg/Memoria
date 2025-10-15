package com.exp.memoria.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.exp.memoria.data.local.dao.CondensedMemoryDao
import com.exp.memoria.data.local.dao.RawMemoryDao
import com.exp.memoria.data.local.entity.CondensedMemory
import com.exp.memoria.data.local.entity.FTSMemoryIndex
import com.exp.memoria.data.local.entity.RawMemory

/**
 * [Room数据库主类]
 *
 * 职责:
 * 1. 定义应用的Room数据库配置。
 * 2. 使用 @Database 注解，并列出所有的实体类（Entities）。
 * 3. 提供所有DAO的抽象访问方法。
 * 4. 配置数据库的版本号和迁移策略。
 * 5. 通过回调函数，在数据库创建时，使用原生 SQL 创建 FTS5 虚拟表，并建立触发器来同步数据。
 *
 * 关联:
 * - 这是应用本地数据持久化的核心。
 * - AppModule 会创建这个类的单例实例，并从中获取DAO提供给其他部分使用。
 *
 */
@Database(
    entities = [RawMemory::class, CondensedMemory::class, FTSMemoryIndex::class],
    version = 1,
    exportSchema = false
)
abstract class MemoriaDatabase : RoomDatabase() {
    abstract fun rawMemoryDao(): RawMemoryDao
    abstract fun condensedMemoryDao(): CondensedMemoryDao

    companion object {
        val FTS_TABLE_CALLBACK = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // 创建 FTS5 虚拟表
                db.execSQL(
                    """
                    CREATE VIRTUAL TABLE IF NOT EXISTS condensed_memory_fts USING fts5(
                        summary_text,
                        content='condensed_memory',
                        content_rowid='id'
                    )
                    """.trimIndent()
                )
                // 创建触发器，在 condensed_memory 表发生变化时，同步更新 FTS 表
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS condensed_memory_after_insert
                    AFTER INSERT ON condensed_memory
                    BEGIN
                        INSERT INTO condensed_memory_fts(rowid, summary_text) VALUES (new.id, new.summary_text);
                    END
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS condensed_memory_after_delete
                    AFTER DELETE ON condensed_memory
                    BEGIN
                        INSERT INTO condensed_memory_fts(condensed_memory_fts, rowid, summary_text) VALUES ('delete', old.id, old.summary_text);
                    END
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS condensed_memory_after_update
                    AFTER UPDATE ON condensed_memory
                    BEGIN
                        INSERT INTO condensed_memory_fts(condensed_memory_fts, rowid, summary_text) VALUES ('delete', old.id, old.summary_text);
                        INSERT INTO condensed_memory_fts(rowid, summary_text) VALUES (new.id, new.summary_text);
                    END
                    """.trimIndent()
                )
            }
        }
    }
}
