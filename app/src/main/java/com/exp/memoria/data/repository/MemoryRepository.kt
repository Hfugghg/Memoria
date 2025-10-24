package com.exp.memoria.data.repository

import android.util.Log
import com.exp.memoria.data.local.dao.CondensedMemoryDao
import com.exp.memoria.data.local.dao.ConversationHeaderDao
import com.exp.memoria.data.local.dao.MessageFileDao
import com.exp.memoria.data.local.dao.RawMemoryDao
import com.exp.memoria.data.local.entity.CondensedMemory
import com.exp.memoria.data.local.entity.ConversationHeader
import com.exp.memoria.data.local.entity.ConversationInfo
import com.exp.memoria.data.local.entity.RawMemory
import com.exp.memoria.data.local.entity.MessageFile
import com.exp.memoria.data.model.FileAttachment // 导入 FileAttachment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject

/**
 * 记忆数据仓库
 *
 * 职责:
 * 1. 作为ViewModel与本地数据源（DAO）之间的唯一中间层。
 * 2. 封装所有与记忆存储和检索相关的业务逻辑，对上层屏蔽数据库细节。
 *
 * 3. 提供方法：
 *    - `saveNewMemory(...)`: 保存一个新的问答对。
 *    - `saveOnlyAiResponse(...)`: 只保存AI的回复（用于重说等场景）。
 *    - `getMemoryById(...)`: 获取指定的记忆。
 *    - `updateProcessedMemory(...)`: 更新一个已处理的记忆。
 *    - `getAllRawMemories()`: 获取所有原始记忆。
 *    - `getRawMemories(...)`: 获取分页的原始记忆。
 *    - `getConversations()`: 获取所有对话的列表。
 *    - `createNewConversation(...)`: 创建一个新的对话头部记录。
 *    - `updateConversationLastUpdate(...)`: 更新对话的最后更新时间。
 *    - `getAllRawMemoriesForConversation(...)`: 获取特定对话的所有原始记忆。
 *    - `deleteConversation(...)`: 删除指定 ID 的对话及其所有相关记忆。
 *    - `renameConversation(...)`: 重命名指定 ID 的对话。
 *    - `updateResponseSchema(...)`: 更新对话的响应模式。
 *    - `updateSystemInstruction(...)`: 更新对话的系统指令。
 *    - `getConversationHeaderById(...)`: 根据ID获取对话头部。
 *    - `updateTotalTokenCount(...)`: 更新对话的总令牌计数。
 *    - `updateMemoryText(...)`: 更新指定记忆的文本内容。
 *    - `deleteFrom(...)`: 删除指定ID及其之后的所有记忆。
 *    - `saveMessageFile(...)`: 保存一个与消息关联的文件。
 *    - `getMessageFilesForMemory(...)`: 获取与指定消息关联的所有文件。
 *    - `saveUserMemory(...)`: 保存用户消息。
 *    - `deleteMessageFile(...)`: 删除一个与消息关联的文件。
 *
 * 关联:
 * - 它会注入 RawMemoryDao 和 CondensedMemoryDao。
 * - GetChatResponseUseCase 会注入并使用这个Repository来管理记忆数据。
 */
