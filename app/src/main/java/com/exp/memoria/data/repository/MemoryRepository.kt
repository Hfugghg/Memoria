package com.exp.memoria.data.repository

import com.exp.memoria.data.local.entity.ConversationInfo
import com.exp.memoria.data.local.entity.RawMemory
import com.exp.memoria.data.model.FileAttachment
import kotlinx.coroutines.flow.Flow

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
interface MemoryRepository : FileAttachmentRepository, MessageRepository, ConversationRepository {
}
