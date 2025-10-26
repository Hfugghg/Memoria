package com.exp.memoria.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.exp.memoria.data.local.entity.CondensedMemory

/**
 * [浓缩记忆数据访问对象 (DAO)]
 *
 * 职责:
 * 1. 定义与 "CondensedMemory" 表 [cite: 59] 和 FTS 虚拟表 [cite: 69] 交互的所有数据库操作方法。
 * 2. 提供插入(Insert)一条新的浓缩记忆的方法。
 * 3. 提供根据状态(status)字段查询待处理记忆("NEW")的方法，供后台Worker使用 [cite: 67]。
 * 4. 提供更新(Update)一条记忆记录的方法，例如，在处理完成后，将其向量、状态等更新 [cite: 67]。
 * 5. 定义核心的检索方法：
 * - 一个使用FTS5进行全文搜索的方法，用于快速预过滤 [cite: 33]。
 * - 一个根据ID列表查询所有量化向量(vector_int8)的方法，用于向量精排 [cite: 33, 66]。
 *
 * 关联:
 * - Room DAO接口，使用 @Insert, @Query, @Update 等注解。
 * - 它的实例由 DatabaseModule 提供，并被注入到 MemoryRepository 中。
 *
 * 实现指导:
 * - @Dao 注解接口。
 * - fun insert(condensedMemory: CondensedMemory): Long
 * - @Query("UPDATE CondensedMemory SET summary_text = :summary, vector_int8 = :vector, status = 'INDEXED' WHERE id = :id")
 * fun updateProcessedMemory(id: Long, summary: String, vector: ByteArray) // 更新处理完成的记忆。
 * - @Query("SELECT * FROM CondensedMemory WHERE status = 'NEW'")
 * fun getUnprocessedMemories(): List<CondensedMemory> // 获取待处理的记忆。
 * - @Query("SELECT rowid FROM FTSMemoryIndex WHERE FTSMemoryIndex MATCH :query")
 * fun searchFtsIndex(query: String): List<Long> // FTS预过滤，返回匹配的CondensedMemory的ID。
 * - @Query("SELECT id, vector_int8 FROM CondensedMemory WHERE id IN (:ids)")
 * fun getVectorsByIds(ids: List<Long>): List<Pair<Long, ByteArray>> // 根据ID获取向量用于精排。
 */

@Dao
interface CondensedMemoryDao {
    @Insert
    suspend fun insert(condensedMemory: CondensedMemory): Long

    @Update
    suspend fun update(condensedMemory: CondensedMemory)

    @Query("SELECT * FROM condensed_memory WHERE status = 'NEW'")
    suspend fun getUnprocessedMemories(): List<CondensedMemory>

    @Query("SELECT T.raw_memory_id FROM condensed_memory AS T JOIN condensed_memory_fts AS F ON T.id = F.rowid WHERE F.summary_text MATCH :query")
    suspend fun searchFtsIndex(query: String): List<Long>

    @Query("SELECT raw_memory_id, vector_int8 FROM condensed_memory WHERE raw_memory_id IN (:ids)")
    suspend fun getVectorsByIds(ids: List<Long>): List<MemoryVector>

    @Query("DELETE FROM condensed_memory WHERE conversationId = :conversationId")
    suspend fun deleteAllInConversation(conversationId: String)

    @Query("DELETE FROM condensed_memory WHERE conversationId = :conversationId AND raw_memory_id >= :id")
    suspend fun deleteFrom(conversationId: String, id: Long)

    @Query("SELECT * FROM condensed_memory WHERE raw_memory_id = :rawMemoryId")
    suspend fun getCondensedMemoryByRawMemoryId(rawMemoryId: Long): CondensedMemory?
}

data class MemoryVector(
    @ColumnInfo(name = "raw_memory_id") // 这里的名字必须和查询语句中的列名完全一致
    val rawMemoryId: Long,

    @ColumnInfo(name = "vector_int8") // 这里的名字必须和查询语句中的列名完全一致
    val vector: ByteArray?
) {
    // 同样，因为包含 ByteArray，最好重写 equals 和 hashCode
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MemoryVector

        if (rawMemoryId != other.rawMemoryId) return false
        if (vector != null) {
            if (other.vector == null) return false
            if (!vector.contentEquals(other.vector)) return false
        } else if (other.vector != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rawMemoryId.hashCode()
        result = 31 * result + (vector?.contentHashCode() ?: 0)
        return result
    }
}
