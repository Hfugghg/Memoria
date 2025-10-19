package com.exp.memoria.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * [原始记忆实体类]
 *
 * 职责:
 * 1. 定义 "raw_memory" 数据库表的结构。
 * 2. 每个实例代表对话中的一条消息。
 *
 * 字段:
 * - id: 主键，唯一标识一条消息记录。
 * - conversationId: 用于将属于同一个对话的消息分组。
 * - sender: 消息发送方 ("user" or "model")。
 * - text: 消息内容。
 * - timestamp: 消息发送的时间戳。
 */

@Entity(
    tableName = "raw_memory",
    indices = [Index(value = ["conversationId"])] // 为 conversationId 添加索引以提高查询效率
)
data class RawMemory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: String,
    val sender: String,
    val text: String,
    val timestamp: Long
)
