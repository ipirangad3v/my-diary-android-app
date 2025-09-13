package digital.tonima.mydiary.imagevault

import android.content.Context
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import digital.tonima.mydiary.encrypting.PasswordBasedCryptoManager
import okio.buffer
import okio.source
import java.io.File

/**
 * A custom Coil Fetcher that decrypts image files before displaying them.
 */
class EncryptedImageFetcher(
    private val context: Context,
    private val file: File,
    private val masterPassword: CharArray,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val inputStream = PasswordBasedCryptoManager.getDecryptedImageInputStream(context,file, masterPassword)
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
     * A factory that creates EncryptedImageFetcher instances for encrypted files.
     */
    class Factory(private val context: Context, private val masterPassword: CharArray) : Fetcher.Factory<File> {
        override fun create(data: File, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (!data.name.startsWith("vault_")) return null
            return EncryptedImageFetcher(context, data, masterPassword, options)
        }
    }
}
