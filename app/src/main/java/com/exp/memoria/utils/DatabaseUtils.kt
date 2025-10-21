package com.exp.memoria.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

object DatabaseUtils {

    private const val TAG = "DatabaseUtils"
    // 修正数据库名称，与 DatabaseModule.kt 中定义的保持一致
    private const val DATABASE_NAME = "memoria_database"

    /**
     * 将Room数据库导出到公共的“下载”目录。
     *
     * **重要提示：**
     * 1. 此功能需要 `WRITE_EXTERNAL_STORAGE` 权限。请确保您已经在 AndroidManifest.xml 中声明了此权限，
     *    并在调用此函数之前，通过运行时权限请求获得了用户的授权。
     * 2. 对于 Android 10 (API 29) 及更高版本，推荐使用 MediaStore API 来访问公共存储，
     *    这是一种更现代、更安全的做法。
     *
     * @param context 应用程序上下文。
     * @return 如果数据库成功导出，则返回 true，否则返回 false。
     */
    fun exportDatabase(context: Context): Boolean {
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        if (!dbFile.exists()) {
            Log.e(TAG, "数据库文件未找到于: ${dbFile.absolutePath}")
            return false
        }

        val exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        // 在备份文件名中也使用正确的名称
        val backupFile = File(exportDir, "${DATABASE_NAME}_backup_${System.currentTimeMillis()}.db")

        try {
            FileInputStream(dbFile).channel.use { source ->
                FileOutputStream(backupFile).channel.use { destination ->
                    destination.transferFrom(source, 0, source.size())
                    Log.d(TAG, "数据库已成功导出到: ${backupFile.absolutePath}")
                    return true
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "导出数据库时出错", e)
            return false
        }
    }
}
