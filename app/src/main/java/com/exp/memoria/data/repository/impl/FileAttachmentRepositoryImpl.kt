package com.exp.memoria.data.repository.impl

import com.exp.memoria.data.local.dao.MessageFileDao
import com.exp.memoria.data.local.entity.MessageFile
import com.exp.memoria.data.repository.FileAttachmentRepository
import javax.inject.Inject

/**
 * [FileAttachmentRepository] 的实现，用于管理文件附件数据。
 *
 * @property messageFileDao 用于访问消息文件数据的 DAO。
 */
class FileAttachmentRepositoryImpl @Inject constructor(
    private val messageFileDao: MessageFileDao
) : FileAttachmentRepository {

    /**
     * 保存一个消息文件附件。
     *
     * @param file 要保存的 [MessageFile] 实体。
     * @return 插入的文件的 ID。
     */
    override suspend fun saveMessageFile(file: MessageFile): Long {
        return messageFileDao.insert(file)
    }

    /**
     * 获取特定记忆的所有消息文件附件。
     *
     * @param rawMemoryId 原始记忆的 ID。
     * @return 一个 [MessageFile] 实体列表。
     */
    override suspend fun getMessageFilesForMemory(rawMemoryId: Long): List<MessageFile> {
        return messageFileDao.getFilesForMemory(rawMemoryId)
    }

    /**
     * 删除一个消息文件附件。
     *
     * @param file 要删除的 [MessageFile] 实体。
     */
    override suspend fun deleteMessageFile(file: MessageFile) {
        messageFileDao.delete(file)
    }
}
