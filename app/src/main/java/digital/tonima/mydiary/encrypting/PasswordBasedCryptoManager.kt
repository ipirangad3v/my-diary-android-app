package digital.tonima.mydiary.encrypting

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import digital.tonima.mydiary.data.model.DiaryEntry
import java.io.File
import java.io.InputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages all cryptographic operations based on a user-provided master password.
 *
 * This object is responsible for deriving a strong encryption key from a password
 * and using it to encrypt/decrypt diary entries and images. It employs PBKDF2 for
 * key derivation and AES/GCM for encryption, ensuring robust security that is
 * portable across devices (as it's not tied to hardware keys).
 */
object PasswordBasedCryptoManager {

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val AES_KEY_SIZE = 256
    private const val GCM_TAG_LENGTH = 128
    private const val SALT_SIZE = 16
    private const val IV_SIZE = 12
    private const val PBKDF2_ITERATIONS = 100000

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, AES_KEY_SIZE)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        return factory.generateSecret(spec)
    }

    fun saveDiaryEntry(
        context: Context,
        entry: DiaryEntry,
        masterPassword: CharArray,
        fileName: String? = null // <-- Parâmetro adicionado para edição
    ) {
        val salt = ByteArray(SALT_SIZE)
        SecureRandom().nextBytes(salt)

        val key = deriveKey(masterPassword, salt)
        val cipher = Cipher.getInstance(TRANSFORMATION)

        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv

        val gson = Gson()
        val jsonString = gson.toJson(entry)
        val encryptedContent = cipher.doFinal(jsonString.toByteArray())

        // Lógica atualizada para criar um novo ficheiro ou sobrescrever um existente
        val file = if (fileName != null) {
            // Se um nome de ficheiro for fornecido, use-o (sobrescrevendo o existente)
            File(context.filesDir, fileName)
        } else {
            // Caso contrário, crie um novo ficheiro com um novo timestamp
            val timestamp = System.currentTimeMillis()
            File(context.filesDir, "entry_$timestamp.txt")
        }

        file.outputStream().use {
            it.write(salt)
            it.write(iv)
            it.write(encryptedContent)
        }
    }

    fun readDiaryEntry(context: Context, file: File, masterPassword: CharArray): DiaryEntry? {
        return try {
            file.inputStream().use {
                val salt = ByteArray(SALT_SIZE)
                it.read(salt)
                val iv = ByteArray(IV_SIZE)
                it.read(iv)
                val encryptedContent = it.readBytes()
                val key = deriveKey(masterPassword, salt)
                val cipher = Cipher.getInstance(TRANSFORMATION)
                val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                cipher.init(Cipher.DECRYPT_MODE, key, spec)
                val decryptedBytes = cipher.doFinal(encryptedContent)
                val decryptedJson = String(decryptedBytes)
                val gson = Gson()
                gson.fromJson(decryptedJson, DiaryEntry::class.java)
            }
        } catch (e: Exception) {
            Log.e("CryptoManager", "Failed to decrypt or parse diary entry", e)
            null
        }
    }

    fun verifyPassword(context: Context, masterPassword: CharArray): Boolean {
        val firstEntry = getAllEntryFiles(context).firstOrNull() ?: return true
        return readDiaryEntry(context, firstEntry, masterPassword) != null
    }

    fun getAllEntryFiles(context: Context): List<File> {
        return context.filesDir.listFiles { _, name -> name.startsWith("entry_") }?.toList() ?: emptyList()
    }

    fun deleteEncryptedFile(fileToDelete: File) {
        if (fileToDelete.exists()) {
            fileToDelete.delete()
        }
    }

    fun deleteAllEntries(context: Context) {
        getAllEntryFiles(context).forEach { file ->
            deleteEncryptedFile(file)
        }
    }

    // --- Image Vault Functions ---

    fun saveEncryptedImage(context: Context, imageUri: Uri, masterPassword: CharArray) {
        val salt = ByteArray(SALT_SIZE)
        SecureRandom().nextBytes(salt)

        val key = deriveKey(masterPassword, salt)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv

        val timestamp = System.currentTimeMillis()
        val outputFile = File(context.filesDir, "vault_$timestamp.enc")

        context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
            outputFile.outputStream().use { fileOutputStream ->
                fileOutputStream.write(salt)
                fileOutputStream.write(iv)
                CipherOutputStream(fileOutputStream, cipher).use { cipherOutputStream ->
                    inputStream.copyTo(cipherOutputStream)
                }
            }
        }
    }

    fun getDecryptedImageInputStream(context: Context, file: File, masterPassword: CharArray): InputStream? {
        return try {
            val fileInputStream = file.inputStream()
            val salt = ByteArray(SALT_SIZE)
            fileInputStream.read(salt)

            val iv = ByteArray(IV_SIZE)
            fileInputStream.read(iv)

            val key = deriveKey(masterPassword, salt)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            CipherInputStream(fileInputStream, cipher)
        } catch (e: Exception) {
            Log.e("CryptoManager", "Failed to decrypt image", e)
            null
        }
    }

    fun decryptImageToFile(context: Context, encryptedFile: File, tempFile: File, masterPassword: CharArray): Boolean {
        return try {
            getDecryptedImageInputStream(context, encryptedFile, masterPassword)?.use { inputStream ->
                tempFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            true
        } catch (e: Exception) {
            Log.e("CryptoManager", "Failed to decrypt image to file", e)
            false
        }
    }

    fun getAllVaultFiles(context: Context): List<File> {
        return context.filesDir.listFiles { _, name -> name.startsWith("vault_") }?.toList() ?: emptyList()
    }
}
