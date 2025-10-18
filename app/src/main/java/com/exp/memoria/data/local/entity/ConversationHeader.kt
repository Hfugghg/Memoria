package com.exp.memoria.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversation_header")
data class ConversationHeader(
    @PrimaryKey
    val conversationId: String,
    val name: String = "新对话", // 新增的 name 属性，带默认值
    val creationTimestamp: Long,
    var lastUpdateTimestamp: Long
)
