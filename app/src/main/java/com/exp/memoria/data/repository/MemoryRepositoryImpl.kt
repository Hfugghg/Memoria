package com.exp.memoria.data.repository

import com.exp.memoria.data.local.entity.ConversationHeader
import com.exp.memoria.data.local.entity.ConversationInfo
import com.exp.memoria.data.local.entity.MessageFile
import com.exp.memoria.data.local.entity.RawMemory
import com.exp.memoria.data.model.FileAttachment
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MemoryRepositoryImpl @Inject constructor(
    private val fileAttachmentRepository: FileAttachmentRepository,
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository
) : MemoryRepository {

    override suspend fun saveMessageFile(file: MessageFile): Long {
        return fileAttachmentRepository.saveMessageFile(file)
    }

    override suspend fun getMessageFilesForMemory(rawMemoryId: Long): List<MessageFile> {
        return fileAttachmentRepository.getMessageFilesForMemory(rawMemoryId)
    }

    override suspend fun deleteMessageFile(file: MessageFile) {
        fileAttachmentRepository.deleteMessageFile(file)
    }

    override suspend fun saveNewMemory(query: String, response: String, conversationId: String, attachments: List<FileAttachment>): Long {
        conversationRepository.updateConversationLastUpdate(conversationId, System.currentTimeMillis())
        return messageRepository.saveNewMemory(query, response, conversationId, attachments)
    }

    override suspend fun saveOnlyAiResponse(userQuery: String, response: String, conversationId: String): Long {
        conversationRepository.updateConversationLastUpdate(conversationId, System.currentTimeMillis())
        return messageRepository.saveOnlyAiResponse(userQuery, response, conversationId)
    }

    override suspend fun getMemoryById(id: Long): RawMemory? {
        return messageRepository.getMemoryById(id)
    }

    override suspend fun updateProcessedMemory(id: Long, summary: String, vector: List<Float>) {
        messageRepository.updateProcessedMemory(id, summary, vector)
    }

    override suspend fun getAllRawMemories(): List<RawMemory> {
        return messageRepository.getAllRawMemories()
    }

    override suspend fun getAllRawMemoriesForConversation(conversationId: String): List<RawMemory> {
        return messageRepository.getAllRawMemoriesForConversation(conversationId)
    }

    override suspend fun getRawMemories(conversationId: String, limit: Int, offset: Int): List<RawMemory> {
        return messageRepository.getRawMemories(conversationId, limit, offset)
    }

    override suspend fun updateMemoryText(memoryId: Long, newText: String) {
        messageRepository.updateMemoryText(memoryId, newText)
    }

    override suspend fun deleteFrom(conversationId: String, id: Long) {
        messageRepository.deleteFrom(conversationId, id)
    }

    override suspend fun saveUserMemory(query: String, conversationId: String): Long {
        conversationRepository.updateConversationLastUpdate(conversationId, System.currentTimeMillis())
        return messageRepository.saveUserMemory(query, conversationId)
    }

    override fun getConversations(): Flow<List<ConversationInfo>> {
        return conversationRepository.getConversations()
    }

    override suspend fun createNewConversation(conversationId: String) {
        conversationRepository.createNewConversation(conversationId)
    }

    override suspend fun updateConversationLastUpdate(conversationId: String, timestamp: Long) {
        conversationRepository.updateConversationLastUpdate(conversationId, timestamp)
    }

    override suspend fun deleteConversation(conversationId: String) {
        conversationRepository.deleteConversation(conversationId)
    }

    override suspend fun renameConversation(conversationId: String, newName: String) {
        conversationRepository.renameConversation(conversationId, newName)
    }

    override suspend fun updateResponseSchema(conversationId: String, responseSchema: String?) {
        conversationRepository.updateResponseSchema(conversationId, responseSchema)
    }

    override suspend fun updateSystemInstruction(conversationId: String, systemInstruction: String?) {
        conversationRepository.updateSystemInstruction(conversationId, systemInstruction)
    }

    override suspend fun getConversationHeaderById(conversationId: String): ConversationHeader? {
        return conversationRepository.getConversationHeaderById(conversationId)
    }

    override suspend fun updateTotalTokenCount(conversationId: String, totalTokenCount: Int) {
        conversationRepository.updateTotalTokenCount(conversationId, totalTokenCount)
    }
}
