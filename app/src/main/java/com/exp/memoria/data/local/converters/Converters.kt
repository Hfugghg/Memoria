package com.exp.memoria.data.local.converters

import androidx.room.TypeConverter
import com.exp.memoria.data.Content
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromContentList(contents: List<Content>): String {
        return Json.encodeToString(contents)
    }

    @TypeConverter
    fun toContentList(contentsJson: String): List<Content> {
        return Json.decodeFromString(contentsJson)
    }
}
