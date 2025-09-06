package digital.tonima.mydiary.data

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object PasswordBasedCryptoManager {

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val ENTRY_FILE_PREFIX = "entry_"
    private const val KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val KEY_ALGORITHM = "AES"

    private const val AES_KEY_SIZE = 256
    private const val GCM_IV_SIZE = 12 // bytes
    private const val GCM_TAG_LENGTH = 128 // bits

    private const val PBKDF2_ITERATIONS = 100000
    private const val SALT_SIZE = 16 // bytes

    private fun deriveKeyFromPassword(password: CharArray, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, AES_KEY_SIZE)
        val secret = factory.generateSecret(spec)
        return SecretKeySpec(secret.encoded, KEY_ALGORITHM)
    }

    fun saveDiaryEntry(context: Context, fileName: String, content: String, password: CharArray) {
        val salt = ByteArray(SALT_SIZE)
        SecureRandom().nextBytes(salt)

        val secretKey = deriveKeyFromPassword(password, salt)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(content.toByteArray())

        val file = File(context.filesDir, fileName)
        FileOutputStream(file).use { fos ->
            fos.write(salt)
            fos.write(iv)
            fos.write(encryptedData)
        }
    }

    fun readDiaryEntry(context: Context, fileName: String, password: CharArray): String {
        return try {
            val file = File(context.filesDir, fileName)
            FileInputStream(file).use { fis ->
                // 1. Ler o salt e o iv do início do arquivo
                val salt = ByteArray(SALT_SIZE)
                fis.read(salt)

                val iv = ByteArray(GCM_IV_SIZE)
                fis.read(iv)

                val encryptedData = fis.readBytes()

                val secretKey = deriveKeyFromPassword(password, salt)

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
