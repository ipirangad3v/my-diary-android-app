package digital.tonima.mydiary.database.repositories

import digital.tonima.mydiary.data.model.DiaryEntry
import digital.tonima.mydiary.database.daos.DiaryDao
import digital.tonima.mydiary.database.entities.DiaryEntryEntity
import digital.tonima.mydiary.di.DatabaseProvider
import digital.tonima.mydiary.encrypting.PasswordBasedCryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiaryRepository @Inject constructor(
    private val dbProvider: DatabaseProvider,
    private val cryptoManager: PasswordBasedCryptoManager
) {
    private fun getDao(passphrase: CharArray): DiaryDao {
        return dbProvider.getDatabase(passphrase).diaryDao()
    }

    fun getEntries(masterPassword: CharArray): Flow<Map<DiaryEntryEntity, DiaryEntry>> {
        return getDao(masterPassword).getAllEntries().map { entities ->
            entities.mapNotNull { entity ->
                cryptoManager.decryptSecret(entity.encryptedContentHtml, masterPassword)
                    ?.let { decryptedContent ->
                        val diaryEntry = DiaryEntry(title = entity.title, contentHtml = decryptedContent)
                        entity to diaryEntry
                    }
            }.toMap()
        }}

    suspend fun getEntryById(id: Long, masterPassword: CharArray): Pair<DiaryEntryEntity, DiaryEntry>? {
        val entity = getDao(masterPassword).getEntryById(id) ?: return null
        val decryptedContent = cryptoManager.decryptSecret(entity.encryptedContentHtml, masterPassword)
            ?: return null
        return entity to DiaryEntry(title = entity.title, contentHtml = decryptedContent)
    }


    suspend fun addOrUpdateEntry(entry: DiaryEntry, masterPassword: CharArray, entryId: Long?) {
        val dao = getDao(masterPassword)
        val encryptedContent = cryptoManager.encryptSecret(entry.contentHtml, masterPassword)
            ?: return

        if (entryId != null) {
            val originalEntity = dao.getEntryById(entryId)
            if (originalEntity != null) {
                val updatedEntity = originalEntity.copy(
                    title = entry.title,
                    encryptedContentHtml = encryptedContent
                )
                dao.update(updatedEntity)
            }
        } else {
            val newEntity = DiaryEntryEntity(
                timestamp = System.currentTimeMillis(),
                title = entry.title,
                encryptedContentHtml = encryptedContent
            )
            dao.insert(newEntity)
        }
    }
    suspend fun deleteEntry(entryId: Long, masterPassword: CharArray) {
        getDao(masterPassword).deleteById(entryId)
    }
    suspend fun deleteAllEntries(masterPassword: CharArray) {
        getDao(masterPassword).deleteAll()
    }
}
