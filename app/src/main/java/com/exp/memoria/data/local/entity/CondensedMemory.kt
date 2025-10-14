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
    val id: Int = 0, // [cite: 62]
@ColumnInfo(name = "raw_memory_id", index = true)
    val raw_memory_id: Int, // [cite: 63]
    val summary_text: String, // [cite: 64]
    val vector_int8: ByteArray?, // [cite: 66]
    val status: String, // [cite: 67]
    val timestamp: Long // [cite: 68]
) {
    // ByteArray 比较需要 equals/hashCode
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CondensedMemory

        if (id != other.id) return false
        if (raw_memory_id != other.raw_memory_id) return false
        if (summary_text != other.summary_text) return false
        if (vector_int8 != null) {
            if (other.vector_int8 == null) return false
            if (!vector_int8.contentEquals(other.vector_int8)) return false
        } else if (other.vector_int8 != null) return false
        if (status != other.status) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + raw_memory_id
        result = 31 * result + summary_text.hashCode()
        result = 31 * result + (vector_int8?.contentHashCode() ?: 0)
        result = 31 * result + status.hashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}
