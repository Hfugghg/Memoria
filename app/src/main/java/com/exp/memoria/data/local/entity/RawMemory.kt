package com.exp.memoria.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * [原始记忆实体类]
 *
 * 职责:
 * 1. 定义 "RawMemory" 数据库表的结构 [cite: 52]。
 * 2. 字段需要与计划书中的数据模型完全对应：id, user_query, llm_response, timestamp [cite: 55, 56, 57, 58]。
 * 3. 使用 @Entity 注解将其标记为Room的表。
 * 4. 使用 @PrimaryKey, @ColumnInfo 等注解来配置表和字段的属性。
 *
 * 关联:
 * - 这个类是 MemoriaDatabase 中 entities 列表的一部分。
 * - RawMemoryDao 使用这个类作为其查询和插入操作的数据模型。
 */

@Entity(tableName = "raw_memory")
data class RawMemory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, // [cite: 55]
    val user_query: String, // [cite: 56]
    val llm_response: String, // [cite: 57]
    val timestamp: Long // [cite: 58]
)
