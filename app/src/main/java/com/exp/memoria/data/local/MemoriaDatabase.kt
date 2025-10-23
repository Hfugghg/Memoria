package com.exp.memoria.data.local

import android.util.Log
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.exp.memoria.data.local.converters.Converters
import com.exp.memoria.data.local.dao.CondensedMemoryDao
import com.exp.memoria.data.local.dao.ConversationHeaderDao
import com.exp.memoria.data.local.dao.MessageFileDao
import com.exp.memoria.data.local.dao.RawMemoryDao
import com.exp.memoria.data.local.entity.CondensedMemory
import com.exp.memoria.data.local.entity.ConversationHeader
import com.exp.memoria.data.local.entity.FTSMemoryIndex
import com.exp.memoria.data.local.entity.MessageFile
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
    entities = [RawMemory::class, CondensedMemory::class, FTSMemoryIndex::class, ConversationHeader::class, MessageFile::class],
    version = 5, // 数据库版本从 4 增加到 5
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MemoriaDatabase : RoomDatabase() {
    abstract fun rawMemoryDao(): RawMemoryDao
    abstract fun condensedMemoryDao(): CondensedMemoryDao
    abstract fun conversationHeaderDao(): ConversationHeaderDao
    abstract fun messageFileDao(): MessageFileDao

    companion object {
        private const val TAG = "MemoriaDatabase"
        val FTS_TABLE_CALLBACK = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                Log.d(TAG, "数据库首次创建，开始初始化FTS5虚拟表和触发器...")
                // 1. 创建 FTS5 虚拟表 (这部分是正确的)
                db.execSQL(
                    """
                CREATE VIRTUAL TABLE IF NOT EXISTS condensed_memory_fts USING fts5(
                    summary_text,
                    content='condensed_memory',
                    content_rowid='id'
                )
                """.trimIndent()
                )
                Log.d(TAG, "FTS5虚拟表 'condensed_memory_fts' 创建成功。")

                // 2. 创建 INSERT 触发器 (这部分是正确的)
                db.execSQL(
                    """
                CREATE TRIGGER IF NOT EXISTS condensed_memory_after_insert
                AFTER INSERT ON condensed_memory
                BEGIN
                    INSERT INTO condensed_memory_fts(rowid, summary_text) 
                    VALUES (new.id, new.summary_text);
                END
                """.trimIndent()
                )
                Log.d(TAG, "INSERT触发器 'condensed_memory_after_insert' 创建成功。")


                // 3. 创建 DELETE 触发器 (🔥 最终修正)
                // 使用标准的 DELETE 语句，而不是特殊的 INSERT
                db.execSQL(
                    """
                CREATE TRIGGER IF NOT EXISTS condensed_memory_after_delete
                AFTER DELETE ON condensed_memory
                BEGIN
                    DELETE FROM condensed_memory_fts WHERE rowid = old.id;
                END
                """.trimIndent()
                )
                Log.d(TAG, "DELETE触发器 'condensed_memory_after_delete' 创建成功。")


                // 4. 创建 UPDATE 触发器 (🔥 最终修正)
                // 逻辑分解为先删除旧索引，再插入新索引
                db.execSQL(
                    """
                CREATE TRIGGER IF NOT EXISTS condensed_memory_after_update
                AFTER UPDATE ON condensed_memory
                BEGIN
                    DELETE FROM condensed_memory_fts WHERE rowid = old.id;
                    INSERT INTO condensed_memory_fts(rowid, summary_text) 
                    VALUES (new.id, new.summary_text);
                END
                """.trimIndent()
                )
                Log.d(TAG, "UPDATE触发器 'condensed_memory_after_update' 创建成功。")
                Log.d(TAG, "数据库初始化完成。")
            }
        }

        // 数据库迁移，从版本 1 到版本 2
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 为 conversation_header 表添加 name 列
                db.execSQL("ALTER TABLE conversation_header ADD COLUMN name TEXT NOT NULL DEFAULT '新对话'")
                // 为 condensed_memory 表添加 conversationId 列
                db.execSQL("ALTER TABLE condensed_memory ADD COLUMN conversationId TEXT NOT NULL DEFAULT ''")
            }
        }

        // 数据库迁移，从版本 2 到版本 3
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 为 conversation_header 表添加 responseSchema 和 systemInstruction 列
                db.execSQL("ALTER TABLE conversation_header ADD COLUMN responseSchema TEXT")
                db.execSQL("ALTER TABLE conversation_header ADD COLUMN systemInstruction TEXT")
            }
        }

        // 数据库迁移，从版本 3 到版本 4：添加 totalTokenCount 列
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.d(TAG, "执行 MIGRATION_3_4: 为 conversation_header 表添加 totalTokenCount 列")
                // 添加 totalTokenCount 列，默认值为 0
                db.execSQL("ALTER TABLE conversation_header ADD COLUMN totalTokenCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        // 数据库迁移，从版本 4 到版本 5：添加 message_files 表
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.d(TAG, "执行 MIGRATION_4_5: 创建 message_files 表")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `message_files` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `rawMemoryId` INTEGER NOT NULL,
                        `fileName` TEXT NOT NULL,
                        `fileType` TEXT NOT NULL,
                        `fileContentBase64` TEXT NOT NULL,
                        FOREIGN KEY(`rawMemoryId`) REFERENCES `raw_memory`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_message_files_rawMemoryId` ON `message_files` (`rawMemoryId`)")
                Log.d(TAG, "MIGRATION_4_5 完成。")
            }
        }
    }
}
