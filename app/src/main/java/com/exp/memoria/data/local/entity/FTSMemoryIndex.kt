package com.exp.memoria.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * [FTS虚拟表示体类]
 *
 * 职责:
 * 1. 定义一个与 condensed_memory_fts 虚拟表绑定的数据类。
 * 2. 这个表专门用于对 `summary_text` 字段进行高效的全文搜索。
 * 3. FTS5 虚拟表现在通过数据库回调中的原生 SQL 创建，而不是使用 `@Fts5` 注解。
 *
 * 关联:
 * - 这是一个用于接收 FTS 查询结果的特殊 Room 实体。
 * - 它不代表一个由 Room 直接管理的物理表。
 * - CondensedMemoryDao 中的 `searchFtsIndex` 方法将直接查询这个虚拟表。
 */

// 以上注释存在问题，该项目可能无法使用FTS5注解
@Entity(tableName = "condensed_memory_fts")
// 修复KSP构建错误：FTS虚拟表实体必须包含一个名为'rowid'并作为主键的字段，用于与内容表的主键进行映射。
data class FTSMemoryIndex(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowid: Long,

    val summary_text: String
)