interface MemoryRepository {
    /**
     * 保存一个新的问答对。
     *
     * @param query 用户查询文本。
     * @param response AI响应文本。
     * @param conversationId 对话ID。
     * @param attachments 与用户消息关联的文件附件列表。
     * @return 模型消息的ID。
     */
    suspend fun saveNewMemory(query: String, response: String, conversationId: String, attachments: List<FileAttachment>): Long
    /**
     * 只保存AI的回复（用于重说等场景）。
     *
     * @param userQuery 用户查询文本。
     * @param response AI响应文本。
     * @param conversationId 对话ID。
     * @return 模型消息的ID。
     */
    suspend fun saveOnlyAiResponse(userQuery: String, response: String, conversationId: String): Long
    /**
     * 根据ID获取指定的原始记忆。
     *
     * @param id 记忆的ID。
     * @return 对应的RawMemory对象，如果不存在则为null。
     */
    suspend fun getMemoryById(id: Long): RawMemory?
    /**
     * 更新一个已处理的记忆。
     *
     * @param id 记忆的ID。
     * @param summary 记忆的摘要文本。
     * @param vector 记忆的向量表示。
     */
    suspend fun updateProcessedMemory(id: Long, summary: String, vector: List<Float>)
    /**
     * 获取所有原始记忆。
     *
     * @return 所有RawMemory对象的列表。
     */
    suspend fun getAllRawMemories(): List<RawMemory>
    /**
     * 获取特定对话的所有原始记忆。
     *
     * @param conversationId 对话ID。
     * @return 特定对话的所有RawMemory对象的列表。
     */
    suspend fun getAllRawMemoriesForConversation(conversationId: String): List<RawMemory>
    /**
     * 获取分页的原始记忆。
     *
     * @param conversationId 对话ID。
     * @param limit 返回的最大记忆数量。
     * @param offset 跳过的记忆数量。
     * @return 分页的RawMemory对象的列表。
     */
    suspend fun getRawMemories(conversationId: String, limit: Int, offset: Int): List<RawMemory>
    /**
     * 获取所有对话的列表。
     *
     * @return 包含ConversationInfo对象的Flow。
     */
    fun getConversations(): Flow<List<ConversationInfo>>
    /**
     * 创建一个新的对话头部记录。
     *
     * @param conversationId 新对话的ID。
     */
    suspend fun createNewConversation(conversationId: String)
    /**
     * 更新对话的最后更新时间。
     *
     * @param conversationId 对话ID。
     * @param timestamp 最后更新时间戳。
     */
    suspend fun updateConversationLastUpdate(conversationId: String, timestamp: Long)
    /**
     * 删除指定 ID 的对话及其所有相关记忆。
     *
     * @param conversationId 要删除的对话ID。
     */
    suspend fun deleteConversation(conversationId: String)
    /**
     * 重命名指定 ID 的对话。
     *
     * @param conversationId 要重命名的对话ID。
     * @param newName 新的对话名称。
     */
    suspend fun renameConversation(conversationId: String, newName: String)
    /**
     * 更新对话的响应模式。
     *
     * @param conversationId 对话ID。
     * @param responseSchema 新的响应模式字符串，如果为null则清除。
     */
    suspend fun updateResponseSchema(conversationId: String, responseSchema: String?)
    /**
     * 更新对话的系统指令。
     *
     * @param conversationId 对话ID。
     * @param systemInstruction 新的系统指令字符串，如果为null则清除。
     */
    suspend fun updateSystemInstruction(conversationId: String, systemInstruction: String?)
    /**
     * 根据ID获取对话头部。
     *
     * @param conversationId 对话ID。
     * @return 对应的ConversationHeader对象，如果不存在则为null。
     */
    suspend fun getConversationHeaderById(conversationId: String): ConversationHeader?
    /**
     * 更新对话的总令牌计数。
     *
     * @param conversationId 对话ID。
     * @param totalTokenCount 新的总令牌计数。
     */
    suspend fun updateTotalTokenCount(conversationId: String, totalTokenCount: Int)
    /**
     * 更新指定记忆的文本内容。
     *
     * @param memoryId 记忆ID。
     * @param newText 新的文本内容。
     */
    suspend fun updateMemoryText(memoryId: Long, newText: String)
    /**
     * 删除指定ID及其之后的所有记忆。
     *
     * @param conversationId 对话ID。
     * @param id 起始删除的记忆ID。
     */
    suspend fun deleteFrom(conversationId: String, id: Long)
    /**
     * 保存一个与消息关联的文件。
     *
     * @param file 要保存的MessageFile对象。
     * @return 插入的文件ID。
     */
    suspend fun saveMessageFile(file: MessageFile): Long
    /**
     * 获取与指定消息关联的所有文件。
     *
     * @param rawMemoryId 原始记忆的ID。
     * @return 与该记忆关联的MessageFile列表。
     */
    suspend fun getMessageFilesForMemory(rawMemoryId: Long): List<MessageFile>
    /**
     * 保存用户消息。
     *
     * @param query 用户查询文本。
     * @param conversationId 对话ID。
     * @return 插入的用户消息ID。
     */
    suspend fun saveUserMemory(query: String, conversationId: String): Long

