package digital.tonima.mydiary

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import digital.tonima.mydiary.data.EncryptedPassword
import digital.tonima.mydiary.data.PasswordBasedCryptoManager
import digital.tonima.mydiary.data.PasswordRepository
import digital.tonima.mydiary.ui.screens.AppScreen
import digital.tonima.mydiary.ui.screens.AppScreen.Locked
import digital.tonima.mydiary.ui.screens.AppScreen.SetupPassword
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.crypto.Cipher
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val passwordRepository: PasswordRepository,
    @ApplicationContext private val applicationContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        if (passwordRepository.hasPassword()) Locked else SetupPassword
    )
    val uiState = _uiState.asStateFlow()

    /**
     * Retrieves the Initialization Vector (IV) needed to unlock the master password.
     * This is required by the BiometricPrompt.
     */
    fun getIvForDecryption(): ByteArray {
        return passwordRepository.getEncryptedPassword()?.iv ?: byteArrayOf()
    }

    fun onPasswordSetup(password: CharArray, cipher: Cipher) {
        viewModelScope.launch(Dispatchers.IO) {
            val encryptedPasswordBytes = cipher.doFinal(password.concatToString().toByteArray())
            val iv = cipher.iv
            passwordRepository.saveEncryptedPassword(EncryptedPassword(encryptedPasswordBytes, iv))
            _uiState.value = AppScreen.Main(password)
        }
    }

    fun onUnlockSuccess(cipher: Cipher) {
        val encryptedPasswordData = passwordRepository.getEncryptedPassword()
        if (encryptedPasswordData == null) {
            _uiState.value = AppScreen.RecoverPassword
            return
        }

        viewModelScope.launch {
            try {
                val decryptedPasswordBytes = withContext(Dispatchers.IO) {
                    cipher.doFinal(encryptedPasswordData.value)
                }
                _uiState.value = AppScreen.Main(String(decryptedPasswordBytes).toCharArray())
            } catch (_: Exception) {
                _uiState.value = AppScreen.RecoverPassword
            }
        }
    }

    fun onUnlockFailure() {
        _uiState.value = AppScreen.RecoverPassword
    }

    fun onRecoveryPasswordSubmit(passwordAttempt: CharArray, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val isPasswordCorrect = withContext(Dispatchers.IO) {
                PasswordBasedCryptoManager.verifyPassword(applicationContext, passwordAttempt)
            }
            onResult(isPasswordCorrect)
        }
    }

    fun onRecoverySuccess(password: CharArray, cipher: Cipher) {
        onPasswordSetup(password, cipher) // The logic is the same as the initial setup
    }

    fun navigateToAddEntry() {
        val currentState = _uiState.value
        if (currentState is AppScreen.Main) {
            _uiState.value = AppScreen.AddEntry(currentState.masterPassword)
        }
    }

    fun navigateToMain() {
        val currentState = _uiState.value
        if (currentState is AppScreen.AddEntry) {
            _uiState.value = AppScreen.Main(currentState.masterPassword)
        }
    }

    fun resetApp() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                PasswordBasedCryptoManager.deleteAllEntries(applicationContext)
                passwordRepository.clearPassword()
            }
            _uiState.value = SetupPassword
        }
    }
    fun lockApp() {
        _uiState.value = Locked
    }

    fun retryBiometricAction() {
        _uiState.value = _uiState.value
    }

}
