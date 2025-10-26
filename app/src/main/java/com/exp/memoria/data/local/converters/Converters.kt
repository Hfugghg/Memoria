package com.exp.memoria.data.local.converters

import androidx.room.TypeConverter
import com.exp.memoria.data.Content
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Converters {
    @TypeConverter
    fun fromContentList(contents: List<Content>): String {
        return Json.encodeToString(contents)
    }

    @TypeConverter
    fun toContentList(contentsJson: String): List<Content> {
        return Json.decodeFromString(contentsJson)
    }

    @TypeConverter
    fun fromFloatList(vector: List<Float>?): ByteArray? {
        if (vector == null) return null
        val byteBuffer = ByteBuffer.allocate(vector.size * 4).order(ByteOrder.nativeOrder())
        for (value in vector) {
            byteBuffer.putFloat(value)
        }
        return byteBuffer.array()
    }

    @TypeConverter
    fun toFloatList(byteArray: ByteArray?): List<Float>? {
        if (byteArray == null) return null
        val byteBuffer = ByteBuffer.wrap(byteArray).order(ByteOrder.nativeOrder())
        val floatList = mutableListOf<Float>()
        while (byteBuffer.hasRemaining()) {
            floatList.add(byteBuffer.getFloat())
        }
        return floatList
    }
}
