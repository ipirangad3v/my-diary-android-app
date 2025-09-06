package digital.tonima.mydiary.data

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object PasswordBasedCryptoManager {

    private const val KEY_ALGORITHM = "AES"
    private const val KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val CIPHER_ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_LENGTH_BITS = 256
    private const val ITERATION_COUNT = 100000
    private const val SALT_LENGTH_BYTES = 16
    private const val IV_LENGTH_BYTES = 12 // GCM recommended IV size
    private const val TAG_LENGTH_BITS = 128

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, ITERATION_COUNT, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, KEY_ALGORITHM)
    }

    fun saveDiaryEntry(context: Context, password: CharArray, filename: String, content: String) {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        SecureRandom().nextBytes(salt)

        val secretKey = deriveKey(password, salt)

        val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
        val iv = ByteArray(IV_LENGTH_BYTES)
        SecureRandom().nextBytes(iv)
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)

        val encryptedContent = cipher.doFinal(content.toByteArray())

        val entriesDir = File(context.filesDir, "entries")
        if (!entriesDir.exists()) entriesDir.mkdirs()

        FileOutputStream(File(entriesDir, filename)).use { fos ->
            fos.write(salt)
            fos.write(iv)
            fos.write(encryptedContent)
        }
    }

    fun readDiaryEntry(context: Context, password: CharArray, filename: String): String {
        val entriesDir = File(context.filesDir, "entries")
        val file = File(entriesDir, filename)

        FileInputStream(file).use { fis ->
            val salt = ByteArray(SALT_LENGTH_BYTES)
            fis.read(salt)

            val iv = ByteArray(IV_LENGTH_BYTES)
            fis.read(iv)

            val encryptedContent = fis.readBytes()

            val secretKey = deriveKey(password, salt)
            val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)

            val decryptedContent = cipher.doFinal(encryptedContent)
            return String(decryptedContent)
        }
    }

    fun verifyPassword(context: Context, password: CharArray, file: File): Boolean {
        return try {
            readDiaryEntry(context, password, file.name)
            // If readDiaryEntry doesn't throw an exception, the password is correct
            true
        } catch (e: Exception) {
            // Any cryptographic exception (like AEADBadTagException) indicates a wrong password
            Log.e("PasswordVerify", "Failed to verify password for file ${file.name}", e)
            false
        }
    }

    fun getAllEntryFiles(context: Context): List<File> {
        val entriesDir = File(context.filesDir, "entries")
        if (!entriesDir.exists()) return emptyList()
        return entriesDir.listFiles { _, name -> name.startsWith("entry_") }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
}

