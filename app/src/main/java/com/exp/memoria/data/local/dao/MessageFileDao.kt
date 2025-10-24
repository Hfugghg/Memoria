package com.exp.memoria.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.exp.memoria.data.local.entity.MessageFile

/**
 * [消息文件数据访问对象]
 *
 * 职责:
 * 1. 提供对 "message_files" 表的数据库访问方法。
 * 2. 封装了对文件数据的增、删、改、查等SQL操作。
 */
@Dao
interface MessageFileDao {

    /**
     * 插入一个或多个文件记录。
     * 如果已存在相同主键的记录，则替换它。
     * @param messageFile 要插入的文件实体。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(messageFile: MessageFile): Long

    /**
     * 删除一个文件记录。
     * @param messageFile 要删除的文件实体。
     */
    @Delete
    suspend fun delete(messageFile: MessageFile)

    /**
     * 根据消息ID查询关联的所有文件。
     * @param rawMemoryId 消息的ID。
     * @return 返回与该消息关联的文件列表。
     */
    @Query("SELECT * FROM message_files WHERE rawMemoryId = :rawMemoryId")
    suspend fun getFilesForMemory(rawMemoryId: Long): List<MessageFile>

    /**
     * 根据消息ID删除所有关联的文件。
     * @param rawMemoryId 消息的ID。
     */
    @Query("DELETE FROM message_files WHERE rawMemoryId = :rawMemoryId")
    suspend fun deleteFilesForMemory(rawMemoryId: Long)
}
