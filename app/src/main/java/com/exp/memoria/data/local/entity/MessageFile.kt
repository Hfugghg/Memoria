package com.exp.memoria.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * [消息文件实体类]
 *
 * 职责:
 * 1. 定义 "message_files" 数据库表的结构。
 * 2. 每个实例代表一个与消息关联的文件或图片，以Base64形式存储。
 *
 * 字段:
 * - id: 主键，唯一标识一个文件记录。
 * - rawMemoryId: 外键，关联到 'raw_memory' 表的 'id'，表示该文件属于哪条消息。
 * - fileName: 文件名。
 * - fileType: 文件的MIME类型。
 * - fileContentBase64: 文件的Base64编码内容。
 */
@Entity(
    tableName = "message_files",
    foreignKeys = [
        ForeignKey(
            entity = RawMemory::class,
            parentColumns = ["id"],
            childColumns = ["rawMemoryId"],
            onDelete = ForeignKey.CASCADE // 当关联的 RawMemory 被删除时，相应的文件记录也一并删除
        )
    ],
    indices = [Index(value = ["rawMemoryId"])] // 为 rawMemoryId 添加索引以提高查询效率
)
data class MessageFile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rawMemoryId: Long,
    val fileName: String,
    val fileType: String,
    val fileContentBase64: String
)
