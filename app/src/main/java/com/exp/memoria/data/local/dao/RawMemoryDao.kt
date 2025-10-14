package com.exp.memoria.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.exp.memoria.data.local.entity.RawMemory

/**
 * [原始记忆数据访问对象 (DAO)]
 *
 * 职责:
 * 1. 定义与 "RawMemory" 表 [cite: 52] 交互的所有数据库操作方法。
 * 2. 提供插入(Insert)一个新的问答对(Q&A)到 RawMemory 表的方法 [cite: 13]。
 * 3. 提供根据时间戳查询最近N条对话记录的方法，用于获取“热记忆” [cite: 16]。
 * 4. 提供根据ID删除特定记忆的方法（为未来的用户管理功能做准备 [cite: 37]）。
 *
 * 关联:
 * - 这是一个Room DAO接口，方法上会使用 @Insert, @Query, @Delete 等注解。
 * - 其实现由Room在编译时自动生成。
 * - 该接口的实例由 DatabaseModule 提供，并被注入到 MemoryRepository 中使用。
 */

@Dao
interface RawMemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rawMemory: RawMemory)

    // 目前是占位函数
    @Query("SELECT * FROM raw_memory ORDER BY timestamp DESC LIMIT 10")
    suspend fun getRecentMemories(): List<RawMemory>
}
