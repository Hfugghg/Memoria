@file:OptIn(InternalSerializationApi::class)
package com.exp.memoria.ui.settings

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

/**
 * JSON Schema 属性类型枚举
 * 定义了图形化 Response Schema 编辑器中支持的 JSON 数据类型。
 */
@Serializable
enum class JsonSchemaPropertyType(val displayName: String, val description: String) {
    STRING("字符串", "表示文本数据，如姓名、描述。"),
    NUMBER("数字", "表示数值数据，可以是整数或浮点数。"),
    BOOLEAN("布尔值", "表示真或假。"),
    OBJECT("对象", "表示一个键值对集合，可以包含嵌套属性。"), // 简化处理，暂时不深入编辑嵌套结构
    ARRAY("数组", "表示一个有序的同类型元素集合。") // 简化处理，暂时不深入编辑数组元素类型
}

/**
 * 字符串类型格式枚举
 * 定义了字符串类型可以拥有的特定格式。
 */
@Serializable
enum class StringFormat(val displayName: String, val description: String) {
    NONE("无", "无特定格式。"),
    DATE("日期", "形如 'YYYY-MM-DD' 的日期字符串。"),
    DATETIME("日期时间", "形如 'YYYY-MM-DDTHH:MM:SSZ' 的 ISO 8601 日期时间字符串。"),
    EMAIL("电子邮件", "标准的电子邮件地址格式。"),
    URI("URI", "统一资源标识符。"),
    UUID("UUID", "通用唯一标识符。")
}

/**
 * JSON Schema 属性数据类
 * 用于在图形化界面中表示单个 JSON 属性的结构。
 *
 * @property id 唯一标识符，用于在列表中区分不同的属性。
 * @property name 属性的名称，必须是英文。
 * @property type 属性的 JSON Schema 类型。
 * @property description 属性的描述。
 * @property stringFormat 如果类型是 STRING，则为可选的字符串格式。
 * @property numberMinimum 如果类型是 NUMBER，则为可选的最小值。
 * @property numberMaximum 如果类型是 NUMBER，则为可选的最大值。
 * @property required 表示该属性是否为必需的。
 */
@Serializable
data class JsonSchemaProperty(
    val id: Long = System.currentTimeMillis(), // 用于在 Compose LazyColumn 中提供唯一 key
    val name: String = "",
    val type: JsonSchemaPropertyType = JsonSchemaPropertyType.STRING,
    val description: String = "",
    // STRING 类型的特定属性
    val stringFormat: StringFormat = StringFormat.NONE,
    // NUMBER 类型的特定属性
    val numberMinimum: Double? = null,
    val numberMaximum: Double? = null,
    val required: Boolean = false
    // OBJECT/ARRAY 类型的简化处理，暂时不提供嵌套编辑
)

/**
 * 系统指令的数据模型
 * 包含一个或多个指令部分（Part）。
 */
@Serializable
data class SystemInstruction(
    val parts: List<Part>
)

/**
 * 单个指令部分
 * 包含具体的文本内容。
 */
@Serializable
data class Part(
    val text: String
)