    /**
     * 删除一个与消息关联的文件。
     *
     * @param file 要删除的MessageFile对象。
     */
    suspend fun deleteMessageFile(file: MessageFile)
}

class MemoryRepositoryImpl @Inject constructor(
    private val rawMemoryDao: RawMemoryDao,
    private val condensedMemoryDao: CondensedMemoryDao,
    private val conversationHeaderDao: ConversationHeaderDao,
    private val messageFileDao: MessageFileDao // 注入 MessageFileDao
) : MemoryRepository {

    override suspend fun saveNewMemory(query: String, response: String, conversationId: String, attachments: List<FileAttachment>): Long {
        val now = System.currentTimeMillis()

        // 确保对话头部存在或创建，并更新时间戳
        if (conversationHeaderDao.countConversationHeaders(conversationId) == 0) {
            conversationHeaderDao.insert(ConversationHeader(conversationId, "新对话", now, now))
        } else {
            conversationHeaderDao.getConversationHeaderById(conversationId)?.let {
                conversationHeaderDao.update(it.copy(lastUpdateTimestamp = now))
            }
        }

        // 插入用户消息 (query)
        val userMemory = RawMemory(
            conversationId = conversationId,
            sender = "user",
            text = query,
            timestamp = now - 1 // To preserve order
        )
        val userMemoryId = rawMemoryDao.insert(userMemory)

        // 保存与用户消息关联的附件
        attachments.forEach { attachment ->
            val messageFile = MessageFile(
                rawMemoryId = userMemoryId,
                fileName = attachment.fileName,
                fileType = attachment.fileType ?: "application/octet-stream", // 修复点1：处理可空性
                fileContentBase64 = attachment.base64Data // 修复点2：使用正确的参数名和数据
            )
            messageFileDao.insert(messageFile)
        }

        // 插入模型消息 (response)
        val modelMemory = RawMemory(
            conversationId = conversationId,
            sender = "model",
            text = response,
            timestamp = now
        )
        val modelMemoryId = rawMemoryDao.insert(modelMemory)

        // 创建一个与模型响应关联的精简记忆
        val condensedMemory = CondensedMemory(
            raw_memory_id = modelMemoryId,
            conversationId = conversationId,
            summary_text = "",
            vector_int8 = null,
            status = "NEW",
            timestamp = now
        )
        condensedMemoryDao.insert(condensedMemory)

        return modelMemoryId
    }

    override suspend fun saveUserMemory(query: String, conversationId: String): Long {
        val now = System.currentTimeMillis()

        // 确保对话头存在或创建，并更新时间戳
        if (conversationHeaderDao.countConversationHeaders(conversationId) == 0) {
            conversationHeaderDao.insert(ConversationHeader(conversationId, "新对话", now, now))
        } else {
            conversationHeaderDao.getConversationHeaderById(conversationId)?.let {
                conversationHeaderDao.update(it.copy(lastUpdateTimestamp = now))
            }
        }

        // 插入用户消息 (query)
        val userMemory = RawMemory(
            conversationId = conversationId,
            sender = "user",
            text = query,
            timestamp = now
        )
        return rawMemoryDao.insert(userMemory)
    }

    override suspend fun saveOnlyAiResponse(userQuery: String, response: String, conversationId: String): Long {
        val now = System.currentTimeMillis()

        // 确保对话头部存在并更新其时间戳
        conversationHeaderDao.getConversationHeaderById(conversationId)?.let {
            conversationHeaderDao.update(it.copy(lastUpdateTimestamp = now))
        } // 如果头部不存在，我们假定这是一个异常并继续，因为 saveNewMemory 应该已经创建了它。

        // 插入模型消息 (response)
        val modelMemory = RawMemory(
            conversationId = conversationId,
            sender = "model",
            text = response,
            timestamp = now
        )
        val modelMemoryId = rawMemoryDao.insert(modelMemory)

        // 创建一个与模型响应关联的精简记忆
        val condensedMemory = CondensedMemory(
            raw_memory_id = modelMemoryId,
            conversationId = conversationId,
            summary_text = "", // 摘要将由工作器生成
            vector_int8 = null,
            status = "NEW",
            timestamp = now
        )
        condensedMemoryDao.insert(condensedMemory)

        return modelMemoryId
    }


    override suspend fun getMemoryById(id: Long): RawMemory? {
        return rawMemoryDao.getById(id)
    }

    override suspend fun updateProcessedMemory(id: Long, summary: String, vector: List<Float>) {
        val byteBuffer = ByteBuffer.allocate(vector.size * 4).order(ByteOrder.nativeOrder())
        for (value in vector) {
            byteBuffer.putFloat(value)
        }
        val byteArray = byteBuffer.array()

        condensedMemoryDao.updateProcessedMemory(id, summary, byteArray)
    }

    override suspend fun getAllRawMemories(): List<RawMemory> {
        return rawMemoryDao.getAll()
    }

    override suspend fun getAllRawMemoriesForConversation(conversationId: String): List<RawMemory> {
        return rawMemoryDao.getAllForConversation(conversationId)
    }

    override suspend fun getRawMemories(conversationId: String, limit: Int, offset: Int): List<RawMemory> {
        return rawMemoryDao.getWithLimitOffset(conversationId, limit, offset)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getConversations(): Flow<List<ConversationInfo>> {
        // 监听所有对话头部的变化
        return conversationHeaderDao.getAllConversationHeaders()
            .flatMapLatest { headers ->
                if (headers.isEmpty()) {
                    // 如果没有对话，立即返回一个空的Flow
                    return@flatMapLatest flowOf(emptyList())
                }
                // 对于每个对话头，创建一个Flow来获取其最新的消息，并映射成ConversationInfo
                val conversationInfoFlows = headers.map { header ->
                    rawMemoryDao.getLatestMemoryForConversation(header.conversationId)
                        .map { latestRawMemory ->
                            ConversationInfo(
                                conversationId = header.conversationId,
                                name = header.name,
                                // 使用最新消息的时间戳，如果不存在（比如刚创建的对话），则使用对话创建时的时间戳
                                lastTimestamp = latestRawMemory?.timestamp ?: header.creationTimestamp
                            )
                        }
                }
                // 将多个ConversationInfo的Flow合并成一个Flow<List<ConversationInfo>>
                combine(conversationInfoFlows) { conversationInfos ->
                    // 每当任何一个对话的最新消息更新时，这个代码块就会执行
                    // 对合并后的列表按时间戳降序排序
                    conversationInfos.sortedByDescending { it.lastTimestamp }
                }
            }
    }

    override suspend fun createNewConversation(conversationId: String) {
        val now = System.currentTimeMillis()
        val newHeader = ConversationHeader(conversationId, "新对话", now, now)
        conversationHeaderDao.insert(newHeader)
    }

    override suspend fun updateConversationLastUpdate(conversationId: String, timestamp: Long) {
        val header = conversationHeaderDao.getConversationHeaderById(conversationId)
        header?.let {
            conversationHeaderDao.update(it.copy(lastUpdateTimestamp = timestamp))
        }
    }

    override suspend fun deleteConversation(conversationId: String) {
        rawMemoryDao.deleteByConversationId(conversationId)
        condensedMemoryDao.deleteByConversationId(conversationId)
        conversationHeaderDao.deleteByConversationId(conversationId)
        // 修复点3：移除多余且错误的调用，因为数据库会通过外键级联删除
    }

    override suspend fun renameConversation(conversationId: String, newName: String) {
        Log.d("MemoryRepository", "Attempting to rename conversation. ID: $conversationId, New Name: $newName")
        conversationHeaderDao.getConversationHeaderById(conversationId)?.let { header ->
            Log.d("MemoryRepository", "Found existing header: $header")
            val updatedHeader = header.copy(
                name = newName,
                lastUpdateTimestamp = System.currentTimeMillis()
            )
            Log.d("MemoryRepository", "Updating header to: $updatedHeader")
            conversationHeaderDao.update(updatedHeader)
            Log.d("MemoryRepository", "Conversation renamed successfully in DAO.")
        } ?: run {
            Log.w("MemoryRepository", "Conversation header not found for ID: $conversationId. Cannot rename.")
        }
    }

    override suspend fun updateResponseSchema(conversationId: String, responseSchema: String?) {
        Log.d("MemoryRepository", "[Schema] Attempting to update for ID: $conversationId")
        conversationHeaderDao.getConversationHeaderById(conversationId)?.let {
            Log.d(
                "MemoryRepository",
                "[Schema] Found header. Current schema: '${it.responseSchema}'. New schema: '$responseSchema'"
            )
            val updatedHeader = it.copy(responseSchema = responseSchema)
            conversationHeaderDao.update(updatedHeader)
            Log.d("MemoryRepository", "[Schema] DAO update called for ID: $conversationId")
        } ?: run {
            Log.w("MemoryRepository", "[Schema] Header NOT FOUND for ID: $conversationId. Update failed.")
        }
    }

    override suspend fun updateSystemInstruction(conversationId: String, systemInstruction: String?) {
        Log.d("MemoryRepository", "[Instruction] Attempting to update for ID: $conversationId")
        conversationHeaderDao.getConversationHeaderById(conversationId)?.let {
            Log.d(
                "MemoryRepository",
                "[Instruction] Found header. Current instruction: '${it.systemInstruction}'. New instruction: '$systemInstruction'"
            )
            val updatedHeader = it.copy(systemInstruction = systemInstruction)
            conversationHeaderDao.update(updatedHeader)
            Log.d("MemoryRepository", "[Instruction] DAO update called for ID: $conversationId")
        } ?: run {
            Log.w("MemoryRepository", "[Instruction] Header NOT FOUND for ID: $conversationId. Update failed.")
        }
    }

    override suspend fun getConversationHeaderById(conversationId: String): ConversationHeader? {
        return conversationHeaderDao.getConversationHeaderById(conversationId)
    }

    override suspend fun updateTotalTokenCount(conversationId: String, totalTokenCount: Int) {
        conversationHeaderDao.updateTotalTokenCount(conversationId, totalTokenCount)
    }

    override suspend fun updateMemoryText(memoryId: Long, newText: String) {
        rawMemoryDao.updateTextById(memoryId, newText)
    }

    override suspend fun deleteFrom(conversationId: String, id: Long) {
        rawMemoryDao.deleteFrom(conversationId, id)
        condensedMemoryDao.deleteFrom(conversationId, id)
        // 注意：这里的级联删除也会自动处理 MessageFile
    }

    override suspend fun saveMessageFile(file: MessageFile): Long {
        return messageFileDao.insert(file)
    }

    override suspend fun getMessageFilesForMemory(rawMemoryId: Long): List<MessageFile> {
        return messageFileDao.getFilesForMemory(rawMemoryId)
    }

    override suspend fun deleteMessageFile(file: MessageFile) {
        messageFileDao.delete(file)
    }
}