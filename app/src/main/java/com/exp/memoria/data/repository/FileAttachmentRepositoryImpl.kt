package com.exp.memoria.data.repository

import com.exp.memoria.data.local.dao.MessageFileDao
import com.exp.memoria.data.local.entity.MessageFile
import javax.inject.Inject

class FileAttachmentRepositoryImpl @Inject constructor(
    private val messageFileDao: MessageFileDao
) : FileAttachmentRepository {

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
