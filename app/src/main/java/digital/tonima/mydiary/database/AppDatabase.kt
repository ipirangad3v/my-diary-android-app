package digital.tonima.mydiary.database

import androidx.room.Database
import androidx.room.RoomDatabase
import digital.tonima.mydiary.database.daos.DiaryDao
import digital.tonima.mydiary.database.daos.VaultDao
import digital.tonima.mydiary.database.entities.DiaryEntryEntity
import digital.tonima.mydiary.database.entities.VaultImageEntity

@Database(
    entities = [DiaryEntryEntity::class, VaultImageEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun diaryDao(): DiaryDao
    abstract fun vaultDao(): VaultDao
}
