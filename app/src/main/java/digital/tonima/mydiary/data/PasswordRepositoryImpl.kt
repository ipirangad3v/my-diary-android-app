package digital.tonima.mydiary.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.core.content.edit
import com.paulrybitskyi.hiltbinder.BindType
import com.paulrybitskyi.hiltbinder.BindType.Component.VIEW_MODEL
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * A repository for managing the persistence of the encrypted master password.
 * This class abstracts the underlying storage mechanism (SharedPreferences).
 */
@BindType(installIn = VIEW_MODEL, to = PasswordRepository::class)
class PasswordRepositoryImpl @Inject constructor(@ApplicationContext context: Context) :
    PasswordRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object Companion {
        private const val PREFS_NAME = "diary_prefs"
        private const val PREF_KEY_ENCRYPTED_PASSWORD = "encrypted_password"
        private const val PREF_KEY_PASSWORD_IV = "password_iv"
    }

    /**
     * Saves the encrypted password and its IV to persistent storage.
     */
    override fun saveEncryptedPassword(encryptedPassword: EncryptedPassword) {
        prefs.edit {
            putString(
                PREF_KEY_ENCRYPTED_PASSWORD,
                Base64.encodeToString(encryptedPassword.value, Base64.DEFAULT)
            )
            putString(
                PREF_KEY_PASSWORD_IV,
                Base64.encodeToString(encryptedPassword.iv, Base64.DEFAULT)
            )
        }
    }

    /**
     * Retrieves the encrypted password and its IV from storage.
     * @return An [EncryptedPassword] object, or null if not found.
     */
    override fun getEncryptedPassword(): EncryptedPassword? {
        val encryptedPasswordB64 = prefs.getString(PREF_KEY_ENCRYPTED_PASSWORD, null)
        val ivB64 = prefs.getString(PREF_KEY_PASSWORD_IV, null)

        if (encryptedPasswordB64 == null || ivB64 == null) {
            return null
        }

        return try {
            val password = Base64.decode(encryptedPasswordB64, Base64.DEFAULT)
            val iv = Base64.decode(ivB64, Base64.DEFAULT)
            EncryptedPassword(password, iv)
        } catch (_: IllegalArgumentException) {
            Log.e("PasswordRepository", "Failed to decode base64 encoded password or IV")
            null
        }
    }

    /**
     * Checks if a master password has been set up and saved.
     * @return True if a password exists, false otherwise.
     */
    override fun hasPassword(): Boolean {
        return prefs.contains(PREF_KEY_ENCRYPTED_PASSWORD)
    }

    /**
     * Clears the stored encrypted password from persistent storage.
     */
    override fun clearPassword() {
        prefs.edit {
            remove(PREF_KEY_ENCRYPTED_PASSWORD)
            remove(PREF_KEY_PASSWORD_IV)
        }
    }

}
