package com.exp.memoria.data.model

/**
 * 用于临时存储转换后的文件数据
 * @param base64Data Base64编码的文件内容
 * @param fileName 文件名
 * @param fileType 文件MIME类型
 */
data class FileAttachment(
    val base64Data: String,
    val fileName: String,
    val fileType: String?
)
