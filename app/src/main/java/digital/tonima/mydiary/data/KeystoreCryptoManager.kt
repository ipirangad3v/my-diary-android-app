package digital.tonima.mydiary.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages cryptographic operations using the Android Keystore system.
 *
 * This object is responsible for creating, retrieving, and using a hardware-backed
 * cryptographic key to encrypt and decrypt sensitive data, such as a master password.
 * The key is configured to require user authentication (e.g., biometrics, PIN) for use,
 * ensuring that the encrypted data can only be accessed after the user has authenticated.
 */
object KeystoreCryptoManager {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "master_password_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private const val AES_KEY_SIZE = 256
    private const val GCM_TAG_LENGTH = 128

    /**
     * An instance of the Android Keystore, which provides access to the secure key storage facility.
     */
    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    /**
     * Retrieves the secret key from the Android Keystore or creates a new one if it doesn't exist.
     *
     * The key is generated with the following properties:
     * - Stored in the Android Keystore under the alias [KEY_ALIAS].
     * - Uses AES/GCM for encryption/decryption.
     * - Requires the user to authenticate (e.g., via fingerprint, PIN, or pattern) before it can be used.
     * The key material itself never leaves the secure hardware of the device.
     *
     * @return The [SecretKey] for cryptographic operations.
     */
    private fun getOrCreateSecretKey(): SecretKey {
        // Return the key if it already exists.
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        // If not, create specification for a new key.
        val params = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).apply {
            setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            setKeySize(AES_KEY_SIZE)
            // Require the user to authenticate to use this key.
            setUserAuthenticationRequired(true)
        }.build()

        // Generate and store the key in the Android Keystore.
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(params)
        return keyGenerator.generateKey()
    }

    /**
     * Gets a [Cipher] instance initialized for encryption.
     *
     * Using the `doFinal` method on the returned cipher will trigger a system prompt
     * for user authentication if required by the key's properties.
     * The caller is responsible for retrieving the Initialization Vector (IV) from
     * this cipher instance via `cipher.iv` after encryption and storing it securely.
     *
     * @return A [Cipher] object ready for encryption.
     */
    fun getEncryptCipher(): Cipher {
        val key = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher
    }

    /**
     * Gets a [Cipher] instance initialized for decryption with a specific Initialization Vector (IV).
     *
     * Using the `doFinal` method on the returned cipher will trigger a system prompt
     * for user authentication.
     *
     * @param iv The Initialization Vector that was used to encrypt the data.
     * @return A [Cipher] object ready for decryption.
     */
    fun getDecryptCipherForIv(iv: ByteArray): Cipher {
        val key = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher
    }
}
