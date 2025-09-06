package digital.tonima.mydiary.data

import android.content.Context
import android.security.keystore.KeyProperties
import android.util.Log
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

    private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
    private const val PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
    private const val TRANSFORMATION = "$ALGORITHM/$BLOCK_MODE/$PADDING"

    private const val PBKDF2_ITERATION_COUNT = 100000
    private const val PBKDF2_KEY_LENGTH = 256
    private const val SALT_SIZE = 16
    private const val IV_SIZE = 12

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATION_COUNT, PBKDF2_KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return SecretKeySpec(factory.generateSecret(spec).encoded, ALGORITHM)
    }

    fun saveDiaryEntry(context: Context, filename: String, content: String, masterPassword: CharArray) {
        try {
            val salt = ByteArray(SALT_SIZE)
            SecureRandom().nextBytes(salt)

            val key = deriveKey(masterPassword, salt)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)

            val iv = cipher.iv
            val encryptedContent = cipher.doFinal(content.toByteArray())

            val file = File(context.filesDir, filename)
            FileOutputStream(file).use { fos ->
                fos.write(salt)
                fos.write(iv)
                fos.write(encryptedContent)
            }
        } catch (e: Exception) {
            Log.e("CryptoManager", "Error saving diary entry", e)
        }
    }

    fun readDiaryEntry(context: Context, filename: String, masterPassword: CharArray): String? {
        return try {
            val file = File(context.filesDir, filename)
            FileInputStream(file).use { fis ->
                val salt = ByteArray(SALT_SIZE)
                fis.read(salt)

                val iv = ByteArray(IV_SIZE)
                fis.read(iv)

                val encryptedContent = fis.readBytes()

                val key = deriveKey(masterPassword, salt)
                val cipher = Cipher.getInstance(TRANSFORMATION)
                val spec = GCMParameterSpec(128, iv)
                cipher.init(Cipher.DECRYPT_MODE, key, spec)

                val decryptedContent = cipher.doFinal(encryptedContent)
                String(decryptedContent)
            }
        } catch (e: Exception) {
            Log.e("CryptoManager", "Error reading diary entry", e)
            null
        }
    }

    fun getAllEntryFiles(context: Context): List<File> {
        return context.filesDir.listFiles { _, name -> name.startsWith("entry_") }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun verifyPassword(context: Context, masterPassword: CharArray): Boolean {
        val firstEntry = getAllEntryFiles(context).firstOrNull() ?: return true
        return readDiaryEntry(context, firstEntry.name, masterPassword) != null
    }

    fun deleteDiaryEntry(context: Context, filename: String) {
        try {
            val file = File(context.filesDir, filename)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e("CryptoManager", "Failed to delete entry: $filename", e)
        }
    }
}

