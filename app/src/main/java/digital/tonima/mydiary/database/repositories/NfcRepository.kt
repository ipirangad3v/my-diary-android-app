package digital.tonima.mydiary.database.repositories

import digital.tonima.mydiary.encrypting.PasswordBasedCryptoManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NfcRepository
    @Inject
    constructor(
        private val cryptoManager: PasswordBasedCryptoManager,
    ) {

        /**
         * Encrypts a secret string for writing to an NFC tag.
         * @param secret The plain text secret to encrypt.
         * @param masterPassword The user's master password for key derivation.
         * @return A ByteArray containing the encrypted data, ready to be written to a tag, or null on failure.
         */
        fun encryptSecret(secret: String, masterPassword: CharArray): ByteArray? {
            return cryptoManager.encryptSecret(secret, masterPassword)
        }

        /**
         * Decrypts data read from an NFC tag.
         * @param data The raw byte data read from the tag.
         * @param masterPassword The user's master password for key derivation.
         * @return The decrypted secret as a String, or null if decryption fails.
         */
        fun decryptSecret(data: ByteArray, masterPassword: CharArray): String? {
            return cryptoManager.decryptSecret(data, masterPassword)
        }
    }
