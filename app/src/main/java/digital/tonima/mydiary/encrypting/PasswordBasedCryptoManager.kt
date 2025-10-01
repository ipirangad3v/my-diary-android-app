package digital.tonima.mydiary.encrypting

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import digital.tonima.mydiary.data.model.DiaryEntry
import java.io.File
import java.io.InputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages all cryptographic operations based on a user-provided master password.
 * This class is a Singleton managed by Hilt and can be injected into repositories.
 */
@Singleton
class PasswordBasedCryptoManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {

        private val TRANSFORMATION = "AES/GCM/NoPadding"
        private val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        private val AES_KEY_SIZE = 256
        private val GCM_TAG_LENGTH = 128
        private val SALT_SIZE = 16
        private val IV_SIZE = 12
        private val PBKDF2_ITERATIONS = 100000

        private fun deriveKey(password: CharArray, salt: ByteArray): SecretKey {
            val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, AES_KEY_SIZE)
            val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
            return factory.generateSecret(spec)
        }

        fun readDiaryEntry(file: File, masterPassword: CharArray): DiaryEntry? {
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

        fun verifyPassword(masterPassword: CharArray): Boolean {
            val firstEntry = getAllEntryFiles().firstOrNull() ?: return true
            return readDiaryEntry(firstEntry, masterPassword) != null
        }

        fun getAllEntryFiles(): List<File> {
            return context.filesDir.listFiles { _, name -> name.startsWith("entry_") }?.toList() ?: emptyList()
        }

        fun deleteEncryptedFile(fileToDelete: File) {
            if (fileToDelete.exists()) {
                fileToDelete.delete()
            }
        }

        fun deleteAllEntries() {
            getAllEntryFiles().forEach { file ->
                deleteEncryptedFile(file)
            }
        }

        fun saveEncryptedImage(imageUri: Uri, masterPassword: CharArray): File? {
            return try {
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
                outputFile
            } catch (e: Exception) {
                Log.e("CryptoManager", "Failed to save encrypted image", e)
                null
            }
        }

        fun getDecryptedImageInputStream(file: File, masterPassword: CharArray): InputStream? {
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

        fun decryptImageToFile(encryptedFile: File, tempFile: File, masterPassword: CharArray): Boolean {
            return try {
                getDecryptedImageInputStream(encryptedFile, masterPassword)?.use { inputStream ->
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

        fun encryptSecret(secret: String, masterPassword: CharArray): ByteArray? {
            return try {
                val salt = ByteArray(SALT_SIZE)
                SecureRandom().nextBytes(salt)
                val key = deriveKey(masterPassword, salt)
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.ENCRYPT_MODE, key)
                val iv = cipher.iv
                val encryptedContent = cipher.doFinal(secret.toByteArray())

                salt + iv + encryptedContent
            } catch (e: Exception) {
                Log.e("CryptoManager", "Failed to decrypt NFC data", e)
                null
            }
        }

        fun decryptSecret(data: ByteArray, masterPassword: CharArray): String? {
            return try {
                val salt = data.copyOfRange(0, SALT_SIZE)
                val iv = data.copyOfRange(SALT_SIZE, SALT_SIZE + IV_SIZE)
                val encryptedContent = data.copyOfRange(SALT_SIZE + IV_SIZE, data.size)

                val key = deriveKey(masterPassword, salt)
                val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, key, spec)
                val decryptedBytes = cipher.doFinal(encryptedContent)
                String(decryptedBytes)
            } catch (e: Exception) {
                Log.e("CryptoManager", "Failed to decrypt NFC data", e)
                null
            }
        }
    }
