package digital.tonima.meudiario.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object CryptoManager {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "diary_secret_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val ENTRY_FILE_PREFIX = "entry_"

    private const val AES_KEY_SIZE = 256
    private const val GCM_IV_SIZE = 12 // GCM recommended IV size is 12 bytes
    private const val GCM_TAG_LENGTH = 128 // GCM authentication tag length in bits

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        keyStore.getKey(KEY_ALIAS, null)?.let {
            return it as SecretKey
        }

        val params = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).apply {
            setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            setKeySize(AES_KEY_SIZE)
        }.build()

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(params)
        return keyGenerator.generateKey()
    }


    fun saveDiaryEntry(context: Context, fileName: String, content: String) {
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv

        val file = File(context.filesDir, fileName)
        FileOutputStream(file).use { fos ->
            fos.write(iv)
            val encryptedData = cipher.doFinal(content.toByteArray())
            fos.write(encryptedData)
        }
    }

    fun readDiaryEntry(context: Context, fileName: String): String {
        return try {
            val file = File(context.filesDir, fileName)
            FileInputStream(file).use { fis ->
                val iv = ByteArray(GCM_IV_SIZE)
                fis.read(iv)

                val encryptedData = fis.readBytes()

                val secretKey = getOrCreateSecretKey()
                val cipher = Cipher.getInstance(TRANSFORMATION)
                val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

                val decryptedData = cipher.doFinal(encryptedData)
                String(decryptedData)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun getAllEntryFiles(context: Context): List<File> {
        val dataDir = context.filesDir
        return dataDir.listFiles { _, name -> name.startsWith(ENTRY_FILE_PREFIX) }?.toList() ?: emptyList()
    }
}

