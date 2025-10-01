package digital.tonima.mydiary.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diary_entries")
data class DiaryEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "encrypted_content_html", typeAffinity = ColumnInfo.BLOB)
    val encryptedContentHtml: ByteArray,
)
