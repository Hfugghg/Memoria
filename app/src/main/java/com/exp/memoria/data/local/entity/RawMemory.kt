package com.exp.memoria.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index // 导入 Index
import com.exp.memoria.data.Content

/**
 * [原始记忆实体类]
 *
 * 职责:
 * 1. 定义 "RawMemory" 数据库表的结构。
 * 2. 字段需要与计划书中的数据模型完全对应：id, contents, timestamp。
 * 3. 使用 @Entity 注解将其标记为Room的表。
 * 4. 使用 @PrimaryKey, @ColumnInfo 等注解来配置表和字段的属性。
 *
 * 关联:
 * - 这个类是 MemoriaDatabase 中 entities 列表的一部分。
 * - RawMemoryDao 使用这个类作为其查询和插入操作的数据模型。
 */

@Entity(
    tableName = "raw_memory",
    indices = [Index(value = ["conversationId"], unique = true)] // 为 conversationId 添加唯一索引
)
data class RawMemory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: String,
    val contents: List<Content>,
    val timestamp: Long
)
