package com.exp.memoria.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "condensed_memory",
    foreignKeys = [
        ForeignKey(
            entity = RawMemory::class,
            parentColumns = ["id"],
            childColumns = ["raw_memory_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CondensedMemory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, // [cite: 62]
@ColumnInfo(name = "raw_memory_id", index = true)
    val raw_memory_id: Int, // [cite: 63]
    val summary_text: String, // [cite: 64]
    val vector_int8: ByteArray?, // [cite: 66]
    val status: String, // [cite: 67]
    val timestamp: Long // [cite: 68]
) {
    // equals/hashCode needed for ByteArray comparison
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CondensedMemory

        if (id != other.id) return false
        if (raw_memory_id != other.raw_memory_id) return false
        if (summary_text != other.summary_text) return false
        if (vector_int8 != null) {
            if (other.vector_int8 == null) return false
            if (!vector_int8.contentEquals(other.vector_int8)) return false
        } else if (other.vector_int8 != null) return false
        if (status != other.status) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + raw_memory_id
        result = 31 * result + summary_text.hashCode()
        result = 31 * result + (vector_int8?.contentHashCode() ?: 0)
        result = 31 * result + status.hashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}