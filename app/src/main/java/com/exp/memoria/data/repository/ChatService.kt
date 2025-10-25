package com.exp.memoria.data.repository

import com.exp.memoria.data.remote.dto.ChatContent
import kotlinx.coroutines.flow.Flow

interface ChatService {
    fun chatResponse(
        history: List<ChatContent>,
        systemInstruction: String?,
        responseSchema: String?,
        isStreaming: Boolean = false
    ): Flow<ChatChunkResult>
}