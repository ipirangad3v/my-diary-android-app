package digital.tonima.mydiary.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import digital.tonima.mydiary.database.entities.VaultImageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_images ORDER BY timestamp DESC")
    fun getAllImages(): Flow<List<VaultImageEntity>>

    @Insert
    suspend fun insert(image: VaultImageEntity)

    @Delete
    suspend fun delete(image: VaultImageEntity)

    @Query("DELETE FROM vault_images")
    suspend fun deleteAll()
}
