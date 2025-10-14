package com.exp.memoria.data.local.dao

import androidx.room.Dao

/**
 * [浓缩记忆数据访问对象 (DAO)]
 *
 * 职责:
 * 1. 定义与 "CondensedMemory" 表 [cite: 59] 和 FTS 虚拟表 [cite: 69] 交互的所有数据库操作方法。
 * 2. 提供插入(Insert)一条新的浓缩记忆的方法。
 * 3. 提供根据状态(status)字段查询待处理记忆("NEW")的方法，供后台Worker使用 [cite: 67]。
 * 4. 提供更新(Update)一条记忆记录的方法，例如，在处理完成后，将其向量、状态等更新 [cite: 67]。
 * 5. 定义核心的检索方法：
 * - 一个使用FTS5进行全文搜索的方法，用于快速预过滤 [cite: 33]。
 * - 一个根据ID列表查询所有量化向量(vector_int8)的方法，用于向量精排 [cite: 33, 66]。
 *
 * 关联:
 * - Room DAO接口，使用 @Insert, @Query, @Update 等注解。
 * - 它的实例由 DatabaseModule 提供，并被注入到 MemoryRepository 中。
 */

@Dao
interface CondensedMemoryDao {
    // 稍后会添加函数
}