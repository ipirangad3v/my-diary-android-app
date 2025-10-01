package digital.tonima.mydiary.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import digital.tonima.mydiary.database.repositories.NfcRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class NfcUiState(
    val secretText: String = "",
    val isWaitingForTag: Boolean = false,
    val encryptedData: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NfcUiState

        if (isWaitingForTag != other.isWaitingForTag) return false
        if (secretText != other.secretText) return false
        if (!encryptedData.contentEquals(other.encryptedData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isWaitingForTag.hashCode()
        result = 31 * result + secretText.hashCode()
        result = 31 * result + (encryptedData?.contentHashCode() ?: 0)
        return result
    }
}

@HiltViewModel
class NfcViewModel
    @Inject
    constructor(
        private val nfcRepository: NfcRepository,
    ) : ViewModel() {

        private val _uiState = MutableStateFlow(NfcUiState())
        val uiState = _uiState.asStateFlow()

        fun onSecretTextChange(text: String) {
            _uiState.update { it.copy(secretText = text) }
        }

        fun prepareToWrite(masterPassword: CharArray) {
            if (uiState.value.secretText.isBlank()) return

            viewModelScope.launch {
                _uiState.update { it.copy(isWaitingForTag = true) }
                val data = withContext(Dispatchers.IO) {
                    nfcRepository.encryptSecret(uiState.value.secretText, masterPassword)
                }
                _uiState.update { it.copy(encryptedData = data) }
            }
        }

        fun onTagWritten() {
            _uiState.update { it.copy(isWaitingForTag = false, encryptedData = null, secretText = "") }
        }

        fun onWriteCancelled() {
            _uiState.update { it.copy(isWaitingForTag = false, encryptedData = null) }
        }
    }
