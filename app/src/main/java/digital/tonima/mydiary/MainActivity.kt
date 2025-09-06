package digital.tonima.mydiary

import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import digital.tonima.mydiary.data.KeystoreCryptoManager
import digital.tonima.mydiary.data.PasswordBasedCryptoManager
import digital.tonima.mydiary.ui.screens.*
import digital.tonima.mydiary.ui.theme.MyDiaryTheme
import javax.crypto.Cipher
import digital.tonima.mydiary.R.string.incorrect_password_try_again
import androidx.core.content.edit

class MainActivity : FragmentActivity() {

    private val PREFS_NAME = "diary_prefs"
    private val PREF_KEY_ENCRYPTED_PASSWORD = "encrypted_password"
    private val PREF_KEY_PASSWORD_IV = "password_iv"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        setContent {
            MyDiaryTheme {
                var currentScreen by remember {
                    val needsSetup = !prefs.contains(PREF_KEY_ENCRYPTED_PASSWORD)
                    mutableStateOf(if (needsSetup) AppScreen.SetupPassword else AppScreen.Locked)
                }

                when (val screen = currentScreen) {
                    is AppScreen.SetupPassword -> {
                        PasswordSetupScreen { password ->
                            showBiometricPromptForEncryption { cipher ->
                                val encryptedPassword = cipher.doFinal(password.concatToString().toByteArray())
                                val iv = cipher.iv
                                saveEncryptedPassword(encryptedPassword, iv)
                                currentScreen = AppScreen.Main(password)
                            }
                        }
                    }
                    is AppScreen.Locked -> {
                        LockedScreen(onUnlockRequest = {
                            showBiometricPromptForDecryption(
                                onSuccess = { decryptedPassword ->
                                    currentScreen = AppScreen.Main(decryptedPassword)
                                },
                                onFailure = {
                                    currentScreen = AppScreen.RecoverPassword
                                }
                            )
                        })
                    }
                    is AppScreen.RecoverPassword -> {
                        var error by remember { mutableStateOf<String?>(null) }
                        ManualPasswordScreen(
                            error = error,
                            onPasswordSubmit = { passwordAttempt ->
                                val firstEntry = PasswordBasedCryptoManager.getAllEntryFiles(this).firstOrNull()
                                if (firstEntry == null) {
                                    error = getString(R.string.no_entries_for_verification)
                                    return@ManualPasswordScreen
                                }

                                val isPasswordCorrect = PasswordBasedCryptoManager.verifyPassword(this, passwordAttempt, firstEntry)

                                if (isPasswordCorrect) {
                                    Toast.makeText(this, getString(R.string.password_verified_re_encrypt), Toast.LENGTH_LONG).show()
                                    showBiometricPromptForEncryption { cipher ->
                                        val encryptedPassword = cipher.doFinal(passwordAttempt.concatToString().toByteArray())
                                        val iv = cipher.iv
                                        saveEncryptedPassword(encryptedPassword, iv)
                                        currentScreen = AppScreen.Main(passwordAttempt)
                                    }
                                } else {
                                    error = getString(incorrect_password_try_again)
                                }
                            }
                        )
                    }
                    is AppScreen.Main -> {
                        MainScreen(
                            masterPassword = screen.masterPassword,
                            onLockRequest = { currentScreen = AppScreen.Locked },
                            onAddEntry = { currentScreen = AppScreen.AddEntry(screen.masterPassword) }
                        )
                    }
                    is AppScreen.AddEntry -> {
                        AddEntryScreen(
                            masterPassword = screen.masterPassword,
                            onNavigateBack = { currentScreen = AppScreen.Main(screen.masterPassword) }
                        )
                    }
                }
            }
        }
    }

    private fun saveEncryptedPassword(encryptedPassword: ByteArray, iv: ByteArray) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit {
            putString(
                PREF_KEY_ENCRYPTED_PASSWORD,
                Base64.encodeToString(encryptedPassword, Base64.DEFAULT)
            )
                .putString(PREF_KEY_PASSWORD_IV, Base64.encodeToString(iv, Base64.DEFAULT))
        }
    }

    private fun showBiometricPromptForEncryption(onSuccess: (Cipher) -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.secure_your_password))
            .setSubtitle(getString(R.string.confirm_to_encrypt))
            .setNegativeButtonText(getString(R.string.cancel))
            .build()

        try {
            val encryptCipher = KeystoreCryptoManager.getEncryptCipher()
            val biometricPrompt = BiometricPrompt(this, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        result.cryptoObject?.cipher?.let { onSuccess(it) }
                    }
                })
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(encryptCipher))
        } catch (e: Exception) {
            Toast.makeText(this, "Error setting up encryption: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showBiometricPromptForDecryption(onSuccess: (CharArray) -> Unit, onFailure: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.acess_to_diary))
            .setSubtitle(getString(R.string.use_pin_or_digital_to_continue))
            .setNegativeButtonText(getString(R.string.cancel))
            .build()

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val encryptedPasswordB64 = prefs.getString(PREF_KEY_ENCRYPTED_PASSWORD, null)
        val ivB64 = prefs.getString(PREF_KEY_PASSWORD_IV, null)

        if (encryptedPasswordB64 == null || ivB64 == null) {
            onFailure()
            return
        }

        try {
            val iv = Base64.decode(ivB64, Base64.DEFAULT)
            val decryptCipher = KeystoreCryptoManager.getDecryptCipherForIv(iv)
            val biometricPrompt = BiometricPrompt(this, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        try {
                            result.cryptoObject?.cipher?.let { cipher ->
                                val encryptedPassword = Base64.decode(encryptedPasswordB64, Base64.DEFAULT)
                                val decryptedPasswordBytes = cipher.doFinal(encryptedPassword)
                                onSuccess(String(decryptedPasswordBytes).toCharArray())
                            } ?: onFailure()
                        } catch (_: Exception) {
                            // This is the expected failure on a new device.
                            onFailure()
                        }
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        // User cancelled, or other error. Treat as failure to unlock.
                        if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON && errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                            onFailure()
                        }
                    }
                })
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(decryptCipher))
        } catch (_: Exception) {
            // Failure to even initialize the cipher means the key is gone (new device).
            onFailure()
        }
    }
}

