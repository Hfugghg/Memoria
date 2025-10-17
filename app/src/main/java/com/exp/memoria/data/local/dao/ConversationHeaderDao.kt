package com.exp.memoria.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.exp.memoria.data.local.entity.ConversationHeader

@Dao
interface ConversationHeaderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conversationHeader: ConversationHeader)

    @Update
    suspend fun update(conversationHeader: ConversationHeader)

    @Query("SELECT * FROM conversation_header ORDER BY lastUpdateTimestamp DESC")
    suspend fun getAllConversationHeaders(): List<ConversationHeader>

    @Query("SELECT COUNT(*) FROM conversation_header WHERE conversationId = :conversationId")
    suspend fun countConversationHeaders(conversationId: String): Int

    @Query("DELETE FROM conversation_header WHERE conversationId = :conversationId")
    suspend fun deleteConversationHeader(conversationId: String)
}
