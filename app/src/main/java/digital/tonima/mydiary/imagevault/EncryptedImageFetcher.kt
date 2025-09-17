package digital.tonima.mydiary.imagevault

import android.content.Context
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import digital.tonima.mydiary.database.entities.VaultImageEntity
import digital.tonima.mydiary.encrypting.PasswordBasedCryptoManager
import okio.buffer
import okio.source
import java.io.File

/**
 * A custom Coil Fetcher that decrypts image files before displaying them.
 * This version handles a VaultImageEntity by extracting its file name.
 */
class EncryptedImageFetcher(
    private val context: Context,
    private val imageEntity: VaultImageEntity,
    private val masterPassword: CharArray,
    private val options: Options,
    private val cryptoManager: PasswordBasedCryptoManager
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        val file = File(context.filesDir, imageEntity.encryptedFileName)
        if (!file.exists()) return null

        val inputStream = cryptoManager.getDecryptedImageInputStream(file, masterPassword)
            ?: return null

        val bufferedSource = inputStream.source().buffer()
        val imageSource = ImageSource(source = bufferedSource, context = context)

        return SourceResult(
            source = imageSource,
            mimeType = null,
            dataSource = DataSource.DISK
        )
    }

    /**
     * A factory that creates EncryptedImageFetcher instances for VaultImageEntity objects.
     */
    class Factory(
        private val context: Context,
        private val masterPassword: CharArray,
        private val cryptoManager: PasswordBasedCryptoManager
    ) :
        Fetcher.Factory<VaultImageEntity> {
        override fun create(data: VaultImageEntity, options: Options, imageLoader: ImageLoader): Fetcher {
            return EncryptedImageFetcher(
                context,
                data,
                masterPassword,
                options,
                cryptoManager
            )
        }
    }
}
