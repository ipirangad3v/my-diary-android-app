package digital.tonima.mydiary.data

import android.content.Context
import android.security.keystore.KeyProperties
import android.util.Log
import com.google.gson.Gson
import digital.tonima.mydiary.data.model.DiaryEntry
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * A singleton object for managing password-based cryptographic operations for diary entries.
 *
 * It uses AES/GCM for authenticated encryption and PBKDF2 to derive a secure key
 * from a user-provided master password. Each entry file contains its own salt,
 * initialization vector (IV), and the encrypted content.
 */
object PasswordBasedCryptoManager {

    // Constants for the encryption cipher configuration.
    private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
    private const val PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
    private const val TRANSFORMATION = "$ALGORITHM/$BLOCK_MODE/$PADDING"

    // Constants for key derivation using PBKDF2.
    private const val PBKDF2_ITERATION_COUNT = 100000 // Iteration count to strengthen the key.
    private const val PBKDF2_KEY_LENGTH = 256       // Key length in bits.
    private const val SALT_SIZE = 16                // Salt size in bytes for PBKDF2.
    private const val IV_SIZE = 12                  // Initialization Vector size in bytes for AES/GCM.
    private const val GCM_TAG_LENGTH = 128        // Authentication tag length in bits for AES/GCM.

    /**
     * Derives a secret encryption key from a password and a salt using PBKDF2.
     *
     * PBKDF2 (Password-Based Key Derivation Function 2) is used to make brute-force attacks
     * more difficult by adding a computational cost to the key generation.
     *
     * @param password The password from which the key will be derived.
     * @param salt A random value to ensure unique keys are generated even for identical passwords.
     * @return The derived [SecretKey] for use with the AES algorithm.
     */
    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATION_COUNT, PBKDF2_KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return SecretKeySpec(factory.generateSecret(spec).encoded, ALGORITHM)
    }

    /**
     * Encrypts and saves the content of a diary entry to a file.
     *
     * The resulting file is structured as follows: `[salt][iv][encrypted_content]`.
     * A new salt and IV are generated for each save operation.
     *
     * @param context The application context.
     * @param filename The name of the file where the entry will be saved.
     * @param content The plaintext content of the diary entry.
     * @param masterPassword The master password used to derive the encryption key.
     */
    fun saveDiaryEntry(context: Context, entry: DiaryEntry, masterPassword: CharArray) {
        try {
            val salt = ByteArray(SALT_SIZE)
            SecureRandom().nextBytes(salt)

            val key = deriveKey(masterPassword, salt)
            val cipher = Cipher.getInstance(TRANSFORMATION)

            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv

            val gson = Gson()
            val jsonString = gson.toJson(entry)
            val encryptedContent = cipher.doFinal(jsonString.toByteArray())

            val timestamp = System.currentTimeMillis()
            val file = File(context.filesDir, "entry_$timestamp.txt")

            file.outputStream().use {
                it.write(salt)
                it.write(iv)
                it.write(encryptedContent)
            }
        } catch (e: Exception) {
            Log.e("CryptoManager", "Error saving diary entry", e)
        }
    }

    /**
     * Reads and decrypts the content of a diary entry from a file.
     *
     * @param context The application context.
     * @param filename The name of the file to be read.
     * @param masterPassword The master password required to decrypt the content.
     * @return The decrypted content as a [String], or `null` if decryption fails
     * (e.g., incorrect password or corrupted file).
     */
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
            Log.e("CryptoManager", "Error reading diary entry", e)
            null
        }
    }

    /**
     * Gets a list of all diary entry files.
     *
     * Files are identified by the "entry_" prefix and sorted by modification date,
     * from newest to oldest.
     *
     * @param context The application context.
     * @return A [List] of [File] objects representing the diary entries, or an empty list if none are found.
     */
    fun getAllEntryFiles(context: Context): List<File> {
        return context.filesDir.listFiles { _, name -> name.startsWith("entry_") }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * Verifies if the provided master password is correct.
     *
     * The check is performed by attempting to decrypt the most recent diary entry.
     * If there are no entries, the check is considered successful (first-use scenario).
     *
     * @param context The application context.
     * @param masterPassword The master password to be verified.
     * @return `true` if the password is correct or if no entries exist; `false` otherwise.
     */
    fun verifyPassword(context: Context, masterPassword: CharArray): Boolean {
        // Get the latest entry. If none exist, the password is considered valid (first use).
        val firstEntry = getAllEntryFiles(context).firstOrNull() ?: return true
        // Try to read the entry. If it returns non-null, the password was correct.
        return readDiaryEntry(firstEntry, masterPassword) != null
    }

    /**
     * Deletes a specific diary entry file.
     *
     * @param context The application context.
     * @param filename The name of the file to be deleted.
     */
    fun deleteDiaryEntry(filename: File) {
        try {
            if (filename.exists()) {
                filename.delete()
            }
        } catch (e: Exception) {
            Log.e("CryptoManager", "Failed to delete entry: $filename", e)
        }
    }

    fun deleteAllEntries(context: Context) {
        getAllEntryFiles(context).forEach { file ->
            deleteDiaryEntry(file)
        }
    }
}
