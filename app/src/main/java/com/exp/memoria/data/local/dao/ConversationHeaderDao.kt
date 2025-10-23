package com.exp.memoria.data.local.dao

import androidx.room.*
import com.exp.memoria.data.local.entity.ConversationHeader
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationHeaderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conversationHeader: ConversationHeader)

    @Update
    suspend fun update(conversationHeader: ConversationHeader)

    @Query("SELECT * FROM conversation_header")
    fun getAllConversationHeaders(): Flow<List<ConversationHeader>>

    @Query("SELECT COUNT(*) FROM conversation_header WHERE conversationId = :conversationId")
    suspend fun countConversationHeaders(conversationId: String): Int

    @Query("DELETE FROM conversation_header WHERE conversationId = :conversationId")
    suspend fun deleteByConversationId(conversationId: String)

    @Query("SELECT * FROM conversation_header WHERE conversationId = :conversationId")
    suspend fun getConversationHeaderById(conversationId: String): ConversationHeader?

    @Query("UPDATE conversation_header SET responseSchema = :responseSchema WHERE conversationId = :conversationId")
    suspend fun updateResponseSchema(conversationId: String, responseSchema: String?)

    @Query("UPDATE conversation_header SET systemInstruction = :systemInstruction WHERE conversationId = :conversationId")
    suspend fun updateSystemInstruction(conversationId: String, systemInstruction: String?)

    @Query("UPDATE conversation_header SET totalTokenCount = :totalTokenCount WHERE conversationId = :conversationId")
    suspend fun updateTotalTokenCount(conversationId: String, totalTokenCount: Int)
}
