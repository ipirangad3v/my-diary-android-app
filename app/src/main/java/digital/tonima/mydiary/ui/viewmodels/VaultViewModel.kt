package digital.tonima.mydiary.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import digital.tonima.mydiary.BuildConfig
import digital.tonima.mydiary.encrypting.PasswordBasedCryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class VaultUiState(
    val isLoading: Boolean = true,
    val vaultImages: List<File> = emptyList(),
    val selectedImage: File? = null,
    val showDeleteConfirmation: Boolean = false
)

sealed class VaultEvent {
    data class ShareImage(val uri: Uri) : VaultEvent()
}

@HiltViewModel
class VaultViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<VaultEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun loadVaultImages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val images = withContext(Dispatchers.IO) {
                PasswordBasedCryptoManager.getAllVaultFiles(context)
            }
            _uiState.update { it.copy(isLoading = false, vaultImages = images.sortedByDescending { it.name }) }
        }
    }

    fun saveImage(uri: Uri, masterPassword: CharArray) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                PasswordBasedCryptoManager.saveEncryptedImage(context, uri, masterPassword)
            }
            loadVaultImages()
        }
    }

    fun onImageClicked(file: File) {
        _uiState.update { it.copy(selectedImage = file) }
    }

    fun onDismissImageViewer() {
        _uiState.update { it.copy(selectedImage = null) }
    }

    fun onDeleteRequest() {
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }

    fun onDismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    fun deleteSelectedImage() {
        val imageToDelete = _uiState.value.selectedImage ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                PasswordBasedCryptoManager.deleteEncryptedFile(imageToDelete)
            }
            _uiState.update { it.copy(selectedImage = null, showDeleteConfirmation = false) }
            loadVaultImages()
        }
    }

    fun onShareRequest(masterPassword: CharArray) {
        val imageToShare = _uiState.value.selectedImage ?: return
        viewModelScope.launch {
            val uri = withContext(Dispatchers.IO) {
                val cacheDir = File(context.cacheDir, "images")
                cacheDir.mkdirs()
                val tempFile = File.createTempFile("shared_", ".jpg", cacheDir)

                val success = PasswordBasedCryptoManager.decryptImageToFile(
                    context,
                    imageToShare,
                    tempFile,
                    masterPassword
                )

                if (success) {
                    // Get a content URI for the temporary file using the FileProvider
                    FileProvider.getUriForFile(
                        context,
                        "${BuildConfig.APPLICATION_ID}.provider",
                        tempFile
                    )
                } else {
                    null
                }
            }

            uri?.let {
                _eventFlow.emit(VaultEvent.ShareImage(it))
            }
        }
    }
}
