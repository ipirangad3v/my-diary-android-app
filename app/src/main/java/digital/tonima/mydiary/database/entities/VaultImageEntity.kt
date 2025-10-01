package digital.tonima.mydiary.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_images")
data class VaultImageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "encrypted_file_name")
    val encryptedFileName: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
)
