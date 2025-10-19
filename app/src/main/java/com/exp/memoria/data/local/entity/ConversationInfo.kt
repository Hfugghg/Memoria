package com.exp.memoria.data.local.entity

import androidx.room.ColumnInfo

/**
 * [对话信息数据类]
 *
 * 这是一个数据类，用于封装从数据库查询中获取的单个对话的摘要信息。
 * 它不直接映射到一个完整的数据库表，而是作为一个查询结果的容器。
 *
 * @property conversationId 对话的唯一标识符。
 * @property name 对话的名称。
 * @property lastTimestamp 对话中最后一条消息的时间戳，用于排序。
 */
data class ConversationInfo(
    val conversationId: String,
    val name: String,
    @ColumnInfo(name = "lastTimestamp")
    val lastTimestamp: Long
)
