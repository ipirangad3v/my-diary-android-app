package digital.tonima.mydiary.encrypting

/**
 * A repository for managing the persistence of the encrypted master password.
 * This class abstracts the underlying storage mechanism (SharedPreferences).
 */
interface PasswordRepository {
    /**
     * Saves the encrypted password and its IV to persistent storage.
     */
    fun saveEncryptedPassword(encryptedPassword: EncryptedPassword)

    /**
     * Retrieves the encrypted password and its IV from storage.
     * @return An [EncryptedPassword] object, or null if not found.
     */
    fun getEncryptedPassword(): EncryptedPassword?

    /**
     * Checks if a master password has been set up and saved.
     * @return True if a password exists, false otherwise.
     */
    fun hasPassword(): Boolean

    /**
     * Clears the stored encrypted password from persistent storage.
     */
    fun clearPassword()

}
