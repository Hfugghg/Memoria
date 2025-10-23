package com.exp.memoria.data.repository.llmrepository

import com.exp.memoria.data.repository.ChatChunkResult
import com.exp.memoria.data.remote.dto.ChatContent
import com.exp.memoria.data.model.FileAttachment
import kotlinx.coroutines.flow.Flow

interface ChatService {
    fun chatResponse(
        history: List<ChatContent>,
        systemInstruction: String?,
        responseSchema: String?,
        isStreaming: Boolean = false,
        attachments: List<FileAttachment> = emptyList()
    ): Flow<ChatChunkResult>
}