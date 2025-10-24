package com.exp.memoria.data.repository

import com.exp.memoria.data.local.entity.MessageFile

interface FileAttachmentRepository {
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
     * 删除一个与消息关联的文件。
     *
     * @param file 要删除的MessageFile对象。
     */
    suspend fun deleteMessageFile(file: MessageFile)
}
