package digital.tonima.meudiario

import android.content.Context
import android.os.Bundle
import android.util.Base64
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import digital.tonima.meudiario.data.KeystoreCryptoManager
import digital.tonima.meudiario.ui.screens.AddEntryScreen
import digital.tonima.meudiario.ui.screens.LockedScreen
import digital.tonima.meudiario.ui.screens.MainScreen
import digital.tonima.meudiario.ui.screens.PasswordSetupScreen
import digital.tonima.meudiario.ui.theme.MeuDiarioTheme
import javax.crypto.Cipher

sealed class AppScreen {
    object Locked : AppScreen()
    object SetupPassword : AppScreen()
    data class Main(val masterPassword: CharArray) : AppScreen()
    data class AddEntry(val masterPassword: CharArray) : AppScreen()
}

class MainActivity : FragmentActivity() {

    private val PREFS_NAME = "diary_prefs"
    private val PREF_KEY_ENCRYPTED_PASSWORD = "encrypted_password"
    private val PREF_KEY_PASSWORD_IV = "password_iv"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        setContent {
            MeuDiarioTheme {
                var currentScreen by remember {
                    val needsSetup = !prefs.contains(PREF_KEY_ENCRYPTED_PASSWORD)
                    mutableStateOf<AppScreen>(if (needsSetup) AppScreen.SetupPassword else AppScreen.Locked)
                }

                when (val screen = currentScreen) {
                    is AppScreen.SetupPassword -> {
                        PasswordSetupScreen { password ->
                            showBiometricPromptForEncryption { cipher ->
                                val encryptedPassword = cipher.doFinal(password.concatToString().toByteArray())
                                val iv = cipher.iv

                                prefs.edit()
                                    .putString(PREF_KEY_ENCRYPTED_PASSWORD, Base64.encodeToString(encryptedPassword, Base64.DEFAULT))
                                    .putString(PREF_KEY_PASSWORD_IV, Base64.encodeToString(iv, Base64.DEFAULT))
                                    .apply()

                                currentScreen = AppScreen.Main(password)
                            }
                        }
                    }
                    is AppScreen.Locked -> {
                        LockedScreen(onUnlockRequest = {
                            showBiometricPromptForDecryption { decryptedPassword ->
                                currentScreen = AppScreen.Main(decryptedPassword)
                            }
                        })
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

    private fun showBiometricPromptForEncryption(onSuccess: (Cipher) -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.secure_your_password))
            .setSubtitle(getString(R.string.confirm_to_encrypt))
            .setNegativeButtonText(getString(R.string.cancel))
            .build()

        val encryptCipher = KeystoreCryptoManager.getEncryptCipher()

        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    result.cryptoObject?.cipher?.let {
                        onSuccess(it)
                    }
                }
            })

        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(encryptCipher))
    }

    private fun showBiometricPromptForDecryption(onSuccess: (CharArray) -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.acess_to_diary))
            .setSubtitle(getString(R.string.use_pin_or_digital_to_continue))
            .setNegativeButtonText(getString(R.string.cancel))
            .build()

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedPasswordB64 = prefs.getString(PREF_KEY_ENCRYPTED_PASSWORD, null)
        val ivB64 = prefs.getString(PREF_KEY_PASSWORD_IV, null)

        if (encryptedPasswordB64 == null || ivB64 == null) return

        val iv = Base64.decode(ivB64, Base64.DEFAULT)
        val decryptCipher = KeystoreCryptoManager.getDecryptCipherForIv(iv)

        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    result.cryptoObject?.cipher?.let { cipher ->
                        val encryptedPassword = Base64.decode(encryptedPasswordB64, Base64.DEFAULT)
                        val decryptedPasswordBytes = cipher.doFinal(encryptedPassword)
                        onSuccess(String(decryptedPasswordBytes).toCharArray())
                    }
                }
            })

        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(decryptCipher))
    }
}

