package digital.tonima.mydiary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import digital.tonima.mydiary.delegates.ProUserProvider
import digital.tonima.mydiary.encrypting.EncryptedPassword
import digital.tonima.mydiary.encrypting.PasswordBasedCryptoManager
import digital.tonima.mydiary.encrypting.PasswordRepository
import digital.tonima.mydiary.ui.screens.AppScreen
import digital.tonima.mydiary.ui.screens.BottomBarScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.crypto.Cipher
import javax.inject.Inject

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        private val passwordRepository: PasswordRepository,
        private val cryptoManager: PasswordBasedCryptoManager,
        proUserProvider: ProUserProvider
    ) : ViewModel(), ProUserProvider by proUserProvider {

        private val _uiState = MutableStateFlow(
            if (passwordRepository.hasPassword()) AppScreen.Locked else AppScreen.SetupPassword
        )
        val uiState = _uiState.asStateFlow()

        private var pendingBiometricAction: (() -> Unit)? = null

        fun getIvForDecryption(): ByteArray {
            return passwordRepository.getEncryptedPassword()?.iv ?: byteArrayOf()
        }

        fun setPendingBiometricAction(action: () -> Unit) {
            pendingBiometricAction = action
        }

        fun retryBiometricAction() {
            pendingBiometricAction?.invoke()
            pendingBiometricAction = null
        }

        fun onPasswordSetup(password: CharArray, cipher: Cipher) {
            viewModelScope.launch(Dispatchers.IO) {
                val encryptedPasswordBytes = cipher.doFinal(password.concatToString().toByteArray())
                val iv = cipher.iv
                passwordRepository.saveEncryptedPassword(EncryptedPassword(encryptedPasswordBytes, iv))
                _uiState.value = AppScreen.Principal(password)
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
                    _uiState.value = AppScreen.Principal(String(decryptedPasswordBytes).toCharArray())
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
                    cryptoManager.verifyPassword(passwordAttempt)
                }
                onResult(isPasswordCorrect)
            }
        }

        fun onRecoverySuccess(password: CharArray, cipher: Cipher) {
            onPasswordSetup(password, cipher)
        }

        fun onScreenSelected(screen: BottomBarScreen) {
            val currentState = _uiState.value
            if (currentState is AppScreen.Principal) {
                _uiState.update {
                    currentState.copy(currentScreen = screen)
                }
            }
        }

        fun navigateToPrincipal() {
            val currentState = _uiState.value
            if (currentState is AppScreen.AddEntry) {
                _uiState.value = AppScreen.Principal(
                    masterPassword = currentState.masterPassword,
                    currentScreen = (uiState.value as? AppScreen.Principal)?.currentScreen ?: BottomBarScreen.Diary
                )
            }
        }

        fun navigateToAddEntry(entryId: Long? = null) {
            val currentState = _uiState.value
            if (currentState is AppScreen.Principal) {
                _uiState.value = AppScreen.AddEntry(currentState.masterPassword, entryId)
            }
        }

        fun lockApp() {
            _uiState.value = AppScreen.Locked
        }

        fun resetApp() {
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    cryptoManager.deleteAllEntries()
                    passwordRepository.clearPassword()
                }
                _uiState.value = AppScreen.SetupPassword
            }
        }

        fun onNfcTagRead(data: ByteArray) {
            val currentState = _uiState.value as? AppScreen.Principal ?: return
            viewModelScope.launch {
                val decrypted = withContext(Dispatchers.IO) {
                    cryptoManager.decryptSecret(data, currentState.masterPassword)
                }
                _uiState.update { (it as AppScreen.Principal).copy(decryptedNfcSecret = decrypted) }
            }
        }

        fun onDismissNfcSecretDialog() {
            val currentState = _uiState.value
            if (currentState is AppScreen.Principal) {
                _uiState.update { currentState.copy(decryptedNfcSecret = null) }
            }
        }
    }
