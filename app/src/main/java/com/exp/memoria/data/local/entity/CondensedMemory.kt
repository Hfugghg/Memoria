package com.exp.memoria.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * [浓缩记忆实体类]
 *
 * 职责:
 * 1. 定义 "CondensedMemory" 数据库表的结构 [cite: 59]。
 * 2. 字段需要与计划书中的数据模型完全对应：id, raw_memory_id, summary_text, vector_float32, vector_int8, status, timestamp [cite: 62, 63, 64, 65, 66, 67, 68]。
 * 3. 使用 @Entity 注解，并可能使用 @ForeignKey 来建立与 RawMemory 表的关联 [cite: 63]。
 * 4. `summary_text` 字段是全文检索引擎（FTS）的目标 [cite: 64, 70]。
 * 5. `vector_int8` 存储量化后的向量，用于快速相似度计算，实现“冷记忆”的检索增强 [cite: 17, 33, 66]。
 *
 * 关联:
 * - 这个类是 MemoriaDatabase 中 entities 列表的一部分。
 * - CondensedMemoryDao 使用这个类作为其操作的数据模型。
 *
 * 实现指导:
 * - 使用 @Entity(
 * tableName = "CondensedMemory",
 * foreignKeys = [ForeignKey(entity = RawMemory::class, parentColumns = ["id"], childColumns = ["raw_memory_id"])]
 * )
 * - @PrimaryKey(autoGenerate = true) val id: Long = 0
 * - @ColumnInfo(name = "raw_memory_id", index = true) val rawMemoryId: Long [cite: 63]
 * - @ColumnInfo(name = "summary_text") val summaryText: String [cite: 64] // 将用于FTS索引。
 * - @ColumnInfo(name = "vector_float32", typeAffinity = ColumnInfo.BLOB) val vectorFloat32: ByteArray? [cite: 65] // 处理后可清空。
 * - @ColumnInfo(name = "vector_int8", typeAffinity = ColumnInfo.BLOB) val vectorInt8: ByteArray? [cite: 66] // 用于快速相似度计算。
 * - @ColumnInfo(name = "status") val status: String [cite: 67] // "NEW" 或 "INDEXED"。
 * - @ColumnInfo(name = "timestamp") val timestamp: Long [cite: 68]
 */

@Entity(
    tableName = "condensed_memory",
    foreignKeys = [
        ForeignKey(
            entity = RawMemory::class,
            parentColumns = ["id"],
            childColumns = ["raw_memory_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CondensedMemory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, // [cite: 62]
    @ColumnInfo(name = "raw_memory_id", index = true)
    val raw_memory_id: Long, // [cite: 63]
    @ColumnInfo(name = "conversationId", index = true)
    val conversationId: String, // 新增的 conversationId 字段
    val summary_text: String, // [cite: 64]
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val vector_int8: List<Float>?, // [cite: 66]
    val status: String, // [cite: 67]
    val timestamp: Long // [cite: 68]
)
