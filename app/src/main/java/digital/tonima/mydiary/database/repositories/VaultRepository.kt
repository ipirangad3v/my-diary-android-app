package digital.tonima.mydiary.database.repositories

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import digital.tonima.mydiary.BuildConfig
import digital.tonima.mydiary.database.daos.VaultDao
import digital.tonima.mydiary.database.entities.VaultImageEntity
import digital.tonima.mydiary.di.DatabaseProvider
import digital.tonima.mydiary.encrypting.PasswordBasedCryptoManager
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoManager: PasswordBasedCryptoManager,
    private val dbProvider: DatabaseProvider
) {
    private fun getDao(passphrase: CharArray): VaultDao {
        return dbProvider.getDatabase(passphrase).vaultDao()
    }

    fun getImages(masterPassword: CharArray): Flow<List<VaultImageEntity>> {
        return getDao(masterPassword).getAllImages()
    }

    suspend fun saveImage(uri: Uri, masterPassword: CharArray) {
        val encryptedFile = cryptoManager.saveEncryptedImage( uri, masterPassword)
        if (encryptedFile != null) {
            val newImage = VaultImageEntity(
                encryptedFileName = encryptedFile.name,
                timestamp = encryptedFile.lastModified()
            )
            getDao(masterPassword).insert(newImage)
        }
    }


    suspend fun addImage(fileName: String, masterPassword: CharArray) {
        val newImage = VaultImageEntity(
            encryptedFileName = fileName,
            timestamp = System.currentTimeMillis()
        )
        getDao(masterPassword).insert(newImage)
    }

    suspend fun deleteImage(imageEntity: VaultImageEntity, masterPassword: CharArray) {
        getDao(masterPassword).delete(imageEntity)
        val fileToDelete = File(context.filesDir, imageEntity.encryptedFileName)
        if (fileToDelete.exists()) {
            cryptoManager.deleteEncryptedFile(fileToDelete)
        }
    }

    suspend fun prepareImageForSharing(imageEntity: VaultImageEntity, masterPassword: CharArray): Uri? {
        val encryptedFile = File(context.filesDir, imageEntity.encryptedFileName)
        if (!encryptedFile.exists()) return null

        val cacheDir = File(context.cacheDir, "images")
        cacheDir.mkdirs()
        val tempFile = File.createTempFile("shared_", ".jpg", cacheDir)

        val success = cryptoManager.decryptImageToFile(encryptedFile, tempFile, masterPassword)

        return if (success) {
            FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", tempFile)
        } else {
            null
        }
    }

    suspend fun deleteAllImages(masterPassword: CharArray) {
        getDao(masterPassword).deleteAll()
    }
}
